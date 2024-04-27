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
                "DgG/bVefKCQ56qoiWKw7i2Fz7OEU/nzOckQyX+If4fV75cJcm0F6cJSxniTevc+3C1Bl+MWWzr/Xbgyt6+33nWJ3VvW1PawCIr8u8TXdrqCeV1u1K+2JONGt50Tng/htCq/VvBqXNVK4uVKk3Q1o7MVKUqh3YnZ3aKygB56a0d0=",
                "FileName.m4a",
                Files.readAllBytes(Paths.get("/Users/alexcoundouriotis/IdeaProjects/WriteSmith-Server/lib/src/main/resources/voiceFile.m4a"))
        );

        TranscribeSpeechResponse response = TranscribeSpeechEndpoint.transcribeSpeech(request);

        System.out.println(response.getText());
    }


}
