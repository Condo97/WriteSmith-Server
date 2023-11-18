package com.writesmith.util.calculators;

import com.writesmith.Constants;
import com.writesmith.util.IntegerFromBoolean;
import com.writesmith.database.dao.helpers.ChatCountHelper;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.sql.SQLException;

public class ChatRemainingCalculator {

    public static IntegerFromBoolean getCapFromPremium = t -> t ? Constants.Cap_Chat_Daily_Paid : Constants.Cap_Chat_Daily_Free;

    public static Long calculateRemaining(Integer userID, boolean isPremium) throws DBSerializerException, InterruptedException, SQLException {
        // Get count of today's chats
        Long count = ChatCountHelper.countTodaysGeneratedChats(userID);

        // Get cap
        Integer cap = getCapFromPremium.getInt(isPremium);

        // Return null if cap is null
        if (cap == null)
            return null;

        return cap - count;
    }

}
