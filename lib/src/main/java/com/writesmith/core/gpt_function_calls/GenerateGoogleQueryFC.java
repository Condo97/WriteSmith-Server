package com.writesmith.core.gpt_function_calls;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

@FunctionCall(name = "Generate_Google_Query", functionDescription = "Create a google query from the input.")
public class GenerateGoogleQueryFC {

    @FCParameter(name = "query")
    private String query;

    public GenerateGoogleQueryFC() {

    }

    public GenerateGoogleQueryFC(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

}
