package com.writesmith.core.service.websockets.chat;

import com.writesmith.Constants;
import com.writesmith.core.service.websockets.chat.model.AuthResult;
import com.writesmith.core.service.websockets.chat.model.ChatPipelineRequest;
import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.model.StreamResult;
import com.writesmith.core.service.websockets.chat.stages.*;
import com.writesmith.core.service.websockets.chat.util.OpenRouterHttpClient;
import com.writesmith.util.OpenRouterRequestLogger;
import org.eclipse.jetty.websocket.api.Session;

import java.net.http.HttpClient;
import java.util.stream.Stream;

public class ChatPipelineOrchestrator {

    private final AuthenticateStage authenticateStage;
    private final BuildRequestStage buildRequestStage;
    private final FilterMessagesStage filterMessagesStage;
    private final StreamChatStage streamChatStage;
    private final ParseStreamStage parseStreamStage;
    private final PersistResultStage persistResultStage;

    public ChatPipelineOrchestrator(HttpClient httpClient) {
        this.authenticateStage = new AuthenticateStage();
        this.buildRequestStage = new BuildRequestStage();
        this.filterMessagesStage = new FilterMessagesStage();
        this.streamChatStage = new StreamChatStage(
                new OpenRouterHttpClient(httpClient, Constants.OPENAPI_URI));
        this.parseStreamStage = new ParseStreamStage();
        this.persistResultStage = new PersistResultStage();
    }

    public void execute(Session session, String message) throws Exception {
        OpenRouterRequestLogger logger = null;
        try {
            AuthResult authResult = authenticateStage.execute(message);
            logger = authResult.getLogger();

            ChatPipelineRequest pipelineRequest = buildRequestStage.execute(authResult);
            FilteredRequest filteredRequest = filterMessagesStage.execute(pipelineRequest);

            Stream<String> sseStream = streamChatStage.execute(filteredRequest);
            StreamResult result = parseStreamStage.execute(sseStream, session, filteredRequest);

            persistResultStage.execute(result, filteredRequest, session);
        } finally {
            if (logger != null) logger.close();
        }
    }
}
