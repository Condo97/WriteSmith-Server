package com.writesmith.model.database;

import sqlcomponentizer.dbserializer.DBEnumGetter;
import sqlcomponentizer.dbserializer.DBEnumSetter;

public enum Sender {
    USER("user"),
    AI("ai");

    private String string;

    Sender(String string) {
        this.string = string;
    }

    @DBEnumSetter
    public static Sender fromString(String string) {
        Sender[] values = Sender.values();
        for (Sender value: values) {
            if (value.string.equals(string))
                return value;
        }

        throw new IllegalArgumentException("Could not find Role from String");
    }

    @DBEnumGetter
    @Override
    public String toString() {
        return string;
    }
}