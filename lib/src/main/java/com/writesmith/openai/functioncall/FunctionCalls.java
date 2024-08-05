package com.writesmith.openai.functioncall;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

public enum FunctionCalls {

    CheckIfChatRequestsImageRevision("check_if_chat_requests_image_revision"),
    ClassifyChat("classify_chat"),
    Drawers("drawers"),
    GenerateGoogleQuery("generate_google_query"),
    GenerateSuggestions("generate_suggestions"),
    GenerateTitle("generate_title");

    private String name;

    FunctionCalls(String name) {
        this.name = name;
    }

    @JsonCreator
    public static FunctionCalls from(String name) {
        for (FunctionCalls functionCall: FunctionCalls.values()) {
            if (functionCall.getName().equals(name)) {
                return functionCall;
            }
        }

        return null;
    }

    @JsonGetter
    public String getName() {
        return name;
    }

    public Class<?> getFunctionClass() {
        switch (this) {
            case CheckIfChatRequestsImageRevision: return CheckIfChatRequestsImageRevisionFC.class;
            case ClassifyChat: return ClassifyChatFC.class;
            case Drawers: return DrawersFC.class;
            case GenerateGoogleQuery: return GenerateGoogleQueryFC.class;
            case GenerateSuggestions: return GenerateSuggestionsFC.class;
            case GenerateTitle: return GenerateTitleFC.class;
        }

        return null;
    }

}
