package com.writesmith.core.service.response;

public class CheckIfChatRequestsImageRevisionResponse {

    private Boolean requestsImageRevision;

    public CheckIfChatRequestsImageRevisionResponse() {

    }

    public CheckIfChatRequestsImageRevisionResponse(Boolean requestsImageRevision) {
        this.requestsImageRevision = requestsImageRevision;
    }

    public Boolean getRequestsImageRevision() {
        return requestsImageRevision;
    }

}
