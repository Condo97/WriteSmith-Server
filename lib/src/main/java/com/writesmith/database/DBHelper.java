package com.writesmith.database;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import sqlcomponentizer.DBClient;
import sqlcomponentizer.dbserializer.DBSerializer;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.ComponentizedPreparedStatement;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;
import sqlcomponentizer.preparedstatement.statement.SelectComponentizedPreparedStatementBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DBHelper {


    public static Integer countObjectWhereByColumn(Object dbObject, String whereCol, SQLOperators operator, Object whereVal, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhereByColumn(dbObject, Map.of(whereCol, whereVal), operator, byColumn);
    }

    public static Integer countObjectWhereByColumn(Class dbClass, String whereCol, SQLOperators operator, Object whereVal, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhereByColumn(dbClass, Map.of(whereCol, whereVal), operator, byColumn);
    }

    public static Integer countObjectWhereByColumn(Object dbObject, Map<String, Object> whereColValMap, SQLOperators commonOperator, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhereByColumn(dbObject.getClass(), whereColValMap, commonOperator, byColumn);
    }

    public static Integer countObjectWhereByColumn(Class dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        List<PSComponent> sqlConditions = new ArrayList<>();
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));
        return countObjectWhereByColumn(dbClass, sqlConditions, byColumn);
    }

    public static Integer countObjectWhereByColumn(Object dbObject, List<PSComponent> sqlConditions, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhereByColumn(dbObject.getClass(), sqlConditions, byColumn);
    }

    public static Integer countObjectWhereByColumn(Class dbClass, List<PSComponent> sqlConditions, String byColumn) throws DBSerializerException, SQLException, InterruptedException {
        // Table, row=v
        String tableName = DBSerializer.getTableName(dbClass);

        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(byColumn).where(sqlConditions).build();

        // Get connection from pool and query
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            List<Map<String, Object>> resultMap = DBClient.query(conn, cps);

            return resultMap.size();
        } catch (SQLException e) {
            throw e;
        } finally {
            // Release connection instance
            System.out.println("released");
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }
}
