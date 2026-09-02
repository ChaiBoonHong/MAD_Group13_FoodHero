package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum OrderStatus {
    @SerializedName("awaiting_payment")
    AWAITING_PAYMENT("awaiting_payment"),

    @SerializedName("pending_verification")
    PENDING_VERIFICATION("pending_verification"),

    @SerializedName("reserved")
    RESERVED("reserved"),

    @SerializedName("rejected")
    REJECTED("rejected"),

    @SerializedName("completed")
    COMPLETED("completed"),

    @SerializedName("cancelled")
    CANCELLED("cancelled"),

    @SerializedName("expired")
    EXPIRED("expired");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OrderStatus fromString(String text) {
        if (text != null) {
            for (OrderStatus status : OrderStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
        }
        return RESERVED;
    }
}
