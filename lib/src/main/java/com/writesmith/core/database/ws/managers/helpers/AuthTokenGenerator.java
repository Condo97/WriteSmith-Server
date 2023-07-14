package com.writesmith.core.database.ws.managers.helpers;

import java.util.Base64;
import java.util.Random;

public class AuthTokenGenerator {
    public static String generateAuthToken() {
        // Generate AuthToken
        Random rd = new Random();
        byte[] bytes = new byte[128];
        rd.nextBytes(bytes);

        return Base64.getEncoder().encodeToString(bytes);
    }
}
