package com.writesmith.core.service;

import java.util.Random;

public class GetChatCapReachedResponses {

    private static final String[] responses = {"I'd love to keep chatting, but my program uses a lot of computer power. Please upgrade to unlock unlimited chats.",
            "Thank you for chatting with me. To continue, please upgrade to unlimited chats.",
            "I hope I was able to help. If you'd like to keep chatting, please subscribe for unlimited chats. There's a 3 day free trial!",
            "You are appreciated. You are loved. Show us some support and subscribe to keep chatting.",
            "Upgrade today for unlimited chats and a free 3 day trial!"};

    public static String getRandomResponse() {
        int randomIndex = new Random().nextInt(responses.length - 1);
        return responses[randomIndex];
    }

}
