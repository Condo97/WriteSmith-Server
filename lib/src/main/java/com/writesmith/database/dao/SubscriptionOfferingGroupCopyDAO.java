package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.SubscriptionOfferingGroupCopy;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SubscriptionOfferingGroupCopyDAO {

    public static SubscriptionOfferingGroupCopy getByGroupId(Connection conn, Integer offeringGroupId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBObjectNotFoundFromQueryException {
        List<SubscriptionOfferingGroupCopy> copies = DBManager.selectAllWhere(
                conn,
                SubscriptionOfferingGroupCopy.class,
                DBRegistry.Table.SubscriptionOfferingGroupCopy.offering_group_id,
                SQLOperators.EQUAL,
                offeringGroupId
        );

        if (copies.isEmpty())
            throw new DBObjectNotFoundFromQueryException("No copy found for offering group id: " + offeringGroupId);

        return copies.get(0);
    }

}
