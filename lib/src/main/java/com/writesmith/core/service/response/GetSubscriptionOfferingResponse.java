package com.writesmith.core.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GetSubscriptionOfferingResponse {

    private String offeringId;
    private String testGroupId;
    private List<SubscriptionProduct> subscriptions;
    private SubscriptionCopy copy;

    public GetSubscriptionOfferingResponse() {
    }

    public GetSubscriptionOfferingResponse(String offeringId, String testGroupId, List<SubscriptionProduct> subscriptions, SubscriptionCopy copy) {
        this.offeringId = offeringId;
        this.testGroupId = testGroupId;
        this.subscriptions = subscriptions;
        this.copy = copy;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getTestGroupId() {
        return testGroupId;
    }

    public List<SubscriptionProduct> getSubscriptions() {
        return subscriptions;
    }

    public SubscriptionCopy getCopy() {
        return copy;
    }

    public static class SubscriptionProduct {

        private String productId;
        private String type;
        private String fallbackDisplayPrice;
        private int position;

        @JsonProperty("isDefault")
        private boolean isDefault;

        private String badgeText;
        private String subtitleText;

        public SubscriptionProduct() {
        }

        public SubscriptionProduct(String productId, String type, String fallbackDisplayPrice, int position, boolean isDefault, String badgeText, String subtitleText) {
            this.productId = productId;
            this.type = type;
            this.fallbackDisplayPrice = fallbackDisplayPrice;
            this.position = position;
            this.isDefault = isDefault;
            this.badgeText = badgeText;
            this.subtitleText = subtitleText;
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

        public int getPosition() {
            return position;
        }

        public boolean getIsDefault() {
            return isDefault;
        }

        public String getBadgeText() {
            return badgeText;
        }

        public String getSubtitleText() {
            return subtitleText;
        }
    }

    public static class SubscriptionCopy {

        private String headerTitle;
        private String headerSubtitle;
        private String ctaButtonText;
        private String supportingText;

        public SubscriptionCopy() {
        }

        public SubscriptionCopy(String headerTitle, String headerSubtitle, String ctaButtonText, String supportingText) {
            this.headerTitle = headerTitle;
            this.headerSubtitle = headerSubtitle;
            this.ctaButtonText = ctaButtonText;
            this.supportingText = supportingText;
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

}
