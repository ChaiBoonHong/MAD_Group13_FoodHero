# FoodHero - Supabase Database Architecture & Setup Guide

This guide details the reorganized, domain-driven PostgreSQL database architecture for FoodHero (UTAR Kampar Campus) and the steps to execute it in Supabase.

---

## 1. Clean Database Schema (One-Click Setup)

All previous fragmented migrations (`001`, `002`, `003`) have been consolidated into a single master schema file:
📁 **[`supabase/schema.sql`](file:///d:/UTAR/Group13-FoodHero/MAD_Group13_FoodHero/supabase/schema.sql)** (or [`supabase/migrations/001_consolidated_schema.sql`](file:///d:/UTAR/Group13-FoodHero/MAD_Group13_FoodHero/supabase/migrations/001_consolidated_schema.sql)).

### How to apply:
1. Open your Supabase SQL Editor:
   👉 **[Supabase Dashboard - SQL Editor](https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new)**
2. Copy the entire contents of [`supabase/schema.sql`](file:///d:/UTAR/Group13-FoodHero/MAD_Group13_FoodHero/supabase/schema.sql) and paste it into the editor.
3. Click **Run**.

---

## 2. Reorganized Domain Architecture

The database is structured into 6 clean, decoupled domains:

```
auth.users (Supabase Managed GoTrue Auth)
  │
  ▼ [ON DELETE CASCADE]
public.profiles (Identity & Impact Stats)
  │
  ├──► public.user_locations (Campus Geofencing)
  │
  ├──► public.merchants (Owner Outlet Info)
  │      │
  │      ▼ [ON DELETE CASCADE]
  │    public.listings (Surplus Mystery Bags)
  │      │
  │      ▼ [ON DELETE CASCADE]
  ├────► public.orders (Reservations & Verifications)
  │      │
  │      ├──► public.reviews (Star Ratings & Comments)
  │      └──► public.reward_redemptions (Eco Discount Audit)
  │
  └──► public.notifications (In-App Realtime Push)
```

### Domain Breakdown:
1. **Identity & Profiles**:
   - `profiles`: Linked directly to `auth.users(id)` with `ON DELETE CASCADE`. Deleting any user from the Supabase Authentication dashboard cleanly wipes all associated profile, order, and listing data without foreign key errors.
   - `student_allowlist` & `merchant_allowlist`: Verified institutional demo data.
2. **Campus Geolocation**:
   - `service_areas`: UTAR Kampar Campus boundary polygon for strict boundary validation.
   - `campus_landmarks`: Pre-approved pickup landmarks (Pavilion I, Pavilion II, Library, Gates).
   - `user_locations`: Dynamic GPS tracking table.
3. **Catalog & Merchants**:
   - `merchants`: Campus vendor outlets with geo-coordinates and aggregated ratings.
   - `listings`: Active surplus food bags, stock counters, prices (under RM10), and pickup windows.
4. **Transactions & Rewards**:
   - `orders`: Atomic reservations with unique order codes and pickup verification tokens.
   - `reward_redemptions`: Ledger recording Eco Points redeemed for order discounts.
5. **Feedback & Realtime**:
   - `reviews`: Student ratings directly updating merchant metrics via PostgreSQL triggers.
   - `notifications`: Realtime publication table with WebSocket replication.
6. **Automated Business Logic Triggers**:
   - `on_auth_user_created`: Instant profile provisioning upon email signup or Google OAuth.
   - `trg_process_order_reservation`: Atomic stock reduction and automated stock-out alerts.
   - `trg_process_order_completion`: Automatic student Eco Point and CO2 prevention awards upon order completion.
   - `trg_process_review_submission`: Automated calculation of merchant average star rating.

---

## 3. Performance Indexes

B-Tree indexes are applied on foreign keys and frequently queried filter columns:
- `idx_listings_merchant_status` on `listings(merchant_id, status)`
- `idx_orders_student` on `orders(student_id, status)`
- `idx_orders_merchant` on `orders(merchant_id, status)`
- `idx_reviews_merchant` on `reviews(merchant_id)`
- `idx_notifications_recipient` on `notifications(recipient_id, is_read)`

---

## 4. Quick Account Fill Credentials

Once the consolidated schema is executed, both pre-seeded demo accounts are fully active:

| Role | Email | Password | Pre-seeded Features |
|---|---|---|---|
| **Student** | `student@foodhero.my` | `FoodHero123!` | 120 Eco Points, 7 Meals Rescued, FICT |
| **Merchant** | `merchant@foodhero.my` | `FoodHero123!` | Grand Green Cafe, 3 Outlets, 4 Active Listings |
