package com.writesmith.openai.functioncall;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

import java.util.List;

@FunctionCall(name = "Generate_Suggestions", functionDescription = "Create SHORT questions starting to use as prompts for GPT-4 to continue the conversation between user and GPT-4. 2-8 words each. They can be generic and knowledge or learning forward if not enough context is provided. They should be able to help the user delve into the topic they are talking about and learn more. GPT-4 has image vision capability. Keep the questions short as they have to fit on a small screen.")
public class GenerateSuggestionsFC {

    @FCParameter(name = "suggestions")
    private List<String> suggestions;

    public GenerateSuggestionsFC() {

    }

    public GenerateSuggestionsFC(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

}
