//package com.writesmith._deprecated.helpers.chatfiller;
//
//import com.writesmith.database.model.DBRegistry;
//import sqlcomponentizer.dbserializer.DBSerializable;
//
//import java.time.LocalDateTime;
//
//@DBSerializable(tableName = DBRegistry.Table.ChatLegacy1.TABLE_NAME)
//public class ChatLegacy1Wrapper extends ChatLegacy1 {
//    private Long dailyChatsRemaining;
//    private boolean isPremium;
//
//    public ChatLegacy1Wrapper(ChatLegacy1 chatLegacy1) {
//        super(chatLegacy1.getID(), chatLegacy1.getUserID(), chatLegacy1.getUserText(), chatLegacy1.getAiText(), chatLegacy1.getFinishReason(), chatLegacy1.getDate());
//
//        this.dailyChatsRemaining = 0l;
//        isPremium = false;
//    }
//
//    public ChatLegacy1Wrapper(Integer userID, String userText, LocalDateTime date) {
//        super(userID, userText, date);
//
//        this.dailyChatsRemaining = 0l;
//        this.isPremium = false;
//    }
//
//    public ChatLegacy1Wrapper(Integer userID, String userText, LocalDateTime date, Long dailyChatsRemaining, boolean isPremium) {
//        super(userID, userText, date);
//        this.dailyChatsRemaining = dailyChatsRemaining;
//        this.isPremium = isPremium;
//    }
//
//    public ChatLegacy1Wrapper(Integer id, Integer userID, String userText, String aiText, String finishReason, LocalDateTime date, Long dailyChatsRemaining, boolean isPremium) {
//        super(id, userID, userText, aiText, finishReason, date);
//        this.dailyChatsRemaining = dailyChatsRemaining;
//        this.isPremium = isPremium;
//    }
//
//    public Long getDailyChatsRemaining() {
//        return dailyChatsRemaining;
//    }
//
//    public void setDailyChatsRemaining(Long dailyChatsRemaining) {
//        this.dailyChatsRemaining = dailyChatsRemaining;
//    }
//
//    public boolean isPremium() {
//        return isPremium;
//    }
//
//    public void setPremium(boolean premium) {
//        isPremium = premium;
//    }
//}
