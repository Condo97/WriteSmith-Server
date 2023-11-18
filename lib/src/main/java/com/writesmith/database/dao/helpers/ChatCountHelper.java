package com.writesmith.database.dao.helpers;

import com.dbclient.DBManager;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.model.objects.Chat;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.Sender;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ChatCountHelper {

    public static Long countTodaysGeneratedChats(Integer userID) throws DBSerializerException, SQLException, InterruptedException {
        // TODO: Should this pool access be here? Can we move this to GeneratedChatDAO and use GeneratedChatDAO, or is this a special case because it uses both Conversation and Chat, and not GeneratedChat, so would an independent class like this be the best case, and therefore should this have a pool or what?
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return countTodaysGeneratedChats(conn, userID);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static Long countTodaysGeneratedChats(Connection conn, Integer userID) throws DBSerializerException, InterruptedException, SQLException {
        // Build SQL conditions
        SQLOperatorCondition userIDCondition = new SQLOperatorCondition(DBRegistry.Table.Conversation.user_id, SQLOperators.EQUAL, userID);
        SQLOperatorCondition senderNotUserCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.sender, SQLOperators.NOT_EQUAL, Sender.USER.toString());
        SQLOperatorCondition dateCondition = new SQLOperatorCondition(DBRegistry.Table.Chat.date, SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1)));

        List<PSComponent> sqlConditions = List.of(userIDCondition, senderNotUserCondition, dateCondition);

        // Get chats from today for the conversation, need to do an inner join with Chat
        return DBManager.countObjectWhereInnerJoin(
                conn,
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
