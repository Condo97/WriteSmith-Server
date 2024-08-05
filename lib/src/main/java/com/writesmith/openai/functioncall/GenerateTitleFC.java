package com.writesmith.openai.functioncall;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

@FunctionCall(name = "Generate_Title", functionDescription = "Create SHORT title based on the input text. Keep it 1 - 5 words.")
public class GenerateTitleFC {

    @FCParameter(name = "Title")
    private String title;

    public GenerateTitleFC() {

    }

    public GenerateTitleFC(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
