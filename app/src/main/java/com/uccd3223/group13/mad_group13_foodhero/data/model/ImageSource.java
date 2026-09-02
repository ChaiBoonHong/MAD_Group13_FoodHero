package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum ImageSource {
    @SerializedName("storage")
    STORAGE("storage"),

    @SerializedName("external_url")
    EXTERNAL_URL("external_url"),

    @SerializedName("none")
    NONE("none");

    private final String value;

    ImageSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ImageSource fromString(String text) {
        if (text != null) {
            for (ImageSource source : ImageSource.values()) {
                if (source.value.equalsIgnoreCase(text)) {
                    return source;
                }
            }
        }
        return NONE;
    }
}
