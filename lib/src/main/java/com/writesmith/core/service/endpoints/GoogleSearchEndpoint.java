package com.writesmith.core.service.endpoints;

import com.writesmith.core.service.request.GoogleSearchRequest;
import com.writesmith.core.service.response.GoogleSearchResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.google.search.GoogleSearcher;
import com.writesmith.keys.Keys;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GoogleSearchEndpoint {

    private static final String searchEngineID = "c44d35c5fc0304f87";

    public static GoogleSearchResponse search(GoogleSearchRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, URISyntaxException, IOException {
        // Get u_aT from authToken
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Get GoogleSearcher Response by doing Google search
        GoogleSearcher.Response response = GoogleSearcher.search(
                Keys.googleSearchAPIKey,
                searchEngineID,
                request.getQuery()
        );

        // Print log to console
        printLog(u_aT.getUserID());

        // Return transposed response to GoogleSearchResponse
        return new GoogleSearchResponse(
                response.getItems().stream().map(i -> {
                    return new GoogleSearchResponse.Result(
                            i.getTitle(),
                            i.getLink()
                    );
                }).toList()
        );
    }


    private static void printLog(Integer userID) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        System.out.println("User " + userID + " requested a Google Search " + sdf.format(new Date()));
    }

}
