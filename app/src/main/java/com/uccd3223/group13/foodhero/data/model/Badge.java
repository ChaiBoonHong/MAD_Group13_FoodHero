package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Badge implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("threshold")
    private int threshold; // 1, 5, 10, 25 meals

    @SerializedName("tier")
    private String tier; // "bronze", "silver", "gold", "emerald"

    @SerializedName("is_unlocked")
    private boolean isUnlocked;

    public Badge() {
    }

    public Badge(String id, String title, String description, int threshold, String tier, boolean isUnlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.threshold = threshold;
        this.tier = tier;
        this.isUnlocked = isUnlocked;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
}
