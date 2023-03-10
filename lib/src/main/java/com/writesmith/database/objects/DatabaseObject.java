package com.writesmith.database.objects;

public class DatabaseObject {
    private Table type;

    public DatabaseObject(Table type) {
        this.type = type;
    }

    public Table getType() {
        return type;
    }
}
