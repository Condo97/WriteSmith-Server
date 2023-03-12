package com.writesmith.database.preparedstatement;

import com.writesmith.database.objects.DatabaseObject;
import com.writesmith.database.objects.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectPreparedStatement extends PreparedStatementBuilder {

    public enum Order {
        ASC("ASC"),
        DESC("DESC");

        public String string;

        Order(String string) { this.string = string; }
    }

    final String SCOPE_DEFAULT = "*";

    // ORDER BY Variables
    private Order order = Order.ASC;

    private Map<String, Object> whereMap;
    private List<String> orderByColumns;
    private String betweenCol;
    private Object betweenVal1, betweenVal2;
    private int limit = 0;

    public SelectPreparedStatement(Connection conn, Table table) {
        super(conn, Command.SELECT, table);

        whereMap = new HashMap<>();
        orderByColumns = new ArrayList<>();
    }

    @Override
    public PreparedStatement build() throws SQLException {
        // Example: SELECT ?, ? FROM Table WHERE variable=? ORDER BY ? ? LIMIT ?;

        // Setup Prepared Statement
        List<Object> orderedValues = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(SELECT + SPACE + getScopePlaceholderString(orderedValues) + SPACE + FROM + SPACE + table + getWherePlaceholderString(orderedValues) + getOrderByPlaceholderString() + getBetweenPlaceholderString(orderedValues) + getLimitPlaceholderString() + TERMINATOR);

        // Setup Parameters List
        setOrderedValues(ps, orderedValues);

        return ps;
    }

    public void addScope(String scope) {
        this.scope.add(scope);
    }

    public void addScope(List<String> scope) {
        this.scope.addAll(scope);
    }

    public void addWhere(String key, Object value) {
        whereMap.put(key, value);
    }

    public void addWhere(HashMap<String, Object> whereMap) {
        this.whereMap.putAll(whereMap);
    }

    public void addOrderByColumn(String key) {
        orderByColumns.add(key);
    }

    public void addOrderByColumn(List<String> orderByColumns) {
        this.orderByColumns.addAll(orderByColumns);
    }

    public void setBetween(String col, Object val1, Object val2) {
        betweenCol = col;
        betweenVal1 = val1;
        betweenVal2 = val2;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getScopePlaceholderString(List<Object> orderedValues) {
        if (scope.size() == 0) return SCOPE_DEFAULT;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < scope.size(); i++) {
//            sb.append("?");
            sb.append(scope.get(i));
//            orderedValues.add(scope.get(i));

            if (i < scope.size() - 1) sb.append(", ");
        }

        return sb.toString();
    }

    private String getWherePlaceholderString(List<Object> orderedValues) {
        if (whereMap.size() == 0) return "";

        // Build where placeholders with keys
        StringBuilder sb = new StringBuilder();
        sb.append(SPACE + WHERE + SPACE);

        whereMap.forEach((k, v) -> {
            sb.append(k + EQUAL + PLACEHOLDER + SEPARATOR);
            orderedValues.add(v);
        });

        // Trim end ", "
        sb.delete(sb.length() - 2, sb.length());

        return sb.toString();
    }

    private String getOrderByPlaceholderString() {
        if (orderByColumns.size() == 0) return "";

        // Build simple ORDER BY [?] ASC LIMIT ? string
        StringBuilder sb = new StringBuilder();
        sb.append(SPACE + ORDER_BY + SPACE);

        // Append columns placeholder
        orderByColumns.forEach(v -> {
            sb.append(v + SEPARATOR);
        });

        // Trim end ", "
        sb.delete(sb.length() - 2, sb.length());

        // Add order, ASC or DESC
        sb.append(SPACE);
        sb.append(order);

        return sb.toString();
    }

    private String getBetweenPlaceholderString(List<Object> orderedValues) {
        if (betweenCol == null || betweenVal1 == null || betweenVal2 == null) return "";

        // Add to orderedValues list
        orderedValues.add(betweenVal1);
        orderedValues.add(betweenVal2);

        StringBuilder sb = new StringBuilder();
        if (whereMap.size() > 0) {
            // Build simple AND ...
            sb.append(SPACE + AND + SPACE);
        } else {
            // or.. WHERE ...
            sb.append(SPACE + WHERE + SPACE);
        }

        // Add columnVal BETWEEN ? and ?
        sb.append(betweenCol + SPACE + BETWEEN + SPACE + PLACEHOLDER + SPACE + AND + SPACE + PLACEHOLDER);

        return sb.toString();
    }

    private String getLimitPlaceholderString() {
        if (limit == 0) return "";

        // Return simple LIMIT n string
        return SPACE + LIMIT + SPACE + limit;
    }
}
