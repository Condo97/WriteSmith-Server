package com.writesmith.core.service.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateGoogleQueryRequest extends AuthRequest {

    public static class InputChat {

        private CompletionRole role;
        private String input;

        public InputChat() {

        }

        public InputChat(CompletionRole role, String input) {
            this.role = role;
            this.input = input;
        }

        public CompletionRole getRole() {
            return role;
        }

        public String getInput() {
            return input;
        }

    }

    private String input;
    private List<InputChat> inputs;

    public GenerateGoogleQueryRequest() {

    }

    public GenerateGoogleQueryRequest(String authToken, String input) {
        super(authToken);
        this.input = input;
    }

    public GenerateGoogleQueryRequest(String authToken, List<InputChat> inputs) {
        super(authToken);
        this.inputs = inputs;
    }

    public String getInput() {
        return input;
    }

    public List<InputChat> getInputs() {
        return inputs;
    }

}
