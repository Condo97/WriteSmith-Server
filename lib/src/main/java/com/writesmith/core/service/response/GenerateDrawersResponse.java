package com.writesmith.core.service.response;

import java.util.List;

public class GenerateDrawersResponse {

    public static class Drawer {

        private Integer index;
        private String title;
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

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

    private String title;
    private List<Drawer> drawers;

    public GenerateDrawersResponse() {

    }

    public GenerateDrawersResponse(String title, List<Drawer> drawers) {
        this.title = title;
        this.drawers = drawers;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Drawer> getDrawers() {
        return drawers;
    }

    public void setDrawers(List<Drawer> drawers) {
        this.drawers = drawers;
    }

}
