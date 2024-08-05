package com.writesmith.openai.functioncall;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

import java.util.List;

@FunctionCall(name = "Output_In_Drawers", functionDescription = "A drawer is a UI item that has a title, and when the title is tapped the content shows.")
public class DrawersFC {

    public static class Drawer {

        @FCParameter(name = "Index")
        private Integer index;

        @FCParameter(name = "Title")
        private String title;

        @FCParameter(name = "Content")
        private String content;

        public Drawer() {

        }

        public Drawer(Integer index, String title, String content) {
            this.index = index;
            this.title = title;
            this.content = content;
        }

        public Integer getIndex() {
            return index;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

    }

    @FCParameter(name = "Title", description = "A fitting title for this collection of drawers.")
    private String title;

    @FCParameter(name = "Drawers")
    private List<Drawer> drawers;

    public DrawersFC() {

    }

    public DrawersFC(String title, List<Drawer> drawers) {
        this.title = title;
        this.drawers = drawers;
    }

    public String getTitle() {
        return title;
    }

    public List<Drawer> getDrawers() {
        return drawers;
    }

}
