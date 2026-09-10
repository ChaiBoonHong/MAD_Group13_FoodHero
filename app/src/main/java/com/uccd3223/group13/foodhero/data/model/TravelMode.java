package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum TravelMode {
    @SerializedName("walking")
    WALKING("walking"),

    @SerializedName("cycling")
    CYCLING("cycling"),

    @SerializedName("shuttle")
    SHUTTLE("shuttle");

    private final String value;

    TravelMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TravelMode fromString(String text) {
        if (text != null) {
            for (TravelMode mode : TravelMode.values()) {
                if (mode.value.equalsIgnoreCase(text)) {
                    return mode;
                }
            }
        }
        return WALKING;
    }
}
