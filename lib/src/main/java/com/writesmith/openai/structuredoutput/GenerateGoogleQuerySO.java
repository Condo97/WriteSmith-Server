package com.writesmith.openai.structuredoutput;

import com.oaigptconnector.model.JSONSchema;
import com.oaigptconnector.model.JSONSchemaParameter;

@JSONSchema(name = "Generate_Google_Query", functionDescription = "Create a google query from the input.", strict = JSONSchema.NullableBool.TRUE)
public class GenerateGoogleQuerySO {

    @JSONSchemaParameter(name = "query")
    private String query;

    public GenerateGoogleQuerySO() {

    }

    public GenerateGoogleQuerySO(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

}
