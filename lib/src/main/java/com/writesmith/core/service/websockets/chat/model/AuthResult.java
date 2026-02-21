package com.writesmith.core.service.websockets.chat.model;

import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;

import java.time.LocalDateTime;

public final class AuthResult {

    private final User_AuthToken userAuthToken;
    private final GetChatRequest request;
    private final String rawMessage;
    private final OpenRouterRequestLogger logger;
    private final LocalDateTime startTime;
    private final RawPassthroughFields passthroughFields;

    public AuthResult(User_AuthToken userAuthToken, GetChatRequest request, String rawMessage,
                      OpenRouterRequestLogger logger, LocalDateTime startTime,
                      RawPassthroughFields passthroughFields) {
        this.userAuthToken = userAuthToken;
        this.request = request;
        this.rawMessage = rawMessage;
        this.logger = logger;
        this.startTime = startTime;
        this.passthroughFields = passthroughFields;
    }

    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public GetChatRequest getRequest() { return request; }
    public String getRawMessage() { return rawMessage; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public LocalDateTime getStartTime() { return startTime; }
    public RawPassthroughFields getPassthroughFields() { return passthroughFields; }
}
