package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_listings")
public class ListingEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String merchantId;
    public String merchantName;
    public String title;
    public String description;
    public String category;
    public double originalPrice;
    public double discountedPrice;
    public int remainingQuantity;
    public int totalQuantity;
    public String imageSource;
    public String imageUrl;
    public String pickupStart;
    public String pickupEnd;
    public String pickupLocation;
    public double latitude;
    public double longitude;
    public double co2KgPerItem;
    public String status;
    public long cachedAt;

    public ListingEntity() {
        this.id = "";
        this.cachedAt = System.currentTimeMillis();
    }
}
