package com.uccd3223.group13.foodhero.data.remote;

import com.uccd3223.group13.foodhero.BuildConfig;

public class SupabaseConfig {
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL != null ? BuildConfig.SUPABASE_URL : "https://your-project-id.supabase.co";
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY != null ? BuildConfig.SUPABASE_ANON_KEY : "dummy-supabase-anon-key";
    public static final String STORAGE_BUCKET_LISTING_IMAGES = "listing-images";

    public static String getStoragePublicUrl(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return null;
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath;
        }
        return SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET_LISTING_IMAGES + "/" + storagePath;
    }
}
