package com.uccd3223.group13.mad_group13_foodhero.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.local.AppDatabase;
import com.uccd3223.group13.mad_group13_foodhero.data.local.DashboardCacheEntity;
import com.uccd3223.group13.mad_group13_foodhero.data.local.FavouriteEntity;
import com.uccd3223.group13.mad_group13_foodhero.data.local.ListingEntity;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ImageSource;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ListingStatus;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Merchant;
import com.uccd3223.group13.mad_group13_foodhero.data.model.MerchantDashboardData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalCacheRepository {
    private static volatile LocalCacheRepository INSTANCE;
    private final AppDatabase database;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    private LocalCacheRepository(Context context) {
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    public static LocalCacheRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocalCacheRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocalCacheRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public void getCachedFeed(ResultCallback<List<Listing>> callback) {
        executor.execute(() -> {
            try {
                List<ListingEntity> entities = database.listingDao().getAllCachedListings();
                List<String> favIds = database.favouriteDao().getAllFavouriteIds();

                List<Listing> listings = new ArrayList<>();
                for (ListingEntity e : entities) {
                    Listing l = new Listing();
                    l.setId(e.id);
                    l.setMerchantId(e.merchantId);
                    l.setTitle(e.title);
                    l.setDescription(e.description);
                    l.setCategory(e.category);
                    l.setOriginalPrice(e.originalPrice);
                    l.setDiscountedPrice(e.discountedPrice);
                    l.setRemainingQuantity(e.remainingQuantity);
                    l.setTotalQuantity(e.totalQuantity);
                    l.setImageSource(ImageSource.fromString(e.imageSource));
                    l.setImageUrl(e.imageUrl);
                    l.setPickupStart(e.pickupStart);
                    l.setPickupEnd(e.pickupEnd);
                    l.setPickupLocation(e.pickupLocation);
                    l.setLatitude(e.latitude);
                    l.setLongitude(e.longitude);
                    l.setCo2KgPerItem(e.co2KgPerItem);
                    l.setStatus(ListingStatus.fromString(e.status));
                    l.setFavourite(favIds.contains(e.id));

                    if (e.merchantName != null) {
                        Merchant m = new Merchant();
                        m.setId(e.merchantId);
                        m.setBusinessName(e.merchantName);
                        l.setMerchant(m);
                    }
                    listings.add(l);
                }
                postSuccess(callback, listings);
            } catch (Exception err) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to read cached feed", err));
            }
        });
    }

    public void cacheFeed(List<Listing> listings) {
        if (listings == null || listings.isEmpty()) return;
        executor.execute(() -> {
            try {
                List<ListingEntity> entities = new ArrayList<>();
                for (Listing l : listings) {
                    ListingEntity e = new ListingEntity();
                    e.id = l.getId();
                    e.merchantId = l.getMerchantId();
                    e.merchantName = l.getMerchant() != null ? l.getMerchant().getBusinessName() : "";
                    e.title = l.getTitle();
                    e.description = l.getDescription();
                    e.category = l.getCategory();
                    e.originalPrice = l.getOriginalPrice();
                    e.discountedPrice = l.getDiscountedPrice();
                    e.remainingQuantity = l.getRemainingQuantity();
                    e.totalQuantity = l.getTotalQuantity();
                    e.imageSource = l.getImageSource().getValue();
                    e.imageUrl = l.getImageUrl();
                    e.pickupStart = l.getPickupStart();
                    e.pickupEnd = l.getPickupEnd();
                    e.pickupLocation = l.getPickupLocation();
                    e.latitude = l.getLatitude();
                    e.longitude = l.getLongitude();
                    e.co2KgPerItem = l.getCo2KgPerItem();
                    e.status = l.getStatus().getValue();
                    entities.add(e);
                }
                database.listingDao().insertAll(entities);
            } catch (Exception ignored) {
            }
        });
    }

    public void toggleFavourite(String listingId, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                boolean isFav = database.favouriteDao().isFavourite(listingId);
                if (isFav) {
                    database.favouriteDao().removeFavourite(listingId);
                    postSuccess(callback, false);
                } else {
                    database.favouriteDao().addFavourite(new FavouriteEntity(listingId));
                    postSuccess(callback, true);
                }
            } catch (Exception err) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to toggle favourite", err));
            }
        });
    }

    public void getFavourites(ResultCallback<List<String>> callback) {
        executor.execute(() -> {
            try {
                List<String> favs = database.favouriteDao().getAllFavouriteIds();
                postSuccess(callback, favs);
            } catch (Exception err) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to get favourites", err));
            }
        });
    }

    public void cacheDashboard(String merchantId, MerchantDashboardData data) {
        if (merchantId == null || data == null) return;
        executor.execute(() -> {
            try {
                String json = gson.toJson(data);
                database.dashboardCacheDao().insertDashboard(new DashboardCacheEntity(merchantId, json));
            } catch (Exception ignored) {
            }
        });
    }

    public void getCachedDashboard(String merchantId, ResultCallback<MerchantDashboardData> callback) {
        executor.execute(() -> {
            try {
                DashboardCacheEntity entity = database.dashboardCacheDao().getDashboard(merchantId);
                if (entity != null && entity.jsonData != null) {
                    MerchantDashboardData data = gson.fromJson(entity.jsonData, MerchantDashboardData.class);
                    postSuccess(callback, data);
                } else {
                    postError(callback, new DataError(DataError.CODE_NOT_FOUND, "No cached dashboard data"));
                }
            } catch (Exception err) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to load cached dashboard", err));
            }
        });
    }

    private <T> void postSuccess(ResultCallback<T> callback, T result) {
        mainHandler.post(() -> {
            if (callback != null) callback.onSuccess(result);
        });
    }

    private <T> void postError(ResultCallback<T> callback, DataError error) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(error);
        });
    }
}
