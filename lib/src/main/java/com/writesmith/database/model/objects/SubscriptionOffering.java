package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.SubscriptionOffering.TABLE_NAME)
public class SubscriptionOffering {

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.offering_id)
    private String offeringId;

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.name)
    private String name;

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.is_active)
    private Boolean isActive;

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.created_at)
    private LocalDateTime createdAt;

    @DBColumn(name = DBRegistry.Table.SubscriptionOffering.updated_at)
    private LocalDateTime updatedAt;

    public SubscriptionOffering() {
    }

    public Integer getId() {
        return id;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getName() {
        return name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
