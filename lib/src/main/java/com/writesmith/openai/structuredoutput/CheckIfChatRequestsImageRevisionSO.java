package com.writesmith.openai.structuredoutput;

import com.oaigptconnector.model.JSONSchema;
import com.oaigptconnector.model.JSONSchemaParameter;

@JSONSchema(name = "Check_If_Chat_Requests_Image_Revision", functionDescription = "Takes an input chat and checks if it is asking for a revision to a previously generated image, or not. It should be referring to a previously generated image, or expressing implication of referring to a previously generated image. You should be able to come up with an idea of the image to generate based on this chat.", strict = JSONSchema.NullableBool.TRUE)
public class CheckIfChatRequestsImageRevisionSO {

    @JSONSchemaParameter(name = "Requests_Revision", description = "True if the user is asking for a revision of the image that they just generated")
    private Boolean requestsRevision;

    public CheckIfChatRequestsImageRevisionSO() {

    }

    public Boolean getRequestsRevision() {
        return requestsRevision;
    }

}
