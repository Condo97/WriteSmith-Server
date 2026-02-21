package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.SubscriptionOfferingGroupProductDAO;
import com.writesmith.database.model.objects.SubscriptionOfferingGroupProduct;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SubscriptionOfferingGroupProductDAOPooled {

    public static List<SubscriptionOfferingGroupProduct> getByGroupId(Integer offeringGroupId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return SubscriptionOfferingGroupProductDAO.getByGroupId(conn, offeringGroupId);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
