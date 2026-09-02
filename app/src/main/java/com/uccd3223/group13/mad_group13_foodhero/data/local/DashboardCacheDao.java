package com.uccd3223.group13.mad_group13_foodhero.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DashboardCacheDao {
    @Query("SELECT * FROM cached_dashboard WHERE merchantId = :merchantId LIMIT 1")
    DashboardCacheEntity getDashboard(String merchantId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDashboard(DashboardCacheEntity entity);

    @Query("DELETE FROM cached_dashboard WHERE merchantId = :merchantId")
    void clearDashboard(String merchantId);
}
