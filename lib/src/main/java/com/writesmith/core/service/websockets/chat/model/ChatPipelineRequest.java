package com.writesmith.core.service.websockets.chat.model;

import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;

public final class ChatPipelineRequest {

    private final OAIChatCompletionRequest completionRequest;
    private final User_AuthToken userAuthToken;
    private final OpenRouterRequestLogger logger;
    private final RawPassthroughFields passthroughFields;
    private final boolean hasServerFunctionOverride;

    public ChatPipelineRequest(OAIChatCompletionRequest completionRequest, User_AuthToken userAuthToken,
                               OpenRouterRequestLogger logger, RawPassthroughFields passthroughFields,
                               boolean hasServerFunctionOverride) {
        this.completionRequest = completionRequest;
        this.userAuthToken = userAuthToken;
        this.logger = logger;
        this.passthroughFields = passthroughFields;
        this.hasServerFunctionOverride = hasServerFunctionOverride;
    }

    public OAIChatCompletionRequest getCompletionRequest() { return completionRequest; }
    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public RawPassthroughFields getPassthroughFields() { return passthroughFields; }
    public boolean hasServerFunctionOverride() { return hasServerFunctionOverride; }
}
