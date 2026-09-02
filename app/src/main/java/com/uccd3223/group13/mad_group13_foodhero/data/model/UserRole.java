package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public enum UserRole {
    @SerializedName("student")
    STUDENT("student"),

    @SerializedName("merchant")
    MERCHANT("merchant");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromString(String text) {
        if (text != null) {
            for (UserRole role : UserRole.values()) {
                if (role.value.equalsIgnoreCase(text)) {
                    return role;
                }
            }
        }
        return STUDENT;
    }
}
