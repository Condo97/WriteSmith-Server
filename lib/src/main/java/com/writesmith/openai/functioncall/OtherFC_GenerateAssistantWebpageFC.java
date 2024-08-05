package com.writesmith.openai.functioncall;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

import java.util.List;

@FunctionCall(name = "Generate_Assistant_Webpage", functionDescription = "Creates an assistant webpage per the spec.")
public class OtherFC_GenerateAssistantWebpageFC {

    @FCParameter(name = "title", description = "the title of the webpage")
    private String title;

    @FCParameter(name = "subtitle", description = "the subtitle of the webpage")
    private String subtitle;

    @FCParameter(name = "short_description", description = "a short description under the subtitle of the webpage")
    private String shortDescription;

    @FCParameter(name = "who_its_for_title", description = "a title describing who the assistant is for")
    private String whoItsForTitle;

    @FCParameter(name = "who_its_for_description", description = "the description describing who the assistant is for")
    private String whoItsForDescription;

    @FCParameter(name = "who_its_for_points", description = "3-5 points describing different applications that one can use the assistant for")
    private List<String> whoItsForPoints;

    @FCParameter(name = "how_to_use_title", description = "the title describing how to use the assistant")
    private String howToUseTitle;

    @FCParameter(name = "how_to_use_steps", description = "the steps how to use the assistant")
    private List<String> howToUseDescription;

    public OtherFC_GenerateAssistantWebpageFC() {

    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getWhoItsForTitle() {
        return whoItsForTitle;
    }

    public String getWhoItsForDescription() {
        return whoItsForDescription;
    }

    public List<String> getWhoItsForPoints() {
        return whoItsForPoints;
    }

    public String getHowToUseTitle() {
        return howToUseTitle;
    }

    public List<String> getHowToUseDescription() {
        return howToUseDescription;
    }

}
