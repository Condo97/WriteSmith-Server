package writesmith.server.database.preparedstatement;

import writesmith.exceptions.PreparedStatementMissingArgumentException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertIntoPreparedStatement extends PreparedStatementBuilder {

    public Map<String, Object> columnValueMap;

    public InsertIntoPreparedStatement(Connection conn, Command command, String table) {
        super(conn, command, table);

        columnValueMap = new HashMap<>();
    }

    public InsertIntoPreparedStatement(Connection conn, Command command, String table, HashMap<String, Object> columnValueMap) {
        super(conn, command, table);

        this.columnValueMap = columnValueMap;
    }

    public void addColumnValue(String key, Object object) {
        columnValueMap.put(key, object);
    }

    public void addColumnValue(Map<String, Object> columnValueMap) {
        this.columnValueMap.putAll(columnValueMap);
    }

    @Override
    public PreparedStatement build() throws SQLException, PreparedStatementMissingArgumentException {
        // Example: INSERT INTO Table (var_1, var_2) VALUES (NULL, ?);

        // Ensure required arguments exist
        if (columnValueMap.size() == 0) throw new PreparedStatementMissingArgumentException("Missing argument in InsertIntoPreparedStatement.");

        // Setup Prepared Statement
        List<Object> orderedValues = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(INSERT_INTO + SPACE + table + SPACE + getColumnValuePlaceholderString(orderedValues) + TERMINATOR);

        for (int i = 0; i < orderedValues.size(); i++) ps.setObject(i + 1, orderedValues.get(i));

        return ps;
    }

    /***
     * Returns the full "(?, ?,...?) VALUES (?, ?,...?)" string for PreparedStatement
     *
     * @param orderedValues
     * @return full "Column VALUE Object" string for PreparedStatement
     */
    private String getColumnValuePlaceholderString(List<Object> orderedValues) {
        if (columnValueMap.size() == 0) return "";

        // Build column value placeholders
        StringBuilder keyString = new StringBuilder();
        StringBuilder placeholderString = new StringBuilder();
        columnValueMap.forEach((k, v) -> {
            keyString.append(k + SEPARATOR);
            placeholderString.append(PLACEHOLDER + SEPARATOR);
            orderedValues.add(v);
        });

        keyString.delete(keyString.length() - 2, keyString.length());
        placeholderString.delete(placeholderString.length() - 2, placeholderString.length());

        return EXPRESSION_OPEN + keyString + EXPRESSION_CLOSE + SPACE + VALUES + SPACE + EXPRESSION_OPEN + placeholderString + EXPRESSION_CLOSE;
    }
}
