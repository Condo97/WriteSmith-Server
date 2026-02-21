package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = DBRegistry.Table.SubscriptionOfferingGroupCopy.TABLE_NAME)
public class SubscriptionOfferingGroupCopy {

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.offering_group_id)
    private Integer offeringGroupId;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.header_title)
    private String headerTitle;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.header_subtitle)
    private String headerSubtitle;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.cta_button_text)
    private String ctaButtonText;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupCopy.supporting_text)
    private String supportingText;

    public SubscriptionOfferingGroupCopy() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getOfferingGroupId() {
        return offeringGroupId;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public String getHeaderSubtitle() {
        return headerSubtitle;
    }

    public String getCtaButtonText() {
        return ctaButtonText;
    }

    public String getSupportingText() {
        return supportingText;
    }

}
