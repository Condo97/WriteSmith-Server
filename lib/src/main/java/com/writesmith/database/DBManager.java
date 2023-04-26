package com.writesmith.database;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import sqlcomponentizer.DBClient;
import sqlcomponentizer.dbserializer.DBDeserializer;
import sqlcomponentizer.dbserializer.DBSerializer;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.ComponentizedPreparedStatement;
import sqlcomponentizer.preparedstatement.SQLTokens;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.columns.As;
import sqlcomponentizer.preparedstatement.component.columns.aggregate.Count;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;
import sqlcomponentizer.preparedstatement.statement.InsertIntoComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.SelectComponentizedPreparedStatementBuilder;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static sqlcomponentizer.dbserializer.DBSerializer.getTableMap;

public class DBManager {

    /* Insert and Deep Insert */

    public static void insert(Object object) throws DBSerializerException, IllegalAccessException, SQLException, InterruptedException, DBSerializerPrimaryKeyMissingException, InvocationTargetException {
        // Get table name and table map
        String tableName = DBSerializer.getTableName(object.getClass());
        Map<String, Object> tableMap = getTableMap(object);

        // Get if should generate primary key
        boolean shouldGeneratePrimaryKey = false;
        String primaryKeyName = null;
        try {
            // Set the primary key table name
            primaryKeyName = DBSerializer.getPrimaryKeyName(object.getClass());

            // If the primary key is null in the tableMap, set shouldGeneratePrimaryKey to true
            if (tableMap.get(primaryKeyName) == null)
                shouldGeneratePrimaryKey = true;

            // If the primary key has a value, return false
        } catch (DBSerializerPrimaryKeyMissingException e) {
            // If there is no primary key, then leave as false
        }

        // Insert into table with tableMap and build with shouldGeneratePrimaryKey
        ComponentizedPreparedStatement cps = InsertIntoComponentizedPreparedStatementBuilder.forTable(tableName).addColAndVals(tableMap).build(shouldGeneratePrimaryKey);

        // Get connection pool and insert
        Connection conn = SQLConnectionPoolInstance.getConnection();
        List<Map<String, Object>> generatedKeyMaps;
        try {
            generatedKeyMaps = DBClient.updateReturnGeneratedKeys(conn, cps);
        } catch (SQLException e) {
            throw e;
        } finally {
            // Release DB Connection
            SQLConnectionPoolInstance.releaseConnection(conn);
        }

        // If shouldGeneratePrimaryKey, use the first element of the keySet of the first element of generatedKeyMaps to set the primary key TODO: Should this throw an exception
        if (shouldGeneratePrimaryKey) {
            for (String key: generatedKeyMaps.get(0).keySet()) {
                // Check if the generatedKey is a BigInteger and cast to Long
                Object primaryKeyValue = generatedKeyMaps.get(0).get(key);

                if (primaryKeyValue instanceof BigInteger)
                    primaryKeyValue = ((BigInteger)primaryKeyValue).intValue();

                DBDeserializer.setPrimaryKey(object, primaryKeyValue);
                break;
            }
        }

    }

    public static Object deepInsert(Object object, boolean setWithDeepestSubObjectID) throws DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, SQLException, InterruptedException, InvocationTargetException {
        // Create ID to be given by subobject
        Object id = null;

        // Insert sub objects first
        List<Object> subObjects = DBSerializer.getSubObjects(object);
        for (Object subObject: subObjects) {
            // Deep insert subObject
            id = deepInsert(subObject, setWithDeepestSubObjectID);
        }

        // Set primary key as subobject's id if recursive
        if (id != null && setWithDeepestSubObjectID)
            DBDeserializer.setPrimaryKey(object, id);

        // Insert object
        insert(object);

        // If shouldGetID, set the ID as the inserted object's primary key
        if (setWithDeepestSubObjectID) {
            id = DBSerializer.getPrimaryKey(object);
        }

        return id;
    }

    public static void deepInsert(Object object) throws DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, SQLException, InterruptedException, InvocationTargetException {
        // Deep insert recursive with shouldGetID as false
        deepInsert(object, false);
    }

    /* Get All Objects Where */

    public static List<Object> getAllByPrimaryKey(Class dbClass, Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, IllegalAccessException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        String primaryKeyName = DBSerializer.getPrimaryKeyName(dbClass);

        return getAllWhere(dbClass, primaryKeyName, SQLOperators.EQUAL, primaryKey);
    }

    public static List<Object> getAllWhere(Class dbClass, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return getAllWhere(dbClass, Map.of(whereCol, whereVal), operator);
    }

    public static List<Object> getAllWhere(Class dbClass, Map<String, Object> colValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Create PS Component List
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Add SQLOperatorConditions for each colValMap with the commonOperator
        colValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return getAllWhere(dbClass, sqlConditions);
    }

    public static List<Object> getAllWhere(Class dbClass, List<PSComponent> sqlConditions) throws DBSerializerException, InterruptedException, SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        // Create CPS
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName)
                .select(SQLTokens.ALL)
                .where(sqlConditions)
                .build();

        // Get connection from pool and query
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            // Get resultMap
            List<Map<String, Object>> resultMaps = DBClient.query(conn, cps);

            // Create resultArray
            List<Object> resultArray = new ArrayList<>();

            // Deserialize to array of objects of the class dbClass
            for (Map<String, Object> resultMap: resultMaps) {
                // Get objects of the class dbClass
                resultArray.add(DBDeserializer.createObjectFromMap(dbClass, resultMap));
            }

            // Return resultArray
            return resultArray;
        } catch (SQLException e) {
            throw e;
        } finally {
            System.out.println("released");
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    /* Count Object Where */

    public static Long countObjectWhere(Class dbClass, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectWhere(dbClass, Map.of(whereCol, whereVal), operator);
    }

    public static Long countObjectWhere(Class dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException {
        // Create SQL condition list
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Append whereColValMaps with commonOperator SQL condition list
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return countObjectWhere(dbClass, sqlConditions);
    }

    public static Long countObjectWhere(Class dbClass, List<PSComponent> sqlConditions) throws DBSerializerException, InterruptedException, SQLException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);

        // Set tempID
        String tempID = "c";

        // Build componentized prepared statement
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(new As(new Count(SQLTokens.ALL).toString(), tempID).toString()).where(sqlConditions).build();

        // Return result of countWithComponentizedPreparedStatement
        return countWithComponentizedPreparedStatement(cps, tempID);
    }

    /* Count Object Where Inner Join */

    public static Long countObjectWhereInnerJoin(Class dbClass, String whereCol, SQLOperators operator, Object whereVal, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        return countObjectWhereInnerJoin(dbClass, Map.of(whereCol, whereVal), operator, joinClass, joinColumns);
    }

    public static Long countObjectWhereInnerJoin(Class dbClass, Map<String, Object> whereColValMap, SQLOperators commonOperator, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        // Create SQL condition list
        List<PSComponent> sqlConditions = new ArrayList<>();

        // Add each from whereColValMap to sqlConditions using the common operator
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));

        return countObjectWhereInnerJoin(dbClass, sqlConditions, joinClass, joinColumns);
    }

    public static Long countObjectWhereInnerJoin(Class dbClass, List<PSComponent> sqlConditions, Class joinClass, String... joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        return countObjectWhereInnerJoin(dbClass, sqlConditions, joinClass, Arrays.asList(joinColumns));
    }

    public static Long countObjectWhereInnerJoin(Class dbClass, List<PSComponent> sqlConditions, Class joinClass, List<String> joinColumns) throws DBSerializerException, InterruptedException, SQLException {
        // Get tableName
        String tableName = DBSerializer.getTableName(dbClass);
        String joinTableName = DBSerializer.getTableName(joinClass);

        // Set tempID
        String tempID = "c";

        // Build componentized prepared statement
        ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable(tableName).select(new As(new Count(SQLTokens.ALL).toString(), tempID).toString()).where(sqlConditions).innerJoin(joinTableName, joinColumns).build();

        // Return result of countWithComponentizedPreparedStatement
        return countWithComponentizedPreparedStatement(cps, tempID);
    }

    private static Long countWithComponentizedPreparedStatement(ComponentizedPreparedStatement cps, String generatedKeyIdentifier) throws InterruptedException, SQLException {

        // Get connection from pool and query
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            // Get result map
            List<Map<String, Object>> resultMapList = DBClient.query(conn, cps);

            // Should be the first object in the list and the only object in the map
            if (resultMapList.size() > 0) {
                if (resultMapList.get(0).containsKey(generatedKeyIdentifier)) {
                    if (resultMapList.get(0).get(generatedKeyIdentifier) instanceof Long) {
                        return (Long)resultMapList.get(0).get(generatedKeyIdentifier);
                    }
                }
            }

            // Return null if the object did not exist
            return null;

        } catch (SQLException e) {
            throw e;
        } finally {
            // Release connection instance
            System.out.println("released");
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    /* Count Object By Column Where */

    public static Integer countObjectByColumnWhere(Class dbClass, String byColumn, String whereCol, SQLOperators operator, Object whereVal) throws DBSerializerException, SQLException, InterruptedException {
        return countObjectByColumnWhere(dbClass, byColumn, Map.of(whereCol, whereVal), operator);
    }

    public static Integer countObjectByColumnWhere(Class dbClass, String byColumn, Map<String, Object> whereColValMap, SQLOperators commonOperator) throws DBSerializerException, SQLException, InterruptedException {
        List<PSComponent> sqlConditions = new ArrayList<>();
        whereColValMap.forEach((k, v) -> sqlConditions.add(new SQLOperatorCondition(k, commonOperator, v)));
        return countObjectByColumnWhere(dbClass, byColumn, sqlConditions);
    }

    public static Integer countObjectByColumnWhere(Class dbClass, String byColumn, List<PSComponent> sqlConditions) throws DBSerializerException, SQLException, InterruptedException {
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
