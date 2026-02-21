package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.UserOfferingAssignment;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class UserOfferingAssignmentDAO {

    public static UserOfferingAssignment getByUserAndOffering(Connection conn, Integer userId, Integer subscriptionOfferingId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<UserOfferingAssignment> assignments = DBManager.selectAllWhere(
                conn,
                UserOfferingAssignment.class,
                Map.of(
                        DBRegistry.Table.UserOfferingAssignment.user_id, userId,
                        DBRegistry.Table.UserOfferingAssignment.subscription_offering_id, subscriptionOfferingId
                ),
                SQLOperators.EQUAL
        );

        if (assignments.isEmpty())
            return null;

        return assignments.get(0);
    }

    public static void insert(Connection conn, UserOfferingAssignment assignment) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, assignment);
    }

}
