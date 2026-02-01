# OpenRouter Streaming & Structured Outputs

## Overview

Client streams chat via WebSocket to server, which proxies to OpenRouter. Supports:
- Standard streaming (text chunks)
- Thinking/reasoning models (DeepSeek-R1, Claude, Qwen3)
- Structured JSON outputs (client-defined schemas)
- Function/tool calling

## Models

OpenRouter format: `provider/model-name` (e.g., `openai/gpt-4o-mini`)

```swift
let request = OAIChatCompletionRequest(
    model: SupportedModel.openai_gpt_4o_mini.slug,  // uses enum
    // or: model: "openai/gpt-4o-mini",             // direct string
    ...
)
```

- **See `SupportedModels.swift`** for available models, slugs, and capabilities
- **Search "OpenRouter models"** for full list: https://openrouter.ai/models

## Response Models

### `GetChatResponse.Body`
```swift
let oaiResponse: ChatCompletionChunk  // OpenAI-compatible chunk
let isThinking: Bool?                  // true during reasoning phase
let thinkingStatus: String?            // "processing" | "complete"
let thinkingDurationMs: Int64?         // elapsed thinking time
let provider: String?                  // "OpenAI", "Anthropic", "DeepSeek"
let reasoningTokens: Int?              // tokens used for reasoning
```

### `ChatCompletionChoiceDelta`
```swift
let content: String?           // regular response content
let thinkingContent: String?   // reasoning text (during thinking)
let reasoningContent: String?  // alias for thinkingContent
let toolCalls: [...]?          // function call chunks
```

### `ChatCompletionChoice`
```swift
let finishReason: String?        // "stop", "tool_calls", "length"
let nativeFinishReason: String?  // provider-specific (e.g., "completed" from OpenAI)
```

### `ChatCompletionUsage`
```swift
let cost: Double?                            // request cost
let completionTokensDetails: Details? {
    let reasoningTokens: Int?
    let imageTokens: Int?
}
```

## Structured Outputs

Define JSON schemas client-side—server passes through to OpenRouter.

### Quick Start
```swift
// Use helper for common patterns
let format = StructuredOutputHelper.sentimentSchema()

// Or build custom schema
let format = StructuredOutputHelper.createResponseFormat(
    name: "my_schema",
    schema: JSONSchemaObject(
        type: "object",
        properties: [
            "field": JSONSchemaProperty(type: "string", description: "A field")
        ],
        required: ["field"],
        additionalProperties: false
    )
)

// Add to request
let request = OAIChatCompletionRequest(
    model: "openai/gpt-4o-mini",
    responseFormat: format,
    stream: true,
    messages: [...]
)
```

### Schema Types

```swift
// String
JSONSchemaProperty(type: "string", description: "...")

// Number
JSONSchemaProperty(type: "number", description: "...")

// Boolean
JSONSchemaProperty(type: "boolean")

// Enum
JSONSchemaProperty(type: "string", enumValues: ["a", "b", "c"])

// Array
JSONSchemaProperty(
    type: "array",
    items: JSONSchemaProperty(type: "string")
)

// Nested object
JSONSchemaProperty(
    type: "object",
    properties: ["nested": JSONSchemaProperty(type: "string")],
    required: ["nested"]
)
```

### Built-in Helpers

```swift
StructuredOutputHelper.sentimentSchema()      // sentiment + confidence + reasoning
StructuredOutputHelper.keyPointsSchema()      // points array + summary
StructuredOutputHelper.stringPropertiesSchema(["key": "description"])
```

## Function/Tool Calling

### Request
```swift
let request = OAIChatCompletionRequest(
    model: "openai/gpt-4o-mini",
    stream: true,
    messages: [...],
    tools: [
        OAIChatCompletionRequestTool(
            type: "function",
            function: OAIChatCompletionRequestToolFunction(
                name: "get_weather",
                description: "Get current weather",
                parameters: JSONSchemaObject(
                    type: "object",
                    properties: [
                        "location": JSONSchemaProperty(type: "string", description: "City name")
                    ],
                    required: ["location"],
                    additionalProperties: false
                )
            )
        )
    ]
)
```

### Response
```swift
// Tool calls stream in delta.toolCalls
if let toolCalls = delta.toolCalls {
    for call in toolCalls {
        let name = call.function.name        // "get_weather"
        let args = call.function.arguments   // "{\"location\":\"NYC\"}"
        let id = call.id                     // "call_abc123"
    }
}
```

### `toolChoice` Options
```swift
toolChoice: "auto"           // model decides (default)
toolChoice: "none"           // don't call any tools
toolChoice: "required"       // must call a tool
toolChoice: OAIChatCompletionRequestToolChoice(
    type: "function",
    function: .init(name: "get_weather")  // force specific function
)
```

See `OAIChatCompletionRequest.swift` for full tool/function definitions.

## Handling Thinking Models

```swift
// In stream handler
if let isThinking = response.body.isThinking, isThinking {
    // Show "Thinking..." UI
    if let reasoning = delta.thinkingContent ?? delta.reasoningContent {
        reasoningText += reasoning  // accumulate reasoning
    }
} else if let content = delta.content {
    responseText += content  // accumulate response
}
```

## Event Sequence (Thinking Models)

```
1. Initial chunk     → role: "assistant", content: ""
2. Thinking event    → isThinking: true, thinkingStatus: "processing"
3. Reasoning chunks  → thinkingContent: "Let me analyze..." (DeepSeek/Qwen/Claude only)
4. First content     → isThinking: false, thinkingStatus: "complete", thinkingDurationMs: 1879
5. Content chunks    → content: "Here's the answer..."
6. Final chunk       → finishReason: "stop", usage: {...}
```

## Model Capabilities

| Model | Thinking Events | `thinkingContent` | `reasoningTokens` |
|-------|-----------------|-------------------|-------------------|
| OpenAI GPT-5/o1/o3 | ✅ | ❌ encrypted | ✅ |
| DeepSeek-R1 | ✅ | ✅ plain text | ✅ |
| Qwen3 | ✅ | ✅ plain text | ✅ |
| Claude (extended thinking) | ✅ | ✅ | varies |
| Standard GPT-4o/4o-mini | ❌ | ❌ | ❌ |

## Key Files

| File | Purpose |
|------|---------|
| `SupportedModels.swift` | Model enum with slugs + capabilities |
| `OAIChatCompletionRequest.swift` | Request structure (tools, messages, format) |
| `GetChatResponse.swift` | Response wrapper with thinking metadata |
| `ChatCompletionChunk.swift` | OAI-compatible streaming chunk |
| `OAIChatCompletionRequestResponseFormat.swift` | Schema definitions |
| `StructuredOutputHelper.swift` | Schema builder helpers |
| `PersistentChatGenerator.swift` | Stream handling + state |

