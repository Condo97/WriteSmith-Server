package com.writesmith.model.http.client.openaigpt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.writesmith.model.database.DBRegistry;

public enum Role {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private String string;

    Role(String string) {
        this.string = string;
    }

    @JsonCreator
    public static Role fromString(@JsonProperty("role") String string) {
        Role[] values = Role.values();
        for (Role value: values) {
            if (value.string.equals(string))
                return value;
        }

        throw new IllegalArgumentException("Could not find Role from String");
    }

    @JsonValue
    public String stringValue() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

}
