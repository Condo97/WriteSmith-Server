package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.request.TranscribeSpeechRequest;
import com.writesmith.core.service.response.TranscribeSpeechResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidFileTypeException;
import com.writesmith.keys.Keys;
import com.writesmith.openai.SpeechTranscriber;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class TranscribeSpeechEndpoint {

    private static String openAIKey = Keys.openAiAPI;

    public static TranscribeSpeechResponse transcribeSpeech(TranscribeSpeechRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OpenAIGPTException, IOException, InvalidFileTypeException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Get transcription
        SpeechTranscriber.CompletedTranscription transcription = SpeechTranscriber.transcribe(
                request.getSpeechFileName(),
                request.getSpeechFile(),
                null,
                Keys.openAiAPI
        );

        // If transcription text is null throw InvalidFileTypeException, otherwise return transcription text in TranscribeSpeechResponse
        if (transcription.getText() == null) {
            throw new InvalidFileTypeException("Invalid file type", "flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm");
        }

        // Transform into TranscribeSpeechResponse and return
        return new TranscribeSpeechResponse(
                transcription.getText()
        );
    }

}
