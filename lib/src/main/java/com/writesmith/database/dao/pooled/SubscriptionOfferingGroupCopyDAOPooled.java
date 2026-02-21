package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.SubscriptionOfferingGroupCopyDAO;
import com.writesmith.database.model.objects.SubscriptionOfferingGroupCopy;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class SubscriptionOfferingGroupCopyDAOPooled {

    public static SubscriptionOfferingGroupCopy getByGroupId(Integer offeringGroupId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBObjectNotFoundFromQueryException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return SubscriptionOfferingGroupCopyDAO.getByGroupId(conn, offeringGroupId);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
