package com.writesmith._deprecated.getchatrequest;

import com.writesmith.database.model.Sender;
import com.writesmith.core.service.request.GetChatRequest;

import java.util.List;

public class GetChatLegacyRequestAdapter {

    public static GetChatRequest adapt(GetChatLegacyRequest getChatLegacyRequest) {
        // Create GetChatRequest Chat
        GetChatRequest.Chat chat = new GetChatRequest.Chat(
                0,
                getChatLegacyRequest.getInputText(),
                getChatLegacyRequest.getImageData(),
                getChatLegacyRequest.getImageURL(),
                null,
                Sender.USER
        );

        GetChatRequest gcr = new GetChatRequest(
                getChatLegacyRequest.getAuthToken(),
                getChatLegacyRequest.getBehavior(),
                List.of(chat),
                null,
                null,
                null,
                getChatLegacyRequest.getConversationID(),
                getChatLegacyRequest.getUsePaidModel(),
                getChatLegacyRequest.getDebug()
        );

        return gcr;
    }

}
