# OpenRouter Streaming API - Developer Reference Sheet

## Quick Reference

| Property | Value |
|----------|-------|
| **Endpoint** | `wss://<host>/v1/streamChatOpenRouter` |
| **Protocol** | WebSocket (WSS) |
| **Authentication** | `authToken` in request body |
| **Default Model** | `openai/gpt-5-mini` |
| **Max Messages** | 25 |
| **Max Conversation Length** | 50,000 characters |
| **Idle Timeout** | 2 minutes (for reasoning models) |

---

## Request Format

### Basic Request

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [{"type": "text", "text": "Hello!"}]
      }
    ],
    "temperature": 0.7
  }
}
```

### Request Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `authToken` | String | ✅ | User authentication token |
| `chatCompletionRequest` | Object | ✅ | OpenAI-compatible chat completion payload |
| `function` | Enum | ❌ | Server-defined structured output function |

### `chatCompletionRequest` Fields

| Field | Type | Description |
|-------|------|-------------|
| `model` | String | OpenRouter model slug (e.g., `openai/gpt-4o-mini`) |
| `messages` | Array | OpenAI-format messages |
| `temperature` | Number | 0-2, creativity control |
| `response_format` | Object | JSON schema for structured output |
| `tools` | Array | Function calling tools |
| `tool_choice` | String/Object | Tool selection behavior |

---

## Response Format

### Wrapper Structure

```json
{
  "Success": 1,
  "Body": {
    "oaiResponse": { /* OpenAI-compatible streaming chunk */ },
    
    // Enhanced metadata (optional, null when not applicable)
    "thinking_status": "processing" | "complete" | null,
    "thinking_duration_ms": 1879,
    "is_thinking": true | false | null,
    "provider": "OpenAI",
    "reasoning_tokens": 64
  }
}
```

### Response Fields

| Field | Type | When Present | Description |
|-------|------|--------------|-------------|
| `oaiResponse` | Object | Always | OpenAI-compatible streaming chunk |
| `thinking_status` | String | During/after thinking | `"processing"` or `"complete"` |
| `thinking_duration_ms` | Integer | After thinking starts | Milliseconds spent thinking |
| `is_thinking` | Boolean | During thinking | `true` while thinking |
| `provider` | String | When available | AI provider name |
| `reasoning_tokens` | Integer | Final chunk | Tokens used for reasoning |

### `oaiResponse` (Enhanced OAI Chunk)

```json
{
  "id": "gen-1234567890",
  "object": "chat.completion.chunk",
  "model": "openai/gpt-4o-mini",
  "created": 1764282113,
  "provider": "OpenAI",
  "choices": [{
    "index": 0,
    "delta": {
      "role": "assistant",
      "content": "Hello",
      "thinking_content": "Let me analyze...",
      "reasoning_content": "Let me analyze...",
      "tool_calls": [...]
    },
    "finish_reason": "stop",
    "native_finish_reason": "completed"
  }],
  "usage": {
    "prompt_tokens": 418,
    "completion_tokens": 184,
    "total_tokens": 602,
    "cost": 0.0004725,
    "completion_tokens_details": {
      "reasoning_tokens": 64,
      "image_tokens": 0
    }
  }
}
```

---

## Supported Features

### 1. Standard Streaming

Basic text streaming with OpenAI-compatible chunks.

```json
{
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {"role": "user", "content": [{"type": "text", "text": "Write a haiku"}]}
    ]
  }
}
```

### 2. Vision (Images)

Send images as base64 data URLs or HTTP URLs.

```json
{
  "chatCompletionRequest": {
    "model": "openai/gpt-4o",
    "messages": [{
      "role": "user",
      "content": [
        {"type": "text", "text": "Describe this image"},
        {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
      ]
    }]
  }
}
```

**Image Handling:**
- Auto-resized to max 1024×1400
- Token cost: `(width × height) / 750`
- Premium users: 50% token weight toward conversation limit
- Free users: 100% token weight

### 3. Structured Outputs (JSON Schema)

Get guaranteed JSON responses matching your schema.

```json
{
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [...],
    "response_format": {
      "type": "json_schema",
      "json_schema": {
        "name": "weather_response",
        "strict": true,
        "schema": {
          "type": "object",
          "properties": {
            "temperature": {"type": "number"},
            "conditions": {"type": "string"},
            "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]}
          },
          "required": ["temperature", "conditions", "unit"],
          "additionalProperties": false
        }
      }
    }
  }
}
```

### 4. Function Calling (Tools)

Define tools for the model to call.

```json
{
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [...],
    "tools": [{
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get current weather for a location",
        "parameters": {
          "type": "object",
          "properties": {
            "location": {"type": "string", "description": "City name"}
          },
          "required": ["location"]
        }
      }
    }],
    "tool_choice": "auto"
  }
}
```

**`tool_choice` Options:**
| Value | Description |
|-------|-------------|
| `"auto"` | Model decides (default) |
| `"none"` | Don't call any tools |
| `"required"` | Must call a tool |
| `{"type": "function", "function": {"name": "..."}}` | Force specific function |

**Response with Tool Call:**
```json
{
  "delta": {
    "tool_calls": [{
      "index": 0,
      "id": "call_abc123",
      "type": "function",
      "function": {
        "name": "get_weather",
        "arguments": "{\"location\": \"NYC\"}"
      }
    }]
  }
}
```

### 5. Server-Defined Functions

Use predefined schemas with the `function` field.

```json
{
  "authToken": "...",
  "chatCompletionRequest": {...},
  "function": "generate_title"
}
```

**Available Functions:**
| Function | Description |
|----------|-------------|
| `check_if_chat_requests_image_revision` | Detect image edit requests |
| `classify_chat` | Classify conversation type |
| `drawers` | Generate drawer/category suggestions |
| `generate_google_query` | Create search queries |
| `generate_suggestions` | Generate follow-up suggestions |
| `generate_title` | Generate conversation title |

⚠️ **Priority:** Server-defined `function` overwrites client-provided `tools`.

### 6. Thinking/Reasoning Models

Models like DeepSeek-R1, GPT-5, o1, o3, Qwen3, and Claude with extended thinking.

**Event Sequence:**
```
1. Initial chunk      → role: "assistant", content: ""
2. Thinking event     → is_thinking: true, thinking_status: "processing"
3. Reasoning chunks   → thinking_content: "Let me analyze..." (visible reasoning)
4. First content      → is_thinking: false, thinking_status: "complete", thinking_duration_ms: 1879
5. Content chunks     → content: "Here's the answer..."
6. Final chunk        → finish_reason: "stop", usage: {...}
```

**Delta Fields for Thinking:**
| Field | Description |
|-------|-------------|
| `thinking_content` | Plain text reasoning (DeepSeek, Qwen, Claude) |
| `reasoning_content` | Alias for thinking_content |

---

## Model Capabilities Matrix

| Model | Standard | Vision | Structured Output | Tools | Thinking Events | Visible Reasoning |
|-------|----------|--------|-------------------|-------|-----------------|-------------------|
| `openai/gpt-4o` | ✅ | ✅ | ✅ (strict) | ✅ | ❌ | ❌ |
| `openai/gpt-4o-mini` | ✅ | ✅ | ✅ (strict) | ✅ | ❌ | ❌ |
| `openai/gpt-5-mini` | ✅ | ✅ | ✅ (strict) | ✅ | ✅ | ❌ (encrypted) |
| `openai/o1` | ✅ | ❌ | ✅ | ❌ | ✅ | ❌ (encrypted) |
| `openai/o3-mini` | ✅ | ❌ | ✅ | ❌ | ✅ | ❌ (encrypted) |
| `anthropic/claude-3.5-sonnet` | ✅ | ✅ | ✅ | ✅ | ✅* | ✅* |
| `deepseek/deepseek-r1` | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ |
| `qwen/qwen3-235b` | ✅ | ❌ | ✅ | ✅ | ✅ | ✅ |

\* Claude requires extended thinking to be enabled

---

## Client Implementation

### Connection Lifecycle

```
1. Connect WebSocket to wss://<host>/v1/streamChatOpenRouter
2. Send ONE JSON request message
3. Receive multiple JSON response messages
4. Server closes connection when complete
5. Open new connection for next request
```

### Swift Example

```swift
struct GetChatRequest: Codable {
    let authToken: String
    let chatCompletionRequest: ChatCompletionRequest
    let function: String? // Optional server-defined function
}

func streamChat(_ request: GetChatRequest) async throws -> AsyncThrowingStream<ResponseBody, Error> {
    let ws = URLSession.shared.webSocketTask(with: url)
    ws.resume()
    
    // Send request
    let data = try JSONEncoder().encode(request)
    try await ws.send(.string(String(data: data, encoding: .utf8)!))
    
    // Stream responses
    return AsyncThrowingStream { continuation in
        Task {
            while true {
                let message = try await ws.receive()
                if case .string(let text) = message {
                    let response = try JSONDecoder().decode(Response.self, from: text.data(using: .utf8)!)
                    continuation.yield(response.body)
                }
            }
        }
    }
}
```

### Handling Thinking UI

```swift
func handleChunk(_ body: ResponseBody) {
    // Show thinking indicator
    if let isThinking = body.isThinking, isThinking {
        showThinkingUI()
        
        // Optionally display reasoning
        if let reasoning = body.oaiResponse?.choices?.first?.delta?.thinkingContent {
            appendReasoningText(reasoning)
        }
    } else {
        hideThinkingUI()
        
        // Show actual content
        if let content = body.oaiResponse?.choices?.first?.delta?.content {
            appendResponseText(content)
        }
    }
    
    // Update thinking duration
    if let duration = body.thinkingDurationMs {
        updateThinkingDuration(Double(duration) / 1000.0)
    }
}
```

---

## Error Handling

### Error Response

```json
{
  "Success": 0,
  "description": "Error message"
}
```

### Common Error Codes

| Success | Meaning |
|---------|---------|
| `1` | Success |
| `0` | Error (check `description`) |

### Non-JSON Errors

If upstream returns non-JSON, it's forwarded as:
```json
{
  "Success": 0,
  "Body": "<raw error text>"
}
```

---

## Limits & Quotas

| Limit | Value |
|-------|-------|
| Max input messages | 25 |
| Max conversation length | 50,000 characters |
| Max message text length | Truncated at limit |
| Max image dimensions | 1024 × 1400 |
| WebSocket idle timeout | 2 minutes |
| Image token calculation | `(width × height) / 750` |

---

## Debug Logging

Server logs are written to `logs/openrouter/` with:
- Client requests
- Authentication status
- Message filtering
- Outgoing requests
- Stream chunks (raw + parsed)
- Thinking/reasoning events
- Token usage
- Errors

Log format: `user_{userID}_{timestamp}.log`

---

## Quick Start Checklist

- [ ] Connect via WebSocket (WSS)
- [ ] Include `authToken` in request body
- [ ] Set explicit `model` (e.g., `openai/gpt-4o-mini`)
- [ ] Use OpenAI message format with `content` array
- [ ] Handle `Success: 0` error responses
- [ ] Concatenate `delta.content` across chunks
- [ ] Check `finish_reason: "stop"` for completion
- [ ] (Optional) Handle `is_thinking` for reasoning models
- [ ] (Optional) Add `response_format` for structured output
- [ ] (Optional) Add `tools` for function calling

