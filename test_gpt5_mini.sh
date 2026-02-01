#!/bin/bash

# Test script for GPT-5-mini via WebSocket
# This tests that reasoning models don't timeout prematurely

# Configuration
HOST="${1:-wss://localhost/v1/streamChatOpenRouter}"
AUTH_TOKEN="${2:-YOUR_AUTH_TOKEN_HERE}"

echo "Testing GPT-5-mini WebSocket streaming..."
echo "Host: $HOST"
echo ""

# Create the test request JSON
REQUEST=$(cat <<EOF
{
  "authToken": "$AUTH_TOKEN",
  "chatCompletionRequest": {
    "model": "openai/gpt-5-mini",
    "messages": [
      {
        "role": "user",
        "content": [{"type": "text", "text": "What is 25 * 47? Think step by step."}]
      }
    ],
    "temperature": 0.7
  }
}
EOF
)

echo "Request:"
echo "$REQUEST" | head -20
echo ""
echo "Connecting to WebSocket..."

# Use websocat if available, otherwise provide instructions
if command -v websocat &> /dev/null; then
    echo "$REQUEST" | websocat --no-close "$HOST" | while read -r line; do
        echo "[$(date '+%H:%M:%S')] $line"
        
        # Check for thinking status
        if echo "$line" | grep -q '"is_thinking":true'; then
            echo "  ↳ Model is thinking..."
        fi
        
        # Check for content
        if echo "$line" | grep -q '"content":'; then
            echo "  ↳ Received content!"
        fi
        
        # Check for finish
        if echo "$line" | grep -q '"finish_reason":"stop"'; then
            echo "  ↳ Stream completed successfully!"
        fi
    done
else
    echo "websocat not found. Install it with: brew install websocat"
    echo ""
    echo "Alternative: Use this curl command to test the health of the server:"
    echo "curl -v $HOST"
    echo ""
    echo "Or test with a WebSocket client tool like:"
    echo "  - Postman (supports WebSocket)"
    echo "  - wscat (npm install -g wscat)"
    echo "  - websocat (brew install websocat)"
fi


