package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum OrderStatus {
    @SerializedName("awaiting_payment")
    AWAITING_PAYMENT("awaiting_payment"),

    @SerializedName("pending_verification")
    PENDING_VERIFICATION("pending_verification"),

    @SerializedName("ready_for_pickup")
    READY_FOR_PICKUP("ready_for_pickup"),

    @SerializedName("payment_rejected")
    PAYMENT_REJECTED("payment_rejected"),

    @SerializedName("completed")
    COMPLETED("completed"),

    @SerializedName("cancelled")
    CANCELLED("cancelled"),

    @SerializedName("expired")
    EXPIRED("expired"),

    @SerializedName("no_show")
    NO_SHOW("no_show");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OrderStatus fromString(String text) {
        if ("reserved".equalsIgnoreCase(text)) return READY_FOR_PICKUP;
        if ("rejected".equalsIgnoreCase(text)) return PAYMENT_REJECTED;
        if (text != null) {
            for (OrderStatus status : OrderStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
        }
        return null;
    }
}
