package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class FoodHeroNotification implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("recipient_id")
    private String recipientId;

    @SerializedName("recipient_role")
    private UserRole recipientRole;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("event_type")
    private NotificationType eventType;

    @SerializedName("related_listing_id")
    private String relatedListingId;

    @SerializedName("related_order_id")
    private String relatedOrderId;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    public FoodHeroNotification() {
        this.isRead = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public UserRole getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(UserRole recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationType eventType) {
        this.eventType = eventType;
    }

    public String getRelatedListingId() {
        return relatedListingId;
    }

    public void setRelatedListingId(String relatedListingId) {
        this.relatedListingId = relatedListingId;
    }

    public String getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(String relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
