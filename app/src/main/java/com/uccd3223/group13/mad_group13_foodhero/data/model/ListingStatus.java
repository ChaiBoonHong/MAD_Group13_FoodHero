package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum ListingStatus {
    @SerializedName("active")
    ACTIVE("active"),

    @SerializedName("sold_out")
    SOLD_OUT("sold_out"),

    @SerializedName("expired")
    EXPIRED("expired"),

    @SerializedName("draft")
    DRAFT("draft");

    private final String value;

    ListingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ListingStatus fromString(String text) {
        if (text != null) {
            for (ListingStatus status : ListingStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
        }
        return ACTIVE;
    }
}
