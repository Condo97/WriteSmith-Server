package com.writesmith.core.service.response;

import java.util.List;

public class GoogleSearchResponse {

    public static class Result {

        private String title;
        private String url;

        public Result() {

        }

        public Result(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

    }

    private List<Result> results;

    public GoogleSearchResponse() {

    }

    public GoogleSearchResponse(List<Result> results) {
        this.results = results;
    }

    public List<Result> getResults() {
        return results;
    }

}
