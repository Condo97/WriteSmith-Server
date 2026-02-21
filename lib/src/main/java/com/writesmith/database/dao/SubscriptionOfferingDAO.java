package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.SubscriptionOffering;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SubscriptionOfferingDAO {

    public static SubscriptionOffering getActive(Connection conn) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<SubscriptionOffering> offerings = DBManager.selectAllWhereOrderByLimit(
                conn,
                SubscriptionOffering.class,
                Map.of(
                        DBRegistry.Table.SubscriptionOffering.is_active, true
                ),
                SQLOperators.EQUAL,
                List.of(
                        DBRegistry.Table.SubscriptionOffering.id
                ),
                OrderByComponent.Direction.DESC,
                1
        );

        if (offerings.isEmpty())
            return null;

        return offerings.get(0);
    }

}
