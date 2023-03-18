package com.writesmith.database.tableobjects.factories;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.DBObject;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import sqlcomponentizer.DBClient;
import sqlcomponentizer.dbserializer.DBDeserializer;
import sqlcomponentizer.dbserializer.DBSerializer;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.ComponentizedPreparedStatement;
import sqlcomponentizer.preparedstatement.statement.InsertIntoComponentizedPreparedStatementBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static sqlcomponentizer.dbserializer.DBSerializer.getTableMap;

public class DBObjectFactory {

    public static void insertWithAutoIncrementingPrimaryKey(DBObject object) throws DBSerializerException, DBSerializerPrimaryKeyMissingException, IllegalAccessException, SQLException, AutoIncrementingDBObjectExistsException {
        // Insert existing fields, getting generated keys and setting primary key to the first corresponding generated primary key
        String tableName = DBSerializer.getTableName(object);
        String primaryKeyName = DBSerializer.getPrimaryKeyName(object);
        Map<String, Object> tableMap = getTableMap(object);

        // If there is a value for the primaryKey in the DBObject tableMap, then throw an exception stating there is already an ID for this object
        if (tableMap.get(primaryKeyName) != null) throw new AutoIncrementingDBObjectExistsException("ID exists for object " + object.getClass());

        System.out.println(tableMap);

        // Insert into table cols and vals
        ComponentizedPreparedStatement cps = InsertIntoComponentizedPreparedStatementBuilder.forTable(tableName).addColAndVals(tableMap).build(true);

        // Get connection from pool and insert
        Connection conn = SQLConnectionPoolInstance.getConnection();
        List<Map<String, Object>> generatedKeysMapList;
        try {
            generatedKeysMapList = DBClient.updateReturnGeneratedKeys(conn, cps);
        } catch (SQLException e) {
            throw e;
        } finally {
            // Release DB connection
            SQLConnectionPoolInstance.releaseConnection(conn);
        }

        // There should only ever be one map, but just in case, loop through and act on the first element where primaryKeyName is a key, then set its value as the primary key using DBSerializer TODO: - Should this throw an exception? What if it is empty?
        for (Map<String, Object> generatedKeysMap: generatedKeysMapList) {
            for (String key: generatedKeysMap.keySet()) {
                if (key.equals(primaryKeyName))
                    DBDeserializer.setPrimaryKey(object, generatedKeysMap.get(key));
            }
        }
    }
}
