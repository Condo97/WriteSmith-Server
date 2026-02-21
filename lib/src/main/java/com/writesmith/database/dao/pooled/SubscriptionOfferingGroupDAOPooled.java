package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.SubscriptionOfferingGroupDAO;
import com.writesmith.database.model.objects.SubscriptionOfferingGroup;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SubscriptionOfferingGroupDAOPooled {

    public static List<SubscriptionOfferingGroup> getByOfferingId(Integer subscriptionOfferingId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return SubscriptionOfferingGroupDAO.getByOfferingId(conn, subscriptionOfferingId);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
