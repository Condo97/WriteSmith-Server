package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.UserOfferingAssignment.TABLE_NAME)
public class UserOfferingAssignment {

    @DBColumn(name = DBRegistry.Table.UserOfferingAssignment.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.UserOfferingAssignment.user_id)
    private Integer userId;

    @DBColumn(name = DBRegistry.Table.UserOfferingAssignment.subscription_offering_id)
    private Integer subscriptionOfferingId;

    @DBColumn(name = DBRegistry.Table.UserOfferingAssignment.offering_group_id)
    private Integer offeringGroupId;

    @DBColumn(name = DBRegistry.Table.UserOfferingAssignment.assigned_at)
    private LocalDateTime assignedAt;

    public UserOfferingAssignment() {
    }

    public UserOfferingAssignment(Integer userId, Integer subscriptionOfferingId, Integer offeringGroupId) {
        this.userId = userId;
        this.subscriptionOfferingId = subscriptionOfferingId;
        this.offeringGroupId = offeringGroupId;
        this.assignedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getSubscriptionOfferingId() {
        return subscriptionOfferingId;
    }

    public Integer getOfferingGroupId() {
        return offeringGroupId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

}
