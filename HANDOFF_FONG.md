# FoodHero — Developer Handoff Guide for Fong Chee Hou

Welcome, Fong Chee Hou! The FoodHero core platform, Supabase backend contracts, Eco-Vibrant design system, and student workflows are fully built and verified by Chai Boon Hong.

This document contains everything you need to implement your assigned 20% merchant presentation layer cleanly and smoothly.

---

## 1. Quick Start & Git Branching

1. Branch off `main`:
   ```bash
   git fetch origin
   git checkout -b feature/fong-merchant-ui origin/main
   ```
2. Open the project in Android Studio.
3. Build and run tests to verify your baseline:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```

---

## 2. Pre-configured Shell & Routing

- **Manifest & Router:** `MerchantHomeActivity` is already registered in `AndroidManifest.xml`. When a merchant logs in, the session router (`SplashActivity`) routes directly to `MerchantHomeActivity`.
- **Bottom Navigation:** Pre-configured with 4 tabs:
  1. `Dashboard` (`@id/nav_dashboard`)
  2. `Listings` (`@id/nav_listings`)
  3. `Orders` (`@id/nav_merchant_orders`)
  4. `Profile` (`@id/nav_profile`)

---

## 3. Merchant Service API Reference

You will interact exclusively with `FoodHeroRepository.getInstance(context)`. All methods run asynchronously on background threads and return their callbacks on the Android main thread.

### A. Merchant Dashboard & Analytics
```java
FoodHeroRepository repo = FoodHeroRepository.getInstance(context);
String merchantId = "m1"; // or SessionManager.getInstance(context).getUserId()

repo.getMerchantDashboard(merchantId, new ResultCallback<MerchantDashboardData>() {
    @Override
    public void onSuccess(MerchantDashboardData data) {
        // data.getRevenueRecovered() -> e.g. 342.50
        // data.getFoodDivertedKg()   -> e.g. 48.2
        // data.getOrdersCompleted()  -> e.g. 46
        // data.getAverageRating()    -> e.g. 4.9
        // data.getActiveListingsCount() -> e.g. 4
        // data.getLowStockAlertsCount() -> e.g. 1
        // data.getRecentOrders()     -> List<Order>
    }

    @Override
    public void onError(DataError error) {
        Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### B. Merchant Listings & Add/Edit Listing
```java
// 1. Fetch Merchant Listings
repo.getMerchantListings(merchantId, new ResultCallback<List<Listing>>() {
    @Override
    public void onSuccess(List<Listing> listings) {
        // Bind to your Merchant Listing RecyclerView adapter
    }
    @Override
    public void onError(DataError error) {}
});

// 2. Create New Listing (Dual-Source Photo Handling)
Listing newListing = new Listing();
newListing.setMerchantId(merchantId);
newListing.setTitle("Surplus Pastry Surprise Bag");
newListing.setDescription("3 fresh croissants and muffins");
newListing.setCategory("Bakery");
newListing.setOriginalPrice(10.00);
newListing.setDiscountedPrice(4.50); // Must be <= RM10.00
newListing.setRemainingQuantity(3);
newListing.setTotalQuantity(3);
newListing.setPickupStart("17:00");
newListing.setPickupEnd("19:00");
newListing.setPickupLocation("Student Pavilion II, Bakery Counter");
newListing.setLatitude(4.337500);  // Must be inside UTAR Kampar boundary
newListing.setLongitude(101.143800);
newListing.setCo2KgPerItem(0.95);

// Dual-source: Either Supabase upload or external HTTPS link:
boolean isExternalUrl = true; 
String imageLink = "https://images.unsplash.com/photo-1555507036-ab1f4038808a";

repo.createListing(newListing, imageLink, isExternalUrl, new ResultCallback<Listing>() {
    @Override
    public void onSuccess(Listing created) {
        Toast.makeText(context, "Listing published successfully!", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onError(DataError error) {
        Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
    }
});

// 3. Deactivate or Restock
repo.deactivateListing(listingId, callback);
repo.restockListing(listingId, 5 /* additional quantity */, callback);
```

### C. Merchant Orders & QR Pickup Verification
```java
// 1. Fetch Orders Queue
repo.getMerchantOrders(merchantId, new ResultCallback<List<Order>>() {
    @Override
    public void onSuccess(List<Order> orders) {
        // Show Reserved and Completed order rows
    }
    @Override
    public void onError(DataError error) {}
});

// 2. Verify Single-Use QR Token or Manual Code
// When QR is scanned or manual code (e.g. "FH-829104" or "FH-TOKEN-829104") is entered:
repo.verifyPickupToken(scannedCode, merchantId, new ResultCallback<OrderVerificationResult>() {
    @Override
    public void onSuccess(OrderVerificationResult result) {
        if (result.isValid()) {
            // Show Success Dialog / Animation
            // result.getMessage() -> "Pickup verified successfully! 10 Eco-Points awarded to student."
        } else {
            // Show Error / Already Used feedback
            // result.getMessage() -> "Order has already been picked up and completed."
        }
    }
    @Override
    public void onError(DataError error) {
        Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```

### D. Merchant Reviews & Role-Aware Notifications
```java
// Fetch reviews received for owned food bags
repo.getMerchantReviews(merchantId, new ResultCallback<List<Review>>() {
    @Override
    public void onSuccess(List<Review> reviews) { ... }
    @Override
    public void onError(DataError error) { ... }
});

// Fetch merchant notifications
repo.getNotifications(UserRole.MERCHANT, new ResultCallback<List<FoodHeroNotification>>() {
    @Override
    public void onSuccess(List<FoodHeroNotification> list) { ... }
    @Override
    public void onError(DataError error) { ... }
});
```

---

## 4. UI Tokens & Styling Reference

Use the locked styles so your screens match the app's Eco-Vibrant theme without hardcoding values:

| Component | Style Resource | Usage |
|---|---|---|
| Primary Button | `@style/Widget.FoodHero.Button.Primary` | Main actions (e.g. "Create Listing", "Verify Pickup") |
| Secondary Button | `@style/Widget.FoodHero.Button.Secondary` | Secondary actions (e.g. "Restock", "Manual Code") |
| Form Fields | `@style/Widget.FoodHero.TextInputLayout` | Text inputs (Prices, Title, Pickup Times) |
| Cards | `@style/Widget.FoodHero.Card` | Listing cards, order cards |
| Metric Tiles | `@style/Widget.FoodHero.Card.Metric` | Revenue recovered, food diverted metric boxes |
| Empty States | `<include layout="@layout/layout_empty_state" />` | Display when list is empty |

---

## 5. Demo Credentials

Use these pre-seeded demo accounts in Android Studio:
- **Merchant Account:** `merchant.demo@utar.edu.my` | Password: `Demo1234!`
- **Student Account:** `student.demo@utar.edu.my` | Password: `Demo1234!`
