package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public class UserRoleRecord {
    @SerializedName("user_id") private String userId;
    @SerializedName("role") private UserRole role;
    public String getUserId() { return userId; }
    public UserRole getRole() { return role; }
}
