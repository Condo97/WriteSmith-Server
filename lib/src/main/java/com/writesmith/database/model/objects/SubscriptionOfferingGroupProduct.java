package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = DBRegistry.Table.SubscriptionOfferingGroupProduct.TABLE_NAME)
public class SubscriptionOfferingGroupProduct {

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.offering_group_id)
    private Integer offeringGroupId;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.product_id)
    private String productId;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.type)
    private String type;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.fallback_display_price)
    private String fallbackDisplayPrice;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.position)
    private Integer position;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.is_default)
    private Boolean isDefault;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.badge_text)
    private String badgeText;

    @DBColumn(name = DBRegistry.Table.SubscriptionOfferingGroupProduct.subtitle_text)
    private String subtitleText;

    public SubscriptionOfferingGroupProduct() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getOfferingGroupId() {
        return offeringGroupId;
    }

    public String getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public String getFallbackDisplayPrice() {
        return fallbackDisplayPrice;
    }

    public Integer getPosition() {
        return position;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public String getSubtitleText() {
        return subtitleText;
    }

}
