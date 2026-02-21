package com.writesmith.chat;

import com.writesmith.core.service.websockets.chat.util.ReasoningModelDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReasoningModelDetectorTest {

    @Test
    void detectsReasoningModels() {
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/o1"));
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/o3-mini"));
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/gpt-5-mini"));
        assertTrue(ReasoningModelDetector.isReasoningModel("deepseek/deepseek-r1"));
        assertTrue(ReasoningModelDetector.isReasoningModel("qwen/qwen3-235b"));
    }

    @Test
    void rejectsNonReasoningModels() {
        assertFalse(ReasoningModelDetector.isReasoningModel("openai/gpt-4o"));
        assertFalse(ReasoningModelDetector.isReasoningModel("openai/gpt-4o-mini"));
        assertFalse(ReasoningModelDetector.isReasoningModel("anthropic/claude-3.5-sonnet"));
        assertFalse(ReasoningModelDetector.isReasoningModel(null));
    }

    @Test
    void caseInsensitive() {
        assertTrue(ReasoningModelDetector.isReasoningModel("OpenAI/O1"));
        assertTrue(ReasoningModelDetector.isReasoningModel("DEEPSEEK/DEEPSEEK-R1"));
    }

    @Test
    void timeoutForReasoningModels() {
        assertEquals(10, ReasoningModelDetector.timeoutMinutes("openai/o1"));
        assertEquals(10, ReasoningModelDetector.timeoutMinutes("openai/gpt-5-mini"));
    }

    @Test
    void timeoutForNonReasoningModels() {
        assertEquals(4, ReasoningModelDetector.timeoutMinutes("openai/gpt-4o"));
        assertEquals(4, ReasoningModelDetector.timeoutMinutes(null));
    }
}
