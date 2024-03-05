package com.writesmith.core.gpt_function_calls;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

import java.util.List;

@FunctionCall(name = "Generate_Suggestions", functionDescription = "Create SHORT questions to use as prompts for professor GPT to continue the conversation between user and professor GPT. 2-8 words each. They can be generic and knowledge or learning forward if not enough context is provided. They should be able to help the user delve into the topic they are talking about and learn more. Keep the questions short as they have to fit on a small screen.")
public class GetSuggestionsFC {

    @FCParameter(name = "Suggestions")
    private List<String> suggestions;

    public GetSuggestionsFC() {

    }

    public GetSuggestionsFC(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

}
