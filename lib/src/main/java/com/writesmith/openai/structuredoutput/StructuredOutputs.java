package com.writesmith.openai.structuredoutput;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;

public enum StructuredOutputs {

    CheckIfChatRequestsImageRevision("check_if_chat_requests_image_revision"),
    ClassifyChat("classify_chat"),
    Drawers("drawers"),
    GenerateGoogleQuery("generate_google_query"),
    GenerateSuggestions("generate_suggestions"),
    GenerateTitle("generate_title");

    private String name;

    StructuredOutputs(String name) {
        this.name = name;
    }

    @JsonCreator
    public static StructuredOutputs from(String name) {
        for (StructuredOutputs structuredOutput: StructuredOutputs.values()) {
            if (structuredOutput.getName().equals(name)) {
                return structuredOutput;
            }
        }

        return null;
    }

    @JsonGetter
    public String getName() {
        return name;
    }

    public Class<?> getJSONSchemaClass() {
        switch (this) {
            case CheckIfChatRequestsImageRevision: return CheckIfChatRequestsImageRevisionSO.class;
            case ClassifyChat: return ClassifyChatSO.class;
            case Drawers: return DrawersSO.class;
            case GenerateGoogleQuery: return GenerateGoogleQuerySO.class;
            case GenerateSuggestions: return GenerateSuggestionsSO.class;
            case GenerateTitle: return GenerateTitleSO.class;
        }

        return null;
    }

}
