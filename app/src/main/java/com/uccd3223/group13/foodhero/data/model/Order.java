package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Order implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("order_code")
    private String orderCode;

    @SerializedName("student_id")
    private String studentId;

    @SerializedName("listing_id")
    private String listingId;

    @SerializedName("merchant_id")
    private String merchantId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("total_original_price")
    private double totalOriginalPrice;

    @SerializedName("total_discounted_price")
    private double totalDiscountedPrice;

    @SerializedName("reward_points_used")
    private int rewardPointsUsed;

    @SerializedName("reward_discount_amount")
    private double rewardDiscountAmount;

    @SerializedName("final_paid_price")
    private double finalPaidPrice;

    @SerializedName("pickup_start")
    private String pickupStart;

    @SerializedName("pickup_end")
    private String pickupEnd;

    @SerializedName("pickup_token")
    private String pickupToken;

    @SerializedName("status")
    private OrderStatus status;

    @SerializedName("completed_at")
    private String completedAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("payment_expires_at")
    private long paymentExpiresAt;

    @SerializedName("payment_receipt_url")
    private String paymentReceiptUrl;

    @SerializedName("payment_method")
    private String paymentMethod = "DUITNOW_QR";

    @SerializedName("payment_reference")
    private String paymentReference;

    // Nested relations
    @SerializedName("listings")
    private Listing listing;

    @SerializedName("merchants")
    private Merchant merchant;

    @SerializedName("profiles")
    private Profile studentProfile;

    public Order() {
        this.status = OrderStatus.RESERVED;
        this.quantity = 1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalOriginalPrice() {
        return totalOriginalPrice;
    }

    public void setTotalOriginalPrice(double totalOriginalPrice) {
        this.totalOriginalPrice = totalOriginalPrice;
    }

    public double getTotalDiscountedPrice() {
        return totalDiscountedPrice;
    }

    public void setTotalDiscountedPrice(double totalDiscountedPrice) {
        this.totalDiscountedPrice = totalDiscountedPrice;
    }

    public int getRewardPointsUsed() {
        return rewardPointsUsed;
    }

    public void setRewardPointsUsed(int rewardPointsUsed) {
        this.rewardPointsUsed = rewardPointsUsed;
    }

    public double getRewardDiscountAmount() {
        return rewardDiscountAmount;
    }

    public void setRewardDiscountAmount(double rewardDiscountAmount) {
        this.rewardDiscountAmount = rewardDiscountAmount;
    }

    public double getFinalPaidPrice() {
        return finalPaidPrice;
    }

    public void setFinalPaidPrice(double finalPaidPrice) {
        this.finalPaidPrice = finalPaidPrice;
    }

    public String getPickupStart() {
        return pickupStart;
    }

    public void setPickupStart(String pickupStart) {
        this.pickupStart = pickupStart;
    }

    public String getPickupEnd() {
        return pickupEnd;
    }

    public void setPickupEnd(String pickupEnd) {
        this.pickupEnd = pickupEnd;
    }

    public String getPickupToken() {
        return pickupToken;
    }

    public void setPickupToken(String pickupToken) {
        this.pickupToken = pickupToken;
    }

    public OrderStatus getStatus() {
        return status != null ? status : OrderStatus.RESERVED;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public Profile getStudentProfile() {
        return studentProfile;
    }

    public void setStudentProfile(Profile studentProfile) {
        this.studentProfile = studentProfile;
    }

    public long getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public void setPaymentExpiresAt(long paymentExpiresAt) {
        this.paymentExpiresAt = paymentExpiresAt;
    }

    public String getPaymentReceiptUrl() {
        return paymentReceiptUrl;
    }

    public void setPaymentReceiptUrl(String paymentReceiptUrl) {
        this.paymentReceiptUrl = paymentReceiptUrl;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Profile getStudent() {
        return studentProfile;
    }

    public void setStudent(Profile studentProfile) {
        this.studentProfile = studentProfile;
    }
}
