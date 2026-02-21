package com.writesmith.core.service.websockets.chat.util;

import java.util.List;
import java.util.Locale;

public final class ReasoningModelDetector {

    private static final List<String> REASONING_PREFIXES = List.of(
            "openai/o1",
            "openai/o3",
            "openai/gpt-5",
            "deepseek/deepseek-r1",
            "qwen/qwen3"
    );

    private static final int REASONING_TIMEOUT_MINUTES = 10;
    private static final int DEFAULT_TIMEOUT_MINUTES = 4;

    private ReasoningModelDetector() {}

    public static boolean isReasoningModel(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase(Locale.ROOT);
        for (String prefix : REASONING_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }

    public static int timeoutMinutes(String modelName) {
        return isReasoningModel(modelName) ? REASONING_TIMEOUT_MINUTES : DEFAULT_TIMEOUT_MINUTES;
    }
}
