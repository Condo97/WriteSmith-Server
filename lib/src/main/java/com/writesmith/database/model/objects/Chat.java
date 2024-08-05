package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.Chat.TABLE_NAME)
public class Chat {

    @DBColumn(name = DBRegistry.Table.Chat.chat_id, primaryKey = true)
    private Integer chat_id;

    @DBColumn(name = DBRegistry.Table.Chat.user_id)
    private Integer user_id;

    @DBColumn(name = DBRegistry.Table.Chat.completion_tokens)
    private Integer completionTokens;

    @DBColumn(name = DBRegistry.Table.Chat.prompt_tokens)
    private Integer promptTokens;

    @DBColumn(name = DBRegistry.Table.Chat.date)
    private LocalDateTime date;


    public Chat() {

    }

    public Chat(Integer chat_id, Integer user_id, Integer completionTokens, Integer promptTokens, LocalDateTime date) {
        this.chat_id = chat_id;
        this.user_id = user_id;
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.date = date;
    }

    public Integer getChat_id() {
        return chat_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public LocalDateTime getDate() {
        return date;
    }

}