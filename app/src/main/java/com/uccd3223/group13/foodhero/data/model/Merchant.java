package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Merchant implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("owner_id")
    private String ownerId;

    @SerializedName("business_name")
    private String businessName;

    @SerializedName("campus_location")
    private String campusLocation;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("closing_time")
    private String closingTime;

    @SerializedName("rating")
    private double rating;

    @SerializedName("total_reviews")
    private int totalReviews;

    @SerializedName("status")
    private String status;

    @SerializedName("rejection_reason")
    private String rejectionReason;

    @SerializedName("campus_id")
    private String campusId;

    @SerializedName("duitnow_display_name")
    private String duitNowDisplayName;

    @SerializedName("duitnow_qr_path")
    private String duitNowQrPath;

    public Merchant() {
    }

    public Merchant(String id, String ownerId, String businessName, String campusLocation, double latitude, double longitude) {
        this.id = id;
        this.ownerId = ownerId;
        this.businessName = businessName;
        this.campusLocation = campusLocation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.closingTime = "18:00";
        this.rating = 0.00;
        this.totalReviews = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCampusLocation() {
        return campusLocation;
    }

    public void setCampusLocation(String campusLocation) {
        this.campusLocation = campusLocation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(String closingTime) {
        this.closingTime = closingTime;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public String getStatus() { return status != null ? status : "pending"; }
    public String getRejectionReason() { return rejectionReason; }
    public boolean isApproved() { return "approved".equalsIgnoreCase(getStatus()); }
    public String getCampusId() { return campusId; }
    public String getDuitNowDisplayName() { return duitNowDisplayName; }
    public String getDuitNowQrPath() { return duitNowQrPath; }
}
