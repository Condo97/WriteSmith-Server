package com.writesmith.common.exceptions;

public class SQLColumnNotFoundException extends Exception {
    String columnName;

    public SQLColumnNotFoundException(String message, String columnName) {
        super(message);

        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
