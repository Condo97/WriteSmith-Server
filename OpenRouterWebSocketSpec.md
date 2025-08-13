## OpenRouter WebSocket Streaming Spec (WriteSmith Server)

This document describes how to use the WriteSmith server’s OpenRouter-backed streaming WebSocket endpoint. It is designed so another project (client or server) can implement it quickly.

### Endpoint URLs

- Production & Dev: `wss://<your-host>/v1/streamChatOpenRouter`

Notes:
- The server runs TLS in production (port 443). Use `wss://`.
- The connection is one-shot per generation: connect, send a single JSON request message, receive streamed responses as multiple JSON messages, then the server closes the connection.

### Authentication

- Include the user’s `authToken` inside the first text message payload (JSON). No headers are required for the WebSocket.

### Request Message Schema

Send a single text message after connecting. The JSON conforms to `GetChatRequest`:

- **authToken**: string (required) – user authentication token
- **chatCompletionRequest**: object (required) – OpenAI-style chat completion payload (see below)
- **function**: object (optional) – structured-output function JSON schema (if you want to force a function/tool call)

The `chatCompletionRequest` matches OpenAI’s Chat Completions format (OpenAI-compatible schema):

- **model**: string – model slug. On OpenRouter, use a supported slug (e.g., `openai/gpt-4o`, `openai/gpt-4.1-mini`, etc.)
- **messages**: array of message objects in the OpenAI format
  - Each message:
    - **role**: `system` | `user` | `assistant` | `tool`
    - **content**: array of content parts; text parts are `{ "type": "text", "text": "..." }`. Images use `{ "type": "image_url", "image_url": { "url": "..." } }`.
- Other standard fields are supported and passed through (e.g., `temperature`, `response_format`, `tools`, `tool_choice`, etc.). The server will ensure `stream_options.include_usage = true`.

Server behavior applied to your request:
- A persistent system prompt is prepended to your system message (or added as a new system message if none exists).
- Messages are truncated to a max of 25 messages. Each message’s combined text length is capped at 5000 characters (image URL lengths count toward this limit).
- If `model` is missing/blank, the server will default it. For best results with OpenRouter, set an explicit model supported by OpenRouter.

#### Minimal request example

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [
          { "type": "text", "text": "Write a haiku about the ocean." }
        ]
      }
    ],
    "temperature": 0.7
  }
}
```

#### With an image

```json
{
  "authToken": "USER_AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-4o",
    "messages": [
      {
        "role": "user",
        "content": [
          { "type": "text", "text": "Describe this image." },
          { "type": "image_url", "image_url": { "url": "https://example.com/cat.jpg" } }
        ]
      }
    ]
  }
}
```

#### Forcing a function/tool call

The `function` field accepts a structured-output schema the server understands. When provided, the server will inject the `tools` and `tool_choice` into the request and force the model to call the provided function.

### Streaming Response Schema

The server sends multiple JSON text messages during generation. Each message is an envelope:

```json
{
  "Success": 1,
  "Body": {
    "oaiResponse": { /* OpenAI-style streaming chunk */ }
  }
}
```

Where `oaiResponse` follows OpenAI’s streaming chunk schema (OpenAI-compatible). Typical shape:

```json
{
  "id": "chatcmpl-...",
  "object": "chat.completion.chunk",
  "created": 1730000000,
  "model": "openai/gpt-4o-mini",
  "choices": [
    {
      "index": 0,
      "delta": { "content": "partial token(s)" },
      "finish_reason": null
    }
  ],
  "usage": {
    "prompt_tokens": 123,
    "completion_tokens": 45,
    "total_tokens": 168
  }
}
```

Notes:
- The server forwards chunks as they arrive. Some chunks may omit fields that only appear in final chunks (like complete `usage`).
- The server strips any leading `data: ` SSE prefix and ignores keep-alive comments.
- When the stream ends, the server closes the WebSocket.

### Error Responses

Errors are also sent as JSON messages over the WebSocket before close:

```json
{
  "Success": 0,
  "description": "Error message"
}
```

If the upstream streaming payload contains non-JSON lines, the server may buffer those and send a single error at the end:

```json
{
  "Success": 0,
  "Body": "<raw error text>"
}
```

### Client Lifecycle

1. Open WebSocket connection to the endpoint URL.
2. Send exactly one JSON text message with the request payload.
3. Read multiple JSON messages until the server closes the socket.
4. On close, if you need another generation, open a new WebSocket and repeat.

### Practical Tips

- Always set a model supported by OpenRouter (e.g., `openai/gpt-4o`, `openai/gpt-4o-mini`).
- For images, use `content` parts with `{ "type": "image_url" }`. Ensure the total content per message stays within the limit (URLs count toward the cap).
- Expect multiple messages; concatenate `choices[0].delta.content` across chunks to build the full text.
- Check `Success` on each envelope before consuming `Body`.
- Usage may be included in a later/final chunk when `stream_options.include_usage = true` (the server enforces this).

### Versioning

- Path: `/v1/streamChatOpenRouter`
- This spec mirrors the existing OpenAI passthrough but targets OpenRouter upstream.


