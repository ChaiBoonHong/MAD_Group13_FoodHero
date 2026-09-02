package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImpactSummary implements Serializable {
    @SerializedName("meals_rescued")
    private int mealsRescued;

    @SerializedName("money_saved")
    private double moneySaved;

    @SerializedName("co2_prevented")
    private double co2Prevented;

    @SerializedName("eco_points")
    private int ecoPoints;

    @SerializedName("tree_progress_percent")
    private int treeProgressPercent;

    @SerializedName("badges")
    private List<Badge> badges;

    @SerializedName("leaderboard")
    private List<LeaderboardEntry> leaderboard;

    public ImpactSummary() {
        this.badges = new ArrayList<>();
        this.leaderboard = new ArrayList<>();
    }

    public int getMealsRescued() {
        return mealsRescued;
    }

    public void setMealsRescued(int mealsRescued) {
        this.mealsRescued = mealsRescued;
    }

    public double getMoneySaved() {
        return moneySaved;
    }

    public void setMoneySaved(double moneySaved) {
        this.moneySaved = moneySaved;
    }

    public double getCo2Prevented() {
        return co2Prevented;
    }

    public void setCo2Prevented(double co2Prevented) {
        this.co2Prevented = co2Prevented;
    }

    public int getEcoPoints() {
        return ecoPoints;
    }

    public void setEcoPoints(int ecoPoints) {
        this.ecoPoints = ecoPoints;
    }

    public int getTreeProgressPercent() {
        // Tree progress targets 25 meals rescued for full mature tree
        return Math.min(100, (int) Math.round((mealsRescued / 25.0) * 100.0));
    }

    public void setTreeProgressPercent(int treeProgressPercent) {
        this.treeProgressPercent = treeProgressPercent;
    }

    public List<Badge> getBadges() {
        return badges;
    }

    public void setBadges(List<Badge> badges) {
        this.badges = badges;
    }

    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboard;
    }

    public void setLeaderboard(List<LeaderboardEntry> leaderboard) {
        this.leaderboard = leaderboard;
    }
}
