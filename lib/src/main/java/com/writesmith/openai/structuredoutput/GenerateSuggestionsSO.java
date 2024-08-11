package com.writesmith.openai.structuredoutput;

import com.oaigptconnector.model.JSONSchema;
import com.oaigptconnector.model.JSONSchemaParameter;

import java.util.List;

@JSONSchema(name = "Generate_Suggestions", functionDescription = "Create SHORT questions starting to use as prompts for GPT-4 to continue the conversation between user and GPT-4. 2-8 words each. They can be generic and knowledge or learning forward if not enough context is provided. They should be able to help the user delve into the topic they are talking about and learn more. GPT-4 has image vision capability. Keep the questions short as they have to fit on a small screen.", strict = JSONSchema.NullableBool.TRUE)
public class GenerateSuggestionsSO {

    @JSONSchemaParameter(name = "suggestions")
    private List<String> suggestions;

    public GenerateSuggestionsSO() {

    }

    public GenerateSuggestionsSO(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

}
