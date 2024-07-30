package com.writesmith;

import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.service.endpoints.TranscribeSpeechEndpoint;
import com.writesmith.core.service.request.TranscribeSpeechRequest;
import com.writesmith.core.service.response.TranscribeSpeechResponse;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidFileTypeException;
import com.writesmith.keys.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

public class TranscriptionTests {

    @BeforeAll
    static void setUp() throws SQLException {
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, 10);
    }

    @Test
    @DisplayName("Transcription Test")
    void transcriptionTest() throws IOException, DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, InvalidFileTypeException {
        TranscribeSpeechRequest request = new TranscribeSpeechRequest(
                "RBdpat4XJLYgQDZe8mlLo/Q6skCwPbGxfD9x0pPdJAbAa5VXp9cPC7fN3BD0mbB/prufGLDJ7PtZsNI5OOeKwbEIAB4ldKpGFQIapftF1LfGxeinPkcTGC0zWvLvcbLwnAs/T8eZ3YwULBNbp3lGmQw2O6MTtwkPVHYiabL/S0E=",
                "FileName.m4a",
                Files.readAllBytes(Paths.get("/Users/alexcoundouriotis/IdeaProjects/WriteSmith-Server/lib/src/main/resources/voiceFile.m4a"))
        );

        TranscribeSpeechResponse response = TranscribeSpeechEndpoint.transcribeSpeech(request);

        System.out.println(response.getText());
    }


}
