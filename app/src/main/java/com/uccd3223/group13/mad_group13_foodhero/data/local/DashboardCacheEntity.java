package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_dashboard")
public class DashboardCacheEntity {
    @PrimaryKey
    @NonNull
    public String merchantId;
    public String jsonData;
    public long cachedAt;

    public DashboardCacheEntity(@NonNull String merchantId, String jsonData) {
        this.merchantId = merchantId;
        this.jsonData = jsonData;
        this.cachedAt = System.currentTimeMillis();
    }
}
