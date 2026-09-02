package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum NotificationType {
    @SerializedName("nearby_listing")
    NEARBY_LISTING("nearby_listing"),

    @SerializedName("reservation_created")
    RESERVATION_CREATED("reservation_created"),

    @SerializedName("pickup_reminder")
    PICKUP_REMINDER("pickup_reminder"),

    @SerializedName("order_completed")
    ORDER_COMPLETED("order_completed"),

    @SerializedName("order_cancelled")
    ORDER_CANCELLED("order_cancelled"),

    @SerializedName("review_received")
    REVIEW_RECEIVED("review_received"),

    @SerializedName("listing_sold_out")
    LISTING_SOLD_OUT("listing_sold_out"),

    @SerializedName("low_stock")
    LOW_STOCK("low_stock");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromString(String text) {
        if (text != null) {
            for (NotificationType type : NotificationType.values()) {
                if (type.value.equalsIgnoreCase(text)) {
                    return type;
                }
            }
        }
        return NEARBY_LISTING;
    }
}
