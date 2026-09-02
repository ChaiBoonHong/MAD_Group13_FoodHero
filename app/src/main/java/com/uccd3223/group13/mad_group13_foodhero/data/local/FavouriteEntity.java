package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favourites")
public class FavouriteEntity {
    @PrimaryKey
    @NonNull
    public String listingId;
    public long savedAt;

    public FavouriteEntity(@NonNull String listingId) {
        this.listingId = listingId;
        this.savedAt = System.currentTimeMillis();
    }
}
