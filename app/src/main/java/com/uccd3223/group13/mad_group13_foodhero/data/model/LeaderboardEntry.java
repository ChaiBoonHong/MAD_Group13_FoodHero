package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class LeaderboardEntry implements Serializable {
    @SerializedName("faculty")
    private String faculty;

    @SerializedName("total_rescued")
    private int totalRescued;

    @SerializedName("rank")
    private int rank;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(String faculty, int totalRescued, int rank) {
        this.faculty = faculty;
        this.totalRescued = totalRescued;
        this.rank = rank;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public int getTotalRescued() {
        return totalRescued;
    }

    public void setTotalRescued(int totalRescued) {
        this.totalRescued = totalRescued;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
