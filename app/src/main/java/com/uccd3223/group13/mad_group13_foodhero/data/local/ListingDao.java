package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ListingDao {
    @Query("SELECT * FROM cached_listings ORDER BY cachedAt DESC")
    List<ListingEntity> getAllCachedListings();

    @Query("SELECT * FROM cached_listings WHERE id = :id LIMIT 1")
    ListingEntity getListingById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ListingEntity> listings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ListingEntity listing);

    @Query("DELETE FROM cached_listings")
    void clearAll();
}
