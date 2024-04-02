package com.writesmith.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DALLE3ImageGenerationResponse {

    public static class Data {

        private String b64_json;
        private String url;
        private String revised_prompt;

        public Data() {

        }

        public Data(String b64_json, String url, String revised_prompt) {
            this.b64_json = b64_json;
            this.url = url;
            this.revised_prompt = revised_prompt;
        }

        public String getB64_json() {
            return b64_json;
        }

        public String getUrl() {
            return url;
        }

        public String getRevised_prompt() {
            return revised_prompt;
        }

    }

    private Long created;
    private List<Data> data;

    public DALLE3ImageGenerationResponse() {

    }

    public DALLE3ImageGenerationResponse(Long created, List<Data> data) {
        this.created = created;
        this.data = data;
    }

    public Long getCreated() {
        return created;
    }

    public List<Data> getData() {
        return data;
    }

}
