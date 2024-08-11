package com.writesmith.openai.structuredoutput;

import com.oaigptconnector.model.JSONSchema;
import com.oaigptconnector.model.JSONSchemaParameter;

@JSONSchema(name = "Generate_Title", functionDescription = "Create SHORT title based on the input text. Keep it 1 - 5 words.", strict = JSONSchema.NullableBool.TRUE)
public class GenerateTitleSO {

    @JSONSchemaParameter(name = "Title")
    private String title;

    public GenerateTitleSO() {

    }

    public GenerateTitleSO(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
