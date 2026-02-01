# Enhanced Streaming API - Client Implementation Guide

This document describes the enhanced streaming response format for the OpenRouter WebSocket endpoint. The new format adds thinking/reasoning indicators while maintaining full backwards compatibility with existing clients.

## Overview

The server now sends additional metadata during the streaming process to help clients:
1. Show "thinking" indicators while the AI model is processing
2. Display reasoning/thinking content from models that support it (DeepSeek-R1, Qwen3, Claude)
3. Track timing metrics (how long the model spent thinking)
4. Identify which AI provider is handling the request

## Backwards Compatibility

**All new fields are optional and use `null` when not applicable.**

- Legacy clients will continue to work unchanged - they decode `oaiResponse.choices[0].delta.content` and ignore unknown fields
- Swift's `Codable` automatically ignores unknown JSON keys
- New optional properties default to `nil` when missing from the server response

## Response Structure

### Wrapper Response (`GetChatStreamResponse`)

```json
{
  "Success": 1,
  "Body": {
    "oaiResponse": { /* Enhanced OAI response - see below */ },
    
    // NEW FIELDS (all optional, null when not applicable)
    "thinking_status": "processing" | "complete" | null,
    "thinking_duration_ms": 1879,
    "provider": "OpenAI",
    "reasoning_tokens": 64,
    "is_thinking": true | false | null
  }
}
```

### New Wrapper Fields

| Field | Type | When Present | Description |
|-------|------|--------------|-------------|
| `thinking_status` | String? | During/after thinking | `"processing"` while thinking, `"complete"` on first content |
| `thinking_duration_ms` | Int64? | After thinking starts | Milliseconds since thinking began |
| `provider` | String? | When available | AI provider (e.g., "OpenAI", "Anthropic", "DeepSeek") |
| `reasoning_tokens` | Int? | In final chunk | Number of tokens used for reasoning |
| `is_thinking` | Bool? | During thinking | `true` while thinking, `false` when content starts |

### Enhanced OAI Response (`oaiResponse`)

The `oaiResponse` object now includes additional fields:

```json
{
  "id": "gen-1234567890",
  "object": "chat.completion.chunk",
  "model": "openai/gpt-5-mini",
  "created": 1764282113,
  "provider": "OpenAI",
  "choices": [{
    "index": 0,
    "delta": {
      "role": "assistant",
      "content": "Hello",
      
      // NEW FIELDS (null when not applicable)
      "thinking_content": "Let me analyze this...",
      "reasoning_content": "Let me analyze this..."
    },
    "finish_reason": null,
    "native_finish_reason": null
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

### New Delta Fields

| Field | Type | When Present | Description |
|-------|------|--------------|-------------|
| `thinking_content` | String? | DeepSeek/Qwen/Claude | Plain-text reasoning from the model |
| `reasoning_content` | String? | Same as above | Alias for `thinking_content` (for compatibility) |

### New Choice Fields

| Field | Type | When Present | Description |
|-------|------|--------------|-------------|
| `native_finish_reason` | String? | From OpenRouter | Provider-specific finish reason (e.g., "completed") |

## Event Sequence

Here's the typical sequence of events during a request:

### 1. Initial Connection Response
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{"delta": {"role": "assistant", "content": ""}}]
    }
  }
}
```
*First chunk with empty content, establishing the role.*

### 2. Thinking Started Event (NEW)
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{"delta": {"role": "assistant", "content": null}}]
    },
    "thinking_status": "processing",
    "is_thinking": true
  }
}
```
*Sent when first keep-alive is received. Client should show "thinking" indicator.*

### 3. Reasoning Content Events (for supported models)
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{
        "delta": {
          "content": null,
          "thinking_content": "I need to consider the user's previous messages...",
          "reasoning_content": "I need to consider the user's previous messages..."
        }
      }]
    },
    "thinking_status": "processing",
    "is_thinking": true,
    "provider": "DeepSeek"
  }
}
```
*Reasoning text from models like DeepSeek-R1, Qwen3. Can be displayed or hidden.*

### 4. First Content Event (Thinking Complete)
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{"delta": {"content": "Hello"}}],
      "provider": "OpenAI"
    },
    "thinking_status": "complete",
    "thinking_duration_ms": 1879,
    "is_thinking": false,
    "provider": "OpenAI"
  }
}
```
*First actual content token. Client should hide "thinking" indicator and start showing response.*

### 5. Subsequent Content Events
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{"delta": {"content": " world!"}}],
      "provider": "OpenAI"
    },
    "provider": "OpenAI"
  }
}
```
*Standard content chunks. Append to response.*

### 6. Final Event with Usage
```json
{
  "Body": {
    "oaiResponse": {
      "choices": [{"delta": {"content": ""}, "finish_reason": "stop"}],
      "usage": {
        "prompt_tokens": 418,
        "completion_tokens": 184,
        "reasoning_tokens": 64,
        "cost": 0.0004725
      }
    },
    "reasoning_tokens": 64,
    "provider": "OpenAI"
  }
}
```
*Final chunk with usage statistics.*

## Swift Implementation

### Updated Models

```swift
// Enhanced delta with thinking fields
struct ChatCompletionChoiceDelta: Codable {
    let role: String?
    let content: String?
    let toolCalls: [ChatCompletionChoiceDeltaToolCall]?
    
    // NEW - thinking/reasoning content (ignored by old clients, nil if missing)
    let thinkingContent: String?
    let reasoningContent: String?
    
    private enum CodingKeys: String, CodingKey {
        case role, content
        case toolCalls = "tool_calls"
        case thinkingContent = "thinking_content"
        case reasoningContent = "reasoning_content"
    }
}

// Enhanced choice with native finish reason
struct ChatCompletionChoice: Codable {
    let index: Int?
    let delta: ChatCompletionChoiceDelta?
    let finishReason: String?
    
    // NEW
    let nativeFinishReason: String?
    
    private enum CodingKeys: String, CodingKey {
        case index, delta
        case finishReason = "finish_reason"
        case nativeFinishReason = "native_finish_reason"
    }
}

// Enhanced OAI response with provider
struct ChatCompletionChunk: Codable {
    let id: String?
    let object: String?
    let model: String?
    let created: Int?
    let choices: [ChatCompletionChoice]?
    let usage: ChatCompletionUsage?
    
    // NEW
    let provider: String?
}

// Enhanced wrapper response
struct GetChatStreamResponseBody: Codable {
    let oaiResponse: ChatCompletionChunk?
    
    // NEW - thinking metadata
    let thinkingStatus: String?
    let thinkingDurationMs: Int64?
    let provider: String?
    let reasoningTokens: Int?
    let isThinking: Bool?
    
    private enum CodingKeys: String, CodingKey {
        case oaiResponse
        case thinkingStatus = "thinking_status"
        case thinkingDurationMs = "thinking_duration_ms"
        case provider
        case reasoningTokens = "reasoning_tokens"
        case isThinking = "is_thinking"
    }
}
```

### UI Logic

```swift
class ChatViewModel: ObservableObject {
    @Published var isThinking = false
    @Published var thinkingDuration: TimeInterval = 0
    @Published var reasoningText = ""
    @Published var responseText = ""
    
    func handleStreamChunk(_ response: GetChatStreamResponseBody) {
        // Update thinking state
        if let isThinking = response.isThinking {
            self.isThinking = isThinking
        }
        
        // Update thinking duration
        if let durationMs = response.thinkingDurationMs {
            self.thinkingDuration = TimeInterval(durationMs) / 1000.0
        }
        
        // Handle reasoning content (optional - can show/hide based on user preference)
        if let reasoning = response.oaiResponse?.choices?.first?.delta?.thinkingContent {
            self.reasoningText += reasoning
        }
        
        // Handle actual content
        if let content = response.oaiResponse?.choices?.first?.delta?.content {
            self.responseText += content
        }
    }
}
```

### UI Example

```swift
struct ChatBubbleView: View {
    @ObservedObject var viewModel: ChatViewModel
    
    var body: some View {
        VStack(alignment: .leading) {
            // Show thinking indicator
            if viewModel.isThinking {
                HStack {
                    ProgressView()
                        .scaleEffect(0.8)
                    Text("Thinking...")
                        .foregroundColor(.secondary)
                    if viewModel.thinkingDuration > 0 {
                        Text(String(format: "%.1fs", viewModel.thinkingDuration))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }
            
            // Optionally show reasoning (collapsed by default)
            if !viewModel.reasoningText.isEmpty {
                DisclosureGroup("View reasoning") {
                    Text(viewModel.reasoningText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            // Show actual response
            Text(viewModel.responseText)
        }
    }
}
```

## Model-Specific Behavior

| Model | Thinking Events | `thinking_content` | `reasoning_tokens` |
|-------|-----------------|--------------------|--------------------|
| OpenAI GPT-5 | ✅ Yes | ❌ No (encrypted) | ✅ Yes |
| OpenAI o1/o3 | ✅ Yes | ❌ No (encrypted) | ✅ Yes |
| DeepSeek-R1 | ✅ Yes | ✅ Yes (plain text) | ✅ Yes |
| Qwen3 | ✅ Yes | ✅ Yes (plain text) | ✅ Yes |
| Claude | ✅ Yes | ✅ Yes (if extended thinking enabled) | Varies |
| Standard models | ❌ No | ❌ No | ❌ No |

## Testing

To test the enhanced streaming:

1. **Thinking indicator**: Use any reasoning model (GPT-5-mini, DeepSeek-R1) - you should see `is_thinking: true` followed by `is_thinking: false`

2. **Reasoning content**: Use DeepSeek-R1 or Qwen3 - you should see `thinking_content` populated with the model's reasoning

3. **Timing**: Check `thinking_duration_ms` on the first content chunk to see how long the model spent reasoning

4. **Provider info**: Check the `provider` field to see which backend handled the request

## Migration Checklist

- [ ] Update `ChatCompletionChoiceDelta` to include `thinkingContent` and `reasoningContent`
- [ ] Update `ChatCompletionChoice` to include `nativeFinishReason`
- [ ] Update `ChatCompletionChunk` to include `provider`
- [ ] Update `GetChatStreamResponseBody` to include all new wrapper fields
- [ ] Add UI for thinking indicator
- [ ] (Optional) Add UI for displaying reasoning content
- [ ] (Optional) Display thinking duration
- [ ] (Optional) Show provider info

## Questions?

The server logs all stream activity to `logs/openrouter/`. Check these logs for debugging or to see the raw data being sent.

