package com.writesmith._deprecated.getchatrequest;

import com.writesmith.database.model.Sender;
import com.writesmith.core.service.request.GetChatRequest_Legacy;

import java.util.List;

public class GetChatLegacyRequestAdapter {

    public static GetChatRequest_Legacy adapt(GetChatLegacyRequest getChatLegacyRequest) {
        // Create GetChatRequest Chat
        GetChatRequest_Legacy.Chat chat = new GetChatRequest_Legacy.Chat(
                0,
                getChatLegacyRequest.getInputText(),
                getChatLegacyRequest.getImageData(),
                getChatLegacyRequest.getImageURL(),
                null,
                Sender.USER
        );

        GetChatRequest_Legacy gcr = new GetChatRequest_Legacy(
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
