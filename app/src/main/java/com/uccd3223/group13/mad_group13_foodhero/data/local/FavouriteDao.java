package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavouriteDao {
    @Query("SELECT listingId FROM favourites ORDER BY savedAt DESC")
    List<String> getAllFavouriteIds();

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE listingId = :listingId)")
    boolean isFavourite(String listingId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFavourite(FavouriteEntity favourite);

    @Query("DELETE FROM favourites WHERE listingId = :listingId")
    void removeFavourite(String listingId);
}
