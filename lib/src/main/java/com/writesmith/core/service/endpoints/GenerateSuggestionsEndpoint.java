package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.OAIDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.SuggestionsGenerator;
import com.writesmith.core.service.request.GenerateSuggestionsRequest;
import com.writesmith.core.service.response.GenerateSuggestionsResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateSuggestionsEndpoint {

    public static GenerateSuggestionsResponse generateSuggestions(GenerateSuggestionsRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, OpenAIGPTException, OAIDeserializerException, IOException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Generate suggestions
        List<SuggestionsGenerator.Suggestion> suggestions = SuggestionsGenerator.generateSuggestions(request.getCount(), request.getConversation(), request.getDifferentThan());

        // Transpose Suggestions to String array
        List<String> suggestionStrings = suggestions.stream()
                .map(SuggestionsGenerator.Suggestion::getSuggestion)
                .toList();

        // Create GenerateSuggestionResponse and return
        GenerateSuggestionsResponse gsResponse = new GenerateSuggestionsResponse(suggestionStrings);

        return gsResponse;
    }

}
