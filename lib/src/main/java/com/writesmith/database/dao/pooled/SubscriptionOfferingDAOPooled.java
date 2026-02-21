package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.SubscriptionOfferingDAO;
import com.writesmith.database.model.objects.SubscriptionOffering;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class SubscriptionOfferingDAOPooled {

    public static SubscriptionOffering getActive() throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return SubscriptionOfferingDAO.getActive(conn);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
