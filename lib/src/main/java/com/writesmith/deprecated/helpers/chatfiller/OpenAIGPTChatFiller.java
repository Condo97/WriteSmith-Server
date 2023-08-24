//package com.writesmith.deprecated.helpers.chatfiller;
//
//import com.oaigptconnector.model.OAIClient;
//import com.oaigptconnector.model.exception.OpenAIGPTException;
//import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
//import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
//import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
//import com.writesmith.Constants;
//import com.writesmith.common.IntegerFromBoolean;
//import com.writesmith.common.exceptions.CapReachedException;
//import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
//import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
//import com.writesmith.core.apple.iapvalidation.RecentReceiptValidator;
//import com.writesmith.deprecated.helpers.IMutableInteger;
//import com.writesmith.deprecated.helpers.MutableInteger;
//import com.writesmith.keys.Keys;
//import com.writesmith.model.database.objects.ChatLegacy;
//import com.writesmith.model.database.objects.Receipt;
//import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
//import sqlcomponentizer.dbserializer.DBSerializerException;
//import sqlcomponentizer.preparedstatement.component.PSComponent;
//import sqlcomponentizer.preparedstatement.component.condition.SQLNullCondition;
//import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
//import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.sql.SQLException;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//public class OpenAIGPTChatFiller {
//
//    public static IntegerFromBoolean getCapFromPremium = t -> t ? Constants.Cap_Chat_Daily_Paid_Legacy : Constants.Cap_Chat_Daily_Free;
//
//    public static void fillChatIfAble(ChatLegacy chatLegacy, Boolean usePaidModel) throws SQLException, OpenAIGPTException, CapReachedException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
//        // Get validated and updated receipt
//        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(chatLegacy);
//        fillChatIfAble(chatLegacy, receipt, usePaidModel);
//    }
//
//    public static void fillChatIfAble(ChatLegacy chatLegacy, Receipt receipt, Boolean usePaidModel) throws SQLException, OpenAIGPTException, CapReachedException, PreparedStatementMissingArgumentException, IOException, InterruptedException, DBSerializerException {
//        // We want that Integer bc if something extends it they should be able to know the count :) But also maybe we can just say it's a way to set the count? if -1 then let it auto set the count or sumptn?
//        fillChatIfAble(chatLegacy, receipt, new MutableInteger(-1), usePaidModel);
//    }
//
//    /***
//     * Fills chat if able! If count is set to -1, it will use db.countTodaysChats() as the value, otherwise that will be the count
//     *
//     * @param chatLegacy keeps reference
//     * @param receipt keeps reference
//     * @param count keeps reference (so we can get the count from DB just once even in subclasses ;) )
//     * @throws IOException
//     * @throws InterruptedException
//     * @throws SQLException
//     * @throws PreparedStatementMissingArgumentException
//     * @throws CapReachedException
//     * @throws OpenAIGPTException
//     */
//    protected static void fillChatIfAble(ChatLegacy chatLegacy, Receipt receipt, IMutableInteger count, Boolean usePaidModel) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, CapReachedException, OpenAIGPTException, DBSerializerException {
//        // Get cooldown end timestamp TODO
//
//        // Verify cooldown end timestamp is before current date, otherwise throw exception TODO
//
//        // Get premium value
//        boolean isPremium = receipt != null && !receipt.isExpired();
//
//        // Get cap corresponding to premium (!receipt.isExpired()) status
//        int cap = getCapFromPremium.getInt(isPremium);
//
//        // Setup the SQL conditions manually since there are different ones depending on the column
//        List<PSComponent> sqlConditions = List.of(
//                new SQLOperatorCondition("date", SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1))),
//                new SQLOperatorCondition("user_id", SQLOperators.EQUAL, chatLegacy.getUserID()),
//                new SQLNullCondition("ai_text", false)
//        );
//
//        // Get count of today's chats, or use integer as override
//        count.set(count.intValue() == -1 ? DBManager.countObjectByColumnWhere(chatLegacy.getClass(), "chat_id", sqlConditions) : -1); // TODO: - Give the option (and use in this case) to use the primary key as byColumn.. also as noted before, chat.getUserID() is redundant as DBSerializer can theoretically look up and get the userID from the user_id column name
//
//        // Verify count is not -1 (infinite) and count with the additional receipt is not greater than cap, otherwise throw exception
//        if (cap != -1 && count.intValue() + 1 > cap) throw new CapReachedException("Cap Reached for User");
//
//        // Get the token limit if there is one
//        int tokenLimit = isPremium ? Constants.Response_Token_Limit_Paid : Constants.Response_Token_Limit_Free;
//
//        // Get model name for paid if using paid and isPremium or free
//        String modelName = isPremium && usePaidModel ? Constants.PAID_MODEL_NAME : Constants.DEFAULT_MODEL_NAME;
////        String modelName = isPremium ? Constants.PAID_MODEL_NAME : Constants.DEFAULT_MODEL_NAME;
//
//        // GENERATE THE CHAT! :)
//        fillChat(chatLegacy, modelName, tokenLimit);
//
//        // If filled successfully, we can increment the count.. This is pretty much just for ChatWrapper to get remaining to save a db call... The count could totally be gotten again in ..ChatWrapperFiller by counting the chats, which would return the proper number if the chat was properly generated, but we can also just do that here and save that db call. so just chill with the polymorphism
//        if (chatLegacy.getAiText() != null) count.set(count.intValue() + 1); // (ree)
//    }
//
//    /***
//     * Fills the chat with a response and finish reason from OpenAI GPT!
//     *
//     * @param chatLegacy
//     * @throws OpenAIGPTException
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    private static void fillChat(ChatLegacy chatLegacy, String modelName, int tokenLimit) throws OpenAIGPTException, IOException, InterruptedException {
//        OAIChatCompletionRequestMessage promptMessageRequest = new OAIChatCompletionRequestMessage(Constants.LEGACY_DEFAULT_ROLE, chatLegacy.getUserText());
//        OAIChatCompletionRequest promptRequest = OAIChatCompletionRequest.build(modelName, tokenLimit, Constants.DEFAULT_TEMPERATURE, Arrays.asList(promptMessageRequest));
//
//        try {
//            OAIGPTChatCompletionResponse response = OAIClient.postChatCompletion(promptRequest, Keys.openAiAPI);
//            if (response.getChoices().length > 0) {
//                chatLegacy.setAiText(response.getChoices()[0].getMessage().getContent());
//                chatLegacy.setFinishReason(response.getChoices()[0].getFinish_reason());
//            }
//        } catch (OpenAIGPTException e) {
//            //TODO: - Process AI Error Response
//            throw e;
//        }
//    }
//}
