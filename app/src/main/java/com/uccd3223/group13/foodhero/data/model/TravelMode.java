package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum TravelMode {
    @SerializedName("walking")
    WALKING("walking", 4.5), // km/h

    @SerializedName("cycling")
    CYCLING("cycling", 15.0),

    @SerializedName("shuttle")
    SHUTTLE("shuttle", 25.0);

    private final String value;
    private final double avgSpeedKmh;

    TravelMode(String value, double avgSpeedKmh) {
        this.value = value;
        this.avgSpeedKmh = avgSpeedKmh;
    }

    public String getValue() {
        return value;
    }

    public double getAvgSpeedKmh() {
        return avgSpeedKmh;
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
