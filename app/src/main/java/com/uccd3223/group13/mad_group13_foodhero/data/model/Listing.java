package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Listing implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("merchant_id")
    private String merchantId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("original_price")
    private double originalPrice;

    @SerializedName("discounted_price")
    private double discountedPrice;

    @SerializedName("remaining_quantity")
    private int remainingQuantity;

    @SerializedName("total_quantity")
    private int totalQuantity;

    @SerializedName("image_source")
    private ImageSource imageSource;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("storage_path")
    private String storagePath;

    @SerializedName("pickup_start")
    private String pickupStart;

    @SerializedName("pickup_end")
    private String pickupEnd;

    @SerializedName("pickup_location")
    private String pickupLocation;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("co2_kg_per_item")
    private double co2KgPerItem;

    @SerializedName("status")
    private ListingStatus status;

    // Associated merchant metadata (optional / populated on fetch)
    @SerializedName("merchants")
    private Merchant merchant;

    // Transient UI / local states
    private transient boolean isFavourite;
    private transient double distanceKm;
    private transient int travelMins;

    public Listing() {
        this.status = ListingStatus.ACTIVE;
        this.imageSource = ImageSource.NONE;
        this.co2KgPerItem = 1.20;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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

    public String getCategory() {
        return category != null ? category : "Meals";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public ImageSource getImageSource() {
        return imageSource != null ? imageSource : ImageSource.NONE;
    }

    public void setImageSource(ImageSource imageSource) {
        this.imageSource = imageSource;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
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

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
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

    public double getCo2KgPerItem() {
        return co2KgPerItem > 0 ? co2KgPerItem : 1.20;
    }

    public void setCo2KgPerItem(double co2KgPerItem) {
        this.co2KgPerItem = co2KgPerItem;
    }

    public ListingStatus getStatus() {
        return status != null ? status : ListingStatus.ACTIVE;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public int getTravelMins() {
        return travelMins;
    }

    public void setTravelMins(int travelMins) {
        this.travelMins = travelMins;
    }

    public double getSavingsAmount() {
        return Math.max(0.0, originalPrice - discountedPrice);
    }
}
