package com.writesmith.core.service.response;

import java.util.List;

public class GenerateSuggestionsResponse {

    List<String> suggestions;

    public GenerateSuggestionsResponse() {

    }

    public GenerateSuggestionsResponse(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

}
