package com.writesmith.database.preparedstatement;

import com.writesmith.database.objects.DatabaseObject;
import com.writesmith.database.objects.Table;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdatePreparedStatement extends PreparedStatementBuilder {

    Map<String, Object> toSetColumnValueMap, whereColumnValueMap;
    public UpdatePreparedStatement(Connection conn, Table table) {
        super(conn, Command.UPDATE, table);

        toSetColumnValueMap = new HashMap<>();
        whereColumnValueMap = new HashMap<>();
    }

    public void addToSetColumnValue(String key, Object object) {
        toSetColumnValueMap.put(key, object);
    }

    public void addToSetColumnValue(Map<String, Object> toSetColumnValueMap) {
        this.toSetColumnValueMap.putAll(toSetColumnValueMap);
    }

    public void addWhereColumnValue(String key, Object object) {
        whereColumnValueMap.put(key, object);
    }

    public void addWhereColumnValue(Map<String, Object> whereColumnValueMap) {
        this.whereColumnValueMap.putAll(whereColumnValueMap);
    }

    @Override
    public PreparedStatement build() throws SQLException, PreparedStatementMissingArgumentException {
        // Example: UPDATE Table SET setVal1=? WHERE whereVal1=?;

        // Ensure required arguments exist
        if (toSetColumnValueMap.size() == 0 || whereColumnValueMap.size() == 0) throw new PreparedStatementMissingArgumentException("Missing argument in UpdatePreparedStatement.");

        List<Object> orderedValues = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(UPDATE + SPACE + table + SPACE + SET + SPACE + getToSetKeyPlaceholderString(orderedValues) + SPACE + WHERE + SPACE + getWhereKeyPlaceholderString(orderedValues) + TERMINATOR);

        setOrderedValues(ps, orderedValues);

        return ps;
    }

    public String getToSetKeyPlaceholderString(List<Object> orderedValues) {
        if (toSetColumnValueMap.size() == 0) return "";

        // Build to set key placeholder strings
        StringBuilder sb = new StringBuilder();
        toSetColumnValueMap.forEach((k, v) -> {
            sb.append(k + EQUAL + PLACEHOLDER + SEPARATOR);
            orderedValues.add(v);
        });

        sb.delete(sb.length() - SEPARATOR.length(), sb.length());

        return sb.toString();
    }

    /***
     * Gets the key placeholder string for the whereColumnValueMap
     * Note: WHERE condition is always AND
     * @param orderedValues
     * @return
     */
    public String getWhereKeyPlaceholderString(List<Object> orderedValues) {
        if (whereColumnValueMap.size() == 0) return "";

        // Build where key placeholder string
        StringBuilder sb = new StringBuilder();
        whereColumnValueMap.forEach((k, v) -> {
            sb.append(k + EQUAL + PLACEHOLDER + SPACE + AND + SPACE);
            orderedValues.add(v);
        });

        sb.delete(sb.length() - SPACE.length() - AND.length() - SPACE.length(), sb.length());

        return sb.toString();
    }
}
