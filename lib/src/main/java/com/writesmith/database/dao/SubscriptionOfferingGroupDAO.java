package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.SubscriptionOfferingGroup;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SubscriptionOfferingGroupDAO {

    public static List<SubscriptionOfferingGroup> getByOfferingId(Connection conn, Integer subscriptionOfferingId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        return DBManager.selectAllWhere(
                conn,
                SubscriptionOfferingGroup.class,
                DBRegistry.Table.SubscriptionOfferingGroup.subscription_offering_id,
                SQLOperators.EQUAL,
                subscriptionOfferingId
        );
    }

}
