package com.writesmith.database.managers.helpers;

import com.writesmith.database.DBManager;
import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ChatCountHelper {

    public static Long countTodaysGeneratedChats(Integer userID) throws DBSerializerException, InterruptedException, SQLException {
        // Build SQL conditions
        SQLOperatorCondition userIDCondition = new SQLOperatorCondition(DBRegistry.Table.Conversation.user_id, SQLOperators.EQUAL, userID);
        SQLOperatorCondition senderNotUserCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.sender, SQLOperators.NOT_EQUAL, Sender.USER.toString());
        SQLOperatorCondition dateCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.date, SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1)));

        List<PSComponent> sqlConditions = List.of(userIDCondition, senderNotUserCondition, dateCondition);

        // Get chats from today for the conversation, need to do an inner join with Chat
        return DBManager.countObjectWhereInnerJoin(
                Conversation.class,
                sqlConditions,
                Chat.class,
                DBRegistry.Table.Conversation.conversation_id
        );
    }

//    public static Long countTodaysGeneratedChats(Integer userID) throws DBSerializerException, InterruptedException, SQLException {
//        // Build SQL conditions
//        SQLOperatorCondition userIDCondition = new SQLOperatorCondition(DBRegistry.Table.Conversation.user_id, SQLOperators.EQUAL, userID);
//        SQLOperatorCondition senderNotUserCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.sender, SQLOperators.NOT_EQUAL, Sender.USER.toString());
//        SQLOperatorCondition dateCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.date, SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1)));
//
//        List<PSComponent> sqlConditions = List.of(userIDCondition, senderNotUserCondition, dateCondition);
//
//        // Get chats from today for the conversation, need to do an inner join with Chat
//        return DBManager.countObjectWhereInnerJoin(
//                Conversation.class,
//                sqlConditions,
//                Chat.class,
//                DBRegistry.Table.Conversation.conversation_id
//        );
//    }

}
