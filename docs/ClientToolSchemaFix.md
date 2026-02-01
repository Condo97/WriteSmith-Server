# Client Tool Schema Validation Error Fix

## Issue

The server is now correctly forwarding tool definitions to OpenRouter, but Azure (via OpenRouter) is rejecting the `submit_review` tool schema with this error:

```
Invalid schema for function 'submit_review': 
'required' is required to be supplied and to be an array including every key in properties. 
Missing 'refinedQuery'.
```

## Root Cause

Azure enforces strict JSON Schema validation: **every property listed in `properties` must also be included in the `required` array**.

## Current Problem

The `submit_review` tool definition has `refinedQuery` in `properties` but it's missing from the `required` array:

```json
{
  "type": "function",
  "function": {
    "name": "submit_review",
    "parameters": {
      "type": "object",
      "properties": {
        "refinedQuery": { "type": "string" },  // ✅ Present in properties
        // ... other properties
      },
      "required": ["someOtherField"]  // ❌ Missing "refinedQuery"
    }
  }
}
```

## Fix Required (Client-Side)

Update the `submit_review` tool schema to include **all properties** in the `required` array:

```json
{
  "required": ["refinedQuery", /* all other properties that must be provided */]
}
```

**OR** if `refinedQuery` is truly optional, remove it from `properties`.

## Verification

After fixing the schema, the server will successfully forward the tool definition to OpenRouter and tool calls should stream properly to the client.

## Status

- ✅ Server: Tool passthrough working correctly
- ❌ Client: Tool schema needs validation fix
