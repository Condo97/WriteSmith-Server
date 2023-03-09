package writesmith.constants;

import java.net.URI;

public final class Constants {

    private Constants() {
    }

    /* In-App Purchases Pricing */
    public static final int DEFAULT_PRICE_INDEX = 0;
    public static final String WEEKLY_PRICE = "6.95";
    public static final String WEEKLY_NAME = "chitchatultra";
    public static final String MONTHLY_PRICE = "19.99";
    public static final String MONTHLY_NAME = "ultramonthly";
    public static final String YEARLY_PRICE = "49.99";
    public static final String YEARLY_NAME = "chitchatultrayearly";

    /* Timeouts and Delays */
    public static final int Delay_Seconds_Free = 15;
    public static final int Delay_Seconds_Paid = 0;

    /* Token Limits */
    public static final int Token_Limit_Free = 40;
    public static final int Token_Limit_Paid = 1000;

    public static final int Delay_Seconds_Premium_Check = 60;

    /* Caps */
    public static final int Cap_Free_Total_Essays = 3; // This is just a constant sent to the device, which handles everything
    public static final int Cap_Chat_Daily_Free = 10;
    public static final int Cap_Chat_Daily_Paid = -1; //-1 is unlimited

    /* URIs for HTTPSServer */
    public static final String GET_CHAT_URI = "/getChat";
    public static final String GET_REMAINING_URI = "/getRemaining";
    public static final String GENERATE_IMAGE_URI = "/generateImage";
    public static final String REGISTER_USER_URI = "/registerUser";
    public static final String GET_IMPORTANT_CONSTANTS_URI = "/getImportantConstants";

    public static final String GET_PRODUCTS_URI = "/getProducts";
    public static final String FULL_VALIDATE_PREMIUM_URI = "/validateAndUpdateReceipt";
    public static final String QUICK_VALIDATE_PREMIUM_URI = "/quickValidateUserIsPremium";
    public static final String GET_IAP_STUFF_URI = "/getIAPStuff";
    public static final String PRIVACY_POLICY_URI = "/privacyPolicy.html";
    public static final String TERMS_AND_CONDITIONS_URI = "/termsAndConditions.html";
    public static final String PRINT_ALL_ACTIVE_SUBSCRIPTIONS_URI = "/printAllActiveSubscriptions";

    /* Legacy URIs for HTTPServer */
    public static final String GET_DISPLAY_PRICE_URI = "/getDisplayPrice";
    public static final String GET_SHARE_URL_URI = "/getShareURL";

    /* Share URL */
    public static final String SHARE_URL = "https://apps.apple.com/us/app/chit-chat-ai-writing-author/id1664039953";

    /* Policy Retrieval Constants */
    public static final int JSON_RETRIEVAL = 0;
    public static final int PRIVACY_POLICY_RETRIEVAL = 1;
    public static final int TERMS_AND_CONDITIONS_RETRIEVAL = 2;
    public static final String PRIVACY_POLICY_LOCATION = "policies/privacy.html";
    public static final String TERMS_AND_CONDITIONS_LOCATION = "policies/termsandconditions.html";

    /* Apple Server Constants */
    public static String Sandbox_Apple_URL = "https://sandbox.itunes.apple.com/verifyReceipt";
    public static String Apple_URL = "https://buy.itunes.apple.com/verifyReceipt";
    public static long APPLE_TIMEOUT_MINUTES = 4;

    /* ChatSonic Server Constants */
    public static URI CHATSONIC_URI = URI.create("https://api.writesonic.com/v2/business/content/chatsonic?engine=premium");

    /* OpenAI Constants */
    public static URI OPENAI_URI = URI.create("https://api.openai.com/v1/completions");
    public static long AI_TIMEOUT_MINUTES = 4;
    public static String Model_Name = "text-davinci-003";
    public static int Temperature = 0;

    /* Success and Exceptions */
    public static final int SUCCESS_Success = 1;
    public static final int SUCCESS_JSONException = 4;
    public static final int SUCCESS_OddRetrievalValue = 10;
    public static final int SUCCESS_IOException = 45;
    public static final int SUCCESS_InterruptedException = 46;
    public static final int SUCCESS_SQLException = 47;
    public static final int SUCCESS_MissingRowException = 48;
    public static final int SUCCESS_DuplicateRowException = 49;
    public static final int SUCCESS_InvalidIndexException = 50;
    public static final int SUCCESS_GenerateCapException = 51;
    public static final int SUCCESS_CooldownException = 52;
    public static final int SUCCESS_UnavailableQualityException = 53;
    public static final int SUCCESS_ChatSaveException = 54;
    public static final int SUCCESS_ReceiptExistsException = 55;
    public static final int SUCCESS_GenerateErrorException = 56;
    public static final int SUCCESS_StillGeneratingException = 57;
    public static final int SUCCESS_SomethingWeirdHappenedException = 69;
    public static final int SUCCESS_UnhandledException = 99;

}
