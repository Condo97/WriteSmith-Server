# Structured Output & Function Call Passthrough Guide

The OpenRouter streaming WebSocket (`/v1/streamChatOpenRouter`) supports client-defined structured outputs and function calls without requiring server-side changes. The client can define JSON schemas directly in the request.

## Overview

You have **two options** for structured outputs/function calls:

| Approach | Best For | How It Works |
|----------|----------|--------------|
| **Passthrough** | Custom schemas, client flexibility | Client defines `tools` or `response_format` in `chatCompletionRequest` |
| **Server-defined** | Predefined schemas, type safety | Client sets `function` enum, server injects schema |

## Option 1: Client-Defined Passthrough (Recommended for Custom Schemas)

Simply include `tools`, `tool_choice`, or `response_format` directly in your `chatCompletionRequest`. The server will pass them through to OpenRouter unchanged.

### Structured Output (JSON Schema Response)

Use `response_format` to get structured JSON responses:

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [{"type": "text", "text": "What's the weather like in Tokyo?"}]
      }
    ],
    "response_format": {
      "type": "json_schema",
      "json_schema": {
        "name": "weather_response",
        "strict": true,
        "schema": {
          "type": "object",
          "properties": {
            "location": {"type": "string"},
            "temperature": {"type": "number"},
            "unit": {"type": "string", "enum": ["celsius", "fahrenheit"]},
            "conditions": {"type": "string"},
            "humidity": {"type": "number"}
          },
          "required": ["location", "temperature", "unit", "conditions"],
          "additionalProperties": false
        }
      }
    }
  }
}
```

**Response:**
```json
{
  "Success": 1,
  "Body": {
    "oaiResponse": {
      "choices": [{
        "delta": {
          "content": "{\"location\": \"Tokyo\", \"temperature\": 22, \"unit\": \"celsius\", \"conditions\": \"partly cloudy\", \"humidity\": 65}"
        }
      }]
    }
  }
}
```

### Function Calling (Tools)

Use `tools` and `tool_choice` for function calling:

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [{"type": "text", "text": "Get the current stock price of AAPL"}]
      }
    ],
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "get_stock_price",
          "description": "Get the current stock price for a given symbol",
          "parameters": {
            "type": "object",
            "properties": {
              "symbol": {
                "type": "string",
                "description": "The stock ticker symbol (e.g., AAPL, GOOGL)"
              }
            },
            "required": ["symbol"]
          }
        }
      }
    ],
    "tool_choice": {"type": "function", "function": {"name": "get_stock_price"}}
  }
}
```

**Response (streaming):**
```json
{
  "Success": 1,
  "Body": {
    "oaiResponse": {
      "choices": [{
        "delta": {
          "tool_calls": [{
            "index": 0,
            "id": "call_abc123",
            "type": "function",
            "function": {
              "name": "get_stock_price",
              "arguments": "{\"symbol\": \"AAPL\"}"
            }
          }]
        }
      }]
    }
  }
}
```

### Multiple Tools

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [{"type": "text", "text": "What's the weather in NYC and the price of Bitcoin?"}]
      }
    ],
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "get_weather",
          "description": "Get weather for a location",
          "parameters": {
            "type": "object",
            "properties": {
              "location": {"type": "string"}
            },
            "required": ["location"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "get_crypto_price",
          "description": "Get cryptocurrency price",
          "parameters": {
            "type": "object",
            "properties": {
              "coin": {"type": "string"}
            },
            "required": ["coin"]
          }
        }
      }
    ],
    "tool_choice": "auto"
  }
}
```

## Option 2: Server-Defined Functions (Predefined Schemas)

For predefined schemas, use the `function` field with an enum value:

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [...]
  },
  "function": "generate_title"
}
```

**Available predefined functions:**
- `check_if_chat_requests_image_revision`
- `classify_chat`
- `drawers`
- `generate_google_query`
- `generate_suggestions`
- `generate_title`

## Swift Client Implementation

### Model Updates

```swift
// Add response_format support to your request model
struct ChatCompletionRequest: Codable {
    let model: String
    let messages: [ChatMessage]
    let temperature: Double?
    let stream: Bool?
    
    // Passthrough fields for structured outputs
    let responseFormat: ResponseFormat?
    let tools: [Tool]?
    let toolChoice: ToolChoice?
    
    private enum CodingKeys: String, CodingKey {
        case model, messages, temperature, stream
        case responseFormat = "response_format"
        case tools
        case toolChoice = "tool_choice"
    }
}

// JSON Schema response format
struct ResponseFormat: Codable {
    let type: String  // "json_schema" or "text"
    let jsonSchema: JSONSchema?
    
    private enum CodingKeys: String, CodingKey {
        case type
        case jsonSchema = "json_schema"
    }
}

struct JSONSchema: Codable {
    let name: String
    let strict: Bool?
    let schema: [String: AnyCodable]  // Use AnyCodable for flexible schema definition
}

// Tool definition
struct Tool: Codable {
    let type: String  // "function"
    let function: ToolFunction
}

struct ToolFunction: Codable {
    let name: String
    let description: String?
    let parameters: [String: AnyCodable]
}

// Tool choice
enum ToolChoice: Codable {
    case auto
    case none
    case function(name: String)
    
    // Custom encoding/decoding...
}
```

### Response Handling

```swift
// Delta now includes tool_calls
struct ChatCompletionChoiceDelta: Codable {
    let role: String?
    let content: String?
    let toolCalls: [ToolCallDelta]?
    
    // From Enhanced API
    let thinkingContent: String?
    let reasoningContent: String?
    
    private enum CodingKeys: String, CodingKey {
        case role, content
        case toolCalls = "tool_calls"
        case thinkingContent = "thinking_content"
        case reasoningContent = "reasoning_content"
    }
}

struct ToolCallDelta: Codable {
    let index: Int?
    let id: String?
    let type: String?
    let function: ToolCallFunction?
}

struct ToolCallFunction: Codable {
    let name: String?
    let arguments: String?  // JSON string - parse after streaming completes
}
```

### Usage Example

```swift
func requestStructuredOutput<T: Codable>(
    messages: [ChatMessage],
    schema: JSONSchema,
    responseType: T.Type
) async throws -> T {
    let request = GetChatRequest(
        authToken: authToken,
        chatCompletionRequest: ChatCompletionRequest(
            model: "openai/gpt-4o-mini",
            messages: messages,
            responseFormat: ResponseFormat(
                type: "json_schema",
                jsonSchema: schema
            )
        )
    )
    
    var fullContent = ""
    
    // Stream and accumulate content
    for try await chunk in streamChat(request) {
        if let content = chunk.oaiResponse?.choices?.first?.delta?.content {
            fullContent += content
        }
    }
    
    // Parse the structured response
    let data = fullContent.data(using: .utf8)!
    return try JSONDecoder().decode(T.self, from: data)
}
```

## Important Notes

1. **Priority**: If you set both `function` (server-defined) and `tools` (client-defined), the server-defined function takes precedence and overwrites your tools.

2. **Model Support**: Not all models support structured outputs. Use models like:
   - `openai/gpt-4o-mini` ✅
   - `openai/gpt-4o` ✅
   - `anthropic/claude-3.5-sonnet` ✅
   - Check OpenRouter docs for full compatibility

3. **Streaming**: Tool call arguments stream incrementally. Accumulate `function.arguments` across chunks before parsing.

4. **Strict Mode**: Set `"strict": true` in your JSON schema for guaranteed schema compliance (supported by OpenAI models).

## Debugging

Check the server logs at `logs/openrouter/` for passthrough information:

```
[PASSTHROUGH] Client provided: tools=2 response_format=yes
```

This confirms your tools/response_format were detected and will be passed to OpenRouter.

## Model Compatibility Quick Reference

| Feature | OpenAI | Anthropic | DeepSeek | Qwen |
|---------|--------|-----------|----------|------|
| `response_format` (json_schema) | ✅ | ✅ | ✅ | ✅ |
| `tools` / function calling | ✅ | ✅ | ✅ | ✅ |
| Strict JSON schema | ✅ | ❌ | ❌ | ❌ |
| Parallel tool calls | ✅ | ✅ | ❌ | ❌ |





