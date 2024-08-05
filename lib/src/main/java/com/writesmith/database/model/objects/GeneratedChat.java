package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;
import sqlcomponentizer.dbserializer.DBSubObject;

@DBSerializable(tableName = DBRegistry.Table.GeneratedChat.TABLE_NAME)
public class GeneratedChat {

    @DBColumn(name = DBRegistry.Table.GeneratedChat.chat_id, primaryKey = true)
    private Integer chat_id;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.finish_reason)
    private String finish_reason;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.model_name)
    private String modelName;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.completion_tokens)
    private Integer completionTokens;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.prompt_tokens)
    private Integer promptTokens;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.total_tokens)
    private Integer totalTokens;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.removed_images)
    private Boolean removedImages;

    @DBSubObject()
    private ChatLegacy chatLegacy;


    public GeneratedChat() {

    }

    public GeneratedChat(ChatLegacy chatLegacy, String finish_reason, String modelName, Integer completionTokens, Integer promptTokens, Integer totalTokens, Boolean removedImages) {
        this.chatLegacy = chatLegacy;
        chat_id = chatLegacy.getChat_id();
        this.finish_reason = finish_reason;
        this.modelName = modelName;
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.totalTokens = totalTokens;
        this.removedImages = removedImages;
    }

    public ChatLegacy getChat() {
        return chatLegacy;
    }

    public void setChat(ChatLegacy chatLegacy) {
        this.chatLegacy = chatLegacy;
        chat_id = chatLegacy.getChat_id();
    }

    public Integer getChat_id() {
        return chat_id;
    }

    public void setChat_id(Integer chat_id) {
        this.chat_id = chat_id;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public String getModelName() {
        return modelName;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Boolean getRemovedImages() {
        return removedImages;
    }

    public void setRemovedImages(Boolean removedImages) {
        this.removedImages = removedImages;
    }

}
