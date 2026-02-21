package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.SubscriptionOfferingGroupProduct;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class SubscriptionOfferingGroupProductDAO {

    public static List<SubscriptionOfferingGroupProduct> getByGroupId(Connection conn, Integer offeringGroupId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        return DBManager.selectAllWhereOrderByLimit(
                conn,
                SubscriptionOfferingGroupProduct.class,
                Map.of(
                        DBRegistry.Table.SubscriptionOfferingGroupProduct.offering_group_id, offeringGroupId
                ),
                SQLOperators.EQUAL,
                List.of(
                        DBRegistry.Table.SubscriptionOfferingGroupProduct.position
                ),
                OrderByComponent.Direction.ASC,
                100
        );
    }

}
