package com.writesmith.database.objects;

public enum Table {
    CHAT("Chat"),
    RECEIPT("Receipt"),
    USER_AUTHTOKEN("User_AuthToken"),
    USER_LOGIN("User_Login");

    public final String string;

    Table(String string) { this.string = string; }
}
