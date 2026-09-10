package com.uccd3223.group13.foodhero.data.remote;

import com.uccd3223.group13.foodhero.BuildConfig;

public class SupabaseConfig {
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public static final String STORAGE_BUCKET_LISTING_IMAGES = "listing-images";
    public static final String STORAGE_BUCKET_PAYMENT_RECEIPTS = "payment-receipts";
    public static final String STORAGE_BUCKET_MERCHANT_QRS = "merchant-payment-qrs";

    public static String getStoragePublicUrl(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return null;
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath;
        }
        return SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET_LISTING_IMAGES + "/" + storagePath;
    }

    public static String getPaymentReceiptUrl(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return null;
        return SUPABASE_URL + "/storage/v1/object/authenticated/" + STORAGE_BUCKET_PAYMENT_RECEIPTS + "/" + storagePath;
    }

    public static String getMerchantQrUrl(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) return null;
        return SUPABASE_URL + "/storage/v1/object/authenticated/" + STORAGE_BUCKET_MERCHANT_QRS + "/" + storagePath;
    }
}
