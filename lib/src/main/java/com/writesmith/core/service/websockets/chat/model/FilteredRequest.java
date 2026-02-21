package com.writesmith.core.service.websockets.chat.model;

import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;

public final class FilteredRequest {

    private final String requestJson;
    private final String modelName;
    private final User_AuthToken userAuthToken;
    private final OpenRouterRequestLogger logger;
    private final int totalImagesFound;
    private final int totalImagesSent;

    public FilteredRequest(String requestJson, String modelName, User_AuthToken userAuthToken,
                           OpenRouterRequestLogger logger, int totalImagesFound, int totalImagesSent) {
        this.requestJson = requestJson;
        this.modelName = modelName;
        this.userAuthToken = userAuthToken;
        this.logger = logger;
        this.totalImagesFound = totalImagesFound;
        this.totalImagesSent = totalImagesSent;
    }

    public String getRequestJson() { return requestJson; }
    public String getModelName() { return modelName; }
    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public int getTotalImagesFound() { return totalImagesFound; }
    public int getTotalImagesSent() { return totalImagesSent; }
}
