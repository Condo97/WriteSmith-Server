package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.SubscriptionOfferingGroup.TABLE_NAME)
public class SubscriptionOfferingGroup {

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroup.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroup.subscription_offering_id)
    private Integer subscriptionOfferingId;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroup.group_name)
    private String groupName;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroup.weight)
    private Double weight;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroup.created_at)
    private LocalDateTime createdAt;

    public SubscriptionOfferingGroup() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getSubscriptionOfferingId() {
        return subscriptionOfferingId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Double getWeight() {
        return weight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
