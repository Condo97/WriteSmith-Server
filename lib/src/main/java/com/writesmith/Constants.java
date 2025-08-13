package com.writesmith;

import com.oaigptconnector.model.request.chat.completion.CompletionRole;

import java.net.URI;

public final class Constants {

    private Constants() {
    }

    public static final class Additional {

        public static final Integer functionCallGenerationTokenLimit = 1500;
        public static final Integer functionCallDefaultTemperature = 1;

    }

    /* In-App Purchases Pricing */
    public static final int DEFAULT_PRICE_INDEX = 0;
    public static final double PRICE_VAR2_DISPLAY_CHANCE = 0.1;
    public static final String WEEKLY_PRICE_VAR1 = "6.95";
    public static final String WEEKLY_NAME_VAR1 = "chitchatultra";
    public static final String MONTHLY_PRICE_VAR1 = "19.99";
    public static final String MONTHLY_NAME_VAR1 = "ultramonthly";
    public static final String WEEKLY_PRICE_VAR2 = "2.99";
    public static final String WEEKLY_NAME_VAR2 = "chitchatultraunlimited";
    public static final String MONTHLY_PRICE_VAR2 = "9.99";
    public static final String MONTHLY_NAME_VAR2 = "writesmithultraunlimitedmonthly";

    public static final String YEARLY_PRICE = "49.99";
    public static final String YEARLY_NAME = "chitchatultrayearly";

    /* Tiered Limits */
    public static final int Character_Limit_Additional_Text_Free = 8000;
    public static final int Character_Limit_Additional_Text_Paid = 16000;
    public static final int Response_Token_Limit_GPT_3_Turbo_Free = 350;
    public static final int Response_Token_Limit_GPT_3_Turbo_Paid = 1400;
    public static final int Response_Token_Limit_GPT_4_Free = 400;
    public static final int Response_Token_Limit_GPT_4_Paid = 2500;
    public static final int Response_Token_Limit_GPT_4_Vision_Free = 1200;
    public static final int Response_Token_Limit_GPT_4_Vision_Paid = 2500;

    public static final int Character_Limit_GPT_3_Turbo_Free = 800;
    public static final int Character_Limit_GPT_3_Turbo_Paid = 2800;
    public static final int Character_Limit_GPT_4_Free = 400;
    public static final int Character_Limit_GPT_4_Paid = 1400;
    public static final int Character_Limit_GPT_4_Vision_Free = 1400;
    public static final int Character_Limit_GPT_4_Vision_Paid = 1400;

    public static final int Image_Token_Count = 400;

    public static final int Chat_Context_Select_Query_Limit = 80;

    /* Delays and Cooldowns */
    public static final int Transaction_Status_Apple_Update_Cooldown = 1700;

    /* Caps */
    public static final int Cap_Free_Total_Essays = 3; // This is just a constant sent to the device, which handles everything
    public static final Integer Cap_Chat_Daily_Free = null;
    public static final Integer Cap_Chat_Daily_Paid = null;
    public static final int Cap_Chat_Daily_Paid_Legacy = -1; //-1 is unlimited

    /* URIs for HTTPSServer */
    class URIs {
        class StructuredOutput {
            public static final String SUBDIRECTORY_PREFIX = "/structuredOutput";
            public static final String SUBDIRECTORY_PREFIX_LEGACY = "/structuredOutput";
            public static final String CHECK_IF_CHAT_REQUESTS_IMAGE_REVISION = "/checkIfChatRequestsImageRevision";
            public static final String CLASSIFY_CHAT = "/classifyChat";
            public static final String GENERATE_DRAWERS = "/generateDrawers";
            public static final String GENERATE_FLASH_CARDS = "/generateFlashCards";
            public static final String GENERATE_GOOGLE_QUERY = "/generateGoogleQuery";
            public static final String GENERATE_SUGGESTIONS = "/generateSuggestions";
            public static final String GENERATE_TITLE = "/generateTitle";
        }

        class StructuredOutputOpenRouter {
            public static final String SUBDIRECTORY_PREFIX = "/structuredOutputOpenRouter";
            public static final String CHECK_IF_CHAT_REQUESTS_IMAGE_REVISION = "/checkIfChatRequestsImageRevision";
            public static final String CLASSIFY_CHAT = "/classifyChat";
            public static final String GENERATE_DRAWERS = "/generateDrawers";
            public static final String GENERATE_FLASH_CARDS = "/generateFlashCards";
            public static final String GENERATE_GOOGLE_QUERY = "/generateGoogleQuery";
            public static final String GENERATE_SUGGESTIONS = "/generateSuggestions";
            public static final String GENERATE_TITLE = "/generateTitle";
        }

        public static final String APPLE_ASSOCIATED_DOMAIN = "/.well-known/apple-app-site-association";

        public static final String CHECK_IF_CHAT_REQUESTS_IMAGE_REVISION = "/checkIfChatRequestsImageRevision";
        public static final String CHEF_APP_DEEP_LINK_PAGE = "/chefappdeeplink";
        public static final String CLASSIFY_CHAT = "/classifyChat";
        public static final String DELETE_CHAT_URI = "/deleteChat";
        public static final String GET_CHAT_URI = "/getChat";
        public static final String GET_CHAT_STREAM_URI = "/streamChatDirect";
        public static final String GET_CHAT_STREAM_URI_LEGACY_1 = "/getChatStream";
        public static final String GET_CHAT_STREAM_URI_LEGACY_2 = "/streamChat";
        public static final String GET_CHAT_STREAM_URI_OPENROUTER = "/streamChatOpenRouter";
        public static final String GET_CHAT_WITH_PERSISTENT_IMAGE_WEB_SOCKET = "/streamChatWithPersistentImage";
        public static final String GET_IAP_STUFF_URI = "/getIAPStuff";
        public static final String GET_IMPORTANT_CONSTANTS_URI = "/getImportantConstants";
        public static final String GET_IS_PREMIUM_URI = "/getIsPremium";
        public static final String GET_REMAINING_URI = "/getRemaining";
        public static final String GENERATE_DRAWERS = "/generateDrawers";
        public static final String GENERATE_GOOGLE_QUERY = "/generateGoogleQuery";
        public static final String GENERATE_SUGGESTIONS = "/generateSuggestions";
        public static final String GENERATE_TITLE = "/generateTitle";
        public static final String GENERATE_IMAGE = "/generateImage";
        public static final String GENERATE_SPEECH = "/generateSpeech";
        public static final String GOOGLE_SEARCH_URI = "/googleSearch";
        public static final String OTHER_FC_GENERATE_ASSISTANT_WEBPAGE = "/otherFCGenerateAssistantWebpage";
        public static final String PRINT_TO_CONSOLE = "/printToConsole";
        public static final String REALTIME = "/realtime";
        public static final String REGISTER_APNS = "/registerAPNS";
        public static final String REGISTER_USER_URI = "/registerUser";
        public static final String REGISTER_TRANSACTION_URI = "/registerTransaction";
        public static final String SEND_PUSH_NOTIFICATION = "/sendPushNotification";
        public static final String TRANSCRIBE_SPEECH = "/transcribeSpeech";
        public static final String SUBMIT_FEEDBACK = "/submitFeedback";
        public static final String VALIDATE_AUTHTOKEN = "/validateAuthToken";
    }

    /* Legacy URIs for HTTPServer */
    public static final String GET_DISPLAY_PRICE_URI = "/getDisplayPrice";
    public static final String GET_SHARE_URL_URI = "/getShareURL";
    public static final String VALIDATE_AND_UPDATE_RECEIPT_URI_LEGACY = "/validateAndUpdateReceipt";

    /* Share URL */
    public static final String SHARE_URL = "https://apps.apple.com/us/app/chit-chat-ai-writing-author/id1664039953";

    /* Policy Retrieval Constants */

    /* MySQL Constants */
    public static final String MYSQL_URL = "jdbc:mysql://localhost:3306/chitchat_schema";

    /* Apple Server Constants */
    public static final String Apple_Bundle_ID = "com.acapplications.ChitChat";
    public static final String Apple_Sandbox_APNS_Base_URL = "https://api.sandbox.push.apple.com:443";
    public static final String Apple_APNS_Base_URL = "https://api.push.apple.com:443";
    public static final String Apple_Sandbox_Storekit_Base_URL = "https://api.storekit-sandbox.itunes.apple.com";
    public static final String Apple_Storekit_Base_URL = "https://api.storekit.itunes.apple.com";
    public static final String Apple_In_Apps_URL_Path = "/inApps";
    public static final String Apple_V1_URL_Path = "/v1";
    public static final String Apple_Subscriptions_URL_Path = "/subscriptions";
    public static final String Apple_Get_Subscription_Status_V1_Full_URL_Path = Apple_In_Apps_URL_Path + Apple_V1_URL_Path + Apple_Subscriptions_URL_Path;
    public static final String Apple_APNS_AuthKey_JWS_Path = "keys/AuthKey_3L975VA2YC.p8";
    public static final String Apple_SubscriptionKey_JWS_Path = "keys/SubscriptionKey_PJ323P8QVH.p8";

    public static final String Sandbox_Apple_Verify_Receipt_URL = "https://sandbox.itunes.apple.com/verifyReceipt";
    public static final String Apple_Verify_Receipt_URL = "https://buy.itunes.apple.com/verifyReceipt";
    public static long APPLE_TIMEOUT_MINUTES = 4;

    /* ChatSonic Server Constants */
    public static URI CHATSONIC_URI = URI.create("https://api.writesonic.com/v2/business/content/chatsonic?engine=premium");

    /* Google Constants */
    public static String Google_Search_Base_URL = "https://customsearch.googleapis.com/customsearch/v1";
    public static long GOOGLE_TIMEOUT_MINUTES = 1;

    /* OpenAI Constants */
    public static final URI OPENAI_URI = URI.create("https://api.openai.com/v1/chat/completions");
    public static final URI OPENAPI_URI = URI.create("https://openrouter.ai/api/v1/chat/completions");
    public static long AI_TIMEOUT_MINUTES = 4;
    public static String DEFAULT_MODEL_NAME = "gpt-3.5-turbo";//"gpt-4";
    public static String PAID_MODEL_NAME = "gpt-4-1106-preview";
    public static String DEFAULT_BEHAVIOR = null;
    public static CompletionRole LEGACY_DEFAULT_ROLE = CompletionRole.USER;
    public static int DEFAULT_TEMPERATURE = 1;

}
