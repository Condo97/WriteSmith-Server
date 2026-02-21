# Agentic Chat Production Hardening — Design

**Date:** 2026-02-21
**Status:** Approved
**Scope:** Full refactor of `GetChatWebSocket_OpenRouter.java` into a pipeline architecture with reliability hardening

## Problem

`GetChatWebSocket_OpenRouter.java` is a 1,204-line monolithic WebSocket handler with a single ~830-line `getChat()` method mixing auth, request building, message filtering, image processing, streaming, response parsing, thinking state management, token tracking, and persistence. This causes:

- **Reliability gaps:** No retry logic, unbounded thread pool, stream resource leaks, dead code paths
- **Speed issues:** Synchronous premium checks block requests, no per-model timeout configuration
- **Unmaintainable:** Too long for humans or AI to reason about in one pass, no testable boundaries

## Architecture: Pipeline of Stages

Decompose the god method into a linear pipeline of single-responsibility stages:

```
WebSocket → Authenticate → BuildRequest → FilterMessages → StreamChat → ParseStream → Persist
```

Each stage is a plain Java class with one public method, typed input/output. The WebSocket handler becomes a thin orchestrator (~60 lines).

### Pipeline Stages

| Stage | Responsibility | Input | Output | ~Lines |
|---|---|---|---|---|
| `AuthenticateStage` | Parse JSON, validate auth token, init logger | raw message string | `AuthResult` | 80 |
| `BuildRequestStage` | System prompt, tools/FC, raw field passthrough | `AuthResult` + parsed request | `ChatPipelineRequest` | 120 |
| `FilterMessagesStage` | Max 25 msgs, 50k char cap, image sizing, premium check | `ChatPipelineRequest` | `FilteredRequest` | 130 |
| `StreamChatStage` | HTTP request to OpenRouter, retry, timeout | `FilteredRequest` | SSE `Stream<String>` | 100 |
| `ParseStreamStage` | SSE parsing, thinking state, enhanced response, send to client | SSE stream + WebSocket session | `StreamResult` | 150 |
| `PersistResultStage` | Token usage DB write, background premium sync | `StreamResult` | void | 60 |

### Data Transfer Objects

| DTO | Contents |
|---|---|
| `AuthResult` | `User_AuthToken`, `OpenRouterRequestLogger`, parse timing |
| `RawPassthroughFields` | Consolidates 7 `JsonNode` variables (response_format, tools, tool_choice, reasoning, reasoning_effort, verbosity, max_completion_tokens) |
| `ChatPipelineRequest` | Built `OAIChatCompletionRequest`, `RawPassthroughFields`, model info |
| `FilteredRequest` | Final messages, image stats, premium status, serialized request JSON |
| `StreamResult` | Token counts (prompt, completion, reasoning, cached), error buffer, thinking metrics |

## Reliability Hardening

### Thread Pool
- Replace `CachedThreadPool` with bounded `ThreadPoolExecutor(20, 100, 60s, queue=50)`
- `CallerRunsPolicy` for graceful degradation under load

### Retry Logic (StreamChatStage)
- Transient failures (HTTP 429, 502, 503, 504): retry up to 2× with exponential backoff (1s, 3s)
- Client/config errors (400, 401, 403): fail immediately
- Timeout: fail with model-type-aware error message

### Stream Resource Safety
- `chatStream` wrapped in try-with-resources
- `clientDisconnected` flag stops processing early (existing pattern, kept)
- `StreamResult.close()` ensures HTTP connection release

### Connection Management
- Keep static `HttpClient` (HTTP/2 multiplexes correctly)
- Per-request timeout: 4 min regular, 10 min reasoning (selected by `ReasoningModelDetector`)

### Logger Failure Tolerance
- If `OpenRouterRequestLogger` init fails, fall back to no-op logger

## Code Cleanup

### Dead Code Removal
- `sbError` (never populated)
- Unused imports: `Graphics2D`, `RenderingHints`, `BufferedImage`, `ByteArrayOutputStream`
- `printStreamedGeneratedChatDoBetterLoggingLol` method

### Naming
- `resizeImageUrlWithDimensions` → `ImageDimensionExtractor.extract()`
- `RawPassthroughFields` replaces 7 separate `JsonNode` variables

### AI Readability
- Every file under 150 lines
- Single responsibility per class, stated in one-line doc comment
- Method signatures are the documentation
- No "what" comments, only "why" for non-obvious decisions

## File Layout

```
com.writesmith.core.service.websockets/
├── GetChatWebSocket_OpenRouter.java          (thin orchestrator)
└── chat/
    ├── ChatPipelineOrchestrator.java          (wires stages, error handling)
    ├── stages/
    │   ├── AuthenticateStage.java
    │   ├── BuildRequestStage.java
    │   ├── FilterMessagesStage.java
    │   ├── StreamChatStage.java
    │   ├── ParseStreamStage.java
    │   └── PersistResultStage.java
    ├── model/
    │   ├── AuthResult.java
    │   ├── ChatPipelineRequest.java
    │   ├── FilteredRequest.java
    │   ├── StreamResult.java
    │   └── RawPassthroughFields.java
    └── util/
        ├── ImageDimensionExtractor.java
        ├── OpenRouterHttpClient.java
        └── ReasoningModelDetector.java
```

## Constraints

- Wire format (WebSocket JSON messages to/from clients) must not change
- Existing `OpenRouterRequestLogger` and `PremiumStatusCache` are kept as-is (already well-structured)
- Legacy WebSocket handlers are untouched in this round
