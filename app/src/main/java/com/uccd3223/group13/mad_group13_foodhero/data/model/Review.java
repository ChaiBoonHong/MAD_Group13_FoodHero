package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Review implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("listing_id")
    private String listingId;

    @SerializedName("student_id")
    private String studentId;

    @SerializedName("merchant_id")
    private String merchantId;

    @SerializedName("rating")
    private int rating;

    @SerializedName("comment")
    private String comment;

    @SerializedName("created_at")
    private String createdAt;

    // Joined fields
    @SerializedName("profiles")
    private Profile reviewer;

    @SerializedName("listings")
    private Listing listing;

    public Review() {
        this.rating = 5;
    }

    public Review(String orderId, String listingId, String studentId, String merchantId, int rating, String comment) {
        this.orderId = orderId;
        this.listingId = listingId;
        this.studentId = studentId;
        this.merchantId = merchantId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Profile getReviewer() {
        return reviewer;
    }

    public void setReviewer(Profile reviewer) {
        this.reviewer = reviewer;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }
}
