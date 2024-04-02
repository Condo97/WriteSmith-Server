package com.writesmith.core.gpt_function_calls;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

@FunctionCall(name = "Check_If_Chat_Requests_Image_Revision", functionDescription = "Takes an input chat and checks if it is asking for a revision to a previously generated image, or not. It should be referring to a previously generated image, or expressing implication of referring to a previously generated image. You should be able to come up with an idea of the image to generate based on this chat.")
public class CheckIfChatRequestsImageRevisionFC {

    @FCParameter(name = "Requests_Revision", description = "True if the user is asking for a revision of the image that they just generated")
    private Boolean requestsRevision;

    public CheckIfChatRequestsImageRevisionFC() {

    }

    public Boolean getRequestsRevision() {
        return requestsRevision;
    }

}
