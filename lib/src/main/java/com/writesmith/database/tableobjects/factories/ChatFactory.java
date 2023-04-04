package com.writesmith.database.tableobjects.factories;

import com.writesmith.database.tableobjects.Chat;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatFactory extends DBObjectFactory {

    public static Chat createInDB(Integer userID, String userText, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, IllegalAccessException, InterruptedException {
        Chat chat = create(userID, userText, date);

        insertWithAutoIncrementingPrimaryKey(chat);

        return chat;
    }

    /***
     * Creates a chat with no chatID. Since no primary key is added to the Chat object when returning, this method is private
     *
     * @param userID
     * @param userText
     * @param date
     *
     * @return Chat object with empty primary key
     */

    private static Chat create(Integer userID, String userText, LocalDateTime date) {
        return new Chat(userID, userText, date);
    }
}
