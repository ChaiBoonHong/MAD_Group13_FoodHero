package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum OrderStatus {
    @SerializedName("reserved")
    RESERVED("reserved"),

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
