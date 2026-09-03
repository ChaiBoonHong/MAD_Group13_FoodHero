<div align="center">

<img src="foodhero-logo.png" alt="FoodHero Logo" width="180" />

# 🍱 FoodHero
### *Save Food. Save Money. Save the Planet.* 🌱
**Hyper-Localized Surplus Food Rescue & Marketplace Platform for UTAR Kampar Campus**

[![Platform](https://img.shields.io/badge/Platform-Android_API_28+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java_11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Backend](https://img.shields.io/badge/Backend-Supabase_PostgreSQL-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com)
[![Maps](https://img.shields.io/badge/Maps-Google_Maps_SDK-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)](https://developers.google.com/maps)
[![Build](https://img.shields.io/badge/Build-Gradle_9.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)

<br/>

[⚡ 60-Second Quick Start](#-60-second-quick-start-cli-first) • [🏗️ Architecture](#-system-architecture) • [🗄️ Database Setup](#-database-setup-cli--dashboard) • [🔑 Default Accounts](#-default-user-accounts-pre-seeded--ready) • [🧪 CLI Testing](#-cli-diagnostics--testing)

</div>

---

## 🌟 Overview

Every day across campus cafeterias, fresh edible surplus food is discarded simply because vendors reach their afternoon or evening closing hours. Meanwhile, university students are actively looking for healthy, budget-friendly meal options.

**FoodHero** bridges this gap. It enables campus merchants (Pavilion I, Pavilion II, cafeterias, and stalls) to post **Surprise Bags & Meals for ≤ RM10**, allowing students to discover, reserve in under 30 seconds, navigate directly to stalls via GPS campus routes, and redeem meals with cryptographic single-use QR tokens.

---

## ⚡ 60-Second Quick Start (CLI First)

Run these commands directly in your terminal (**PowerShell** or **Bash**) from the project root:

### 1. Clone & Navigate
```bash
git clone https://github.com/ChaiBoonHong/MAD_Group13_FoodHero.git
cd MAD_Group13_FoodHero
```

### 2. Auto-Generate Secrets Configuration
Create your local secrets configuration file:

<details open>
<summary><b>PowerShell (Windows)</b></summary>

```powershell
# Copy the template to secrets.properties (gitignored)
Copy-Item secrets.properties.example secrets.properties

# Quick check that secrets are ready
Get-Content secrets.properties
```
</details>

<details>
<summary><b>Bash (macOS / Linux)</b></summary>

```bash
cp secrets.properties.example secrets.properties
cat secrets.properties
```
</details>

### 3. Deploy Database via CLI or Dashboard
Run the single master database schema to initialize all tables, foreign keys, RLS policies, triggers, and UTAR Kampar campus landmarks:

```bash
# Option A: Supabase CLI (if you have Supabase CLI installed)
npx supabase db execute --file supabase/schema.sql

# Option B: Direct Web SQL Editor (Zero install required)
# Open https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new
# Paste the contents of supabase/schema.sql and click Run.
```

### 4. Build & Launch on Device / Emulator
```bash
# Compile and check for errors
./gradlew.bat compileDebugSources

# Assemble Debug APK & Install to connected device via ADB
./gradlew.bat installDebug

# Launch the app immediately via ADB
adb shell am start -n com.uccd3223.group13.foodhero/.ui.SplashActivity
```

---

## 🔑 User Authentication & 100% Dynamic Supabase Data

> [!IMPORTANT]
> **Zero Hardcoded Data**: All content displayed throughout FoodHero—including user profiles, eco-impact metrics, merchant outlets, active surplus food listings, order reservations, payment slips, and verification QR codes—is **retrieved dynamically from Supabase (PostgreSQL + PostgREST + Realtime WebSocket)**. No mock or fallback data is hardcoded in the codebase.

### 📋 Default Test Accounts (Stored in Supabase)

The live Supabase database is pre-configured with two verified test accounts with empty points and empty listings:

| Role | Role Selector Pill | Email | Password | Initial State & Dynamic Capabilities |
|---|---|---|---|---|
| **🎓 Student** | Tap **🎓 Student** | `student@foodhero.my` | `FoodHero123!` | • **Clean Slate**: 0 Eco-Points, 0 Meals Rescued, RM0.00 Saved, 0.0kg CO₂.<br/>• **Capabilities**: Real-time browsing of live listings, campus GPS navigation to Pavilions, DuitNow QR reservation, single-use QR pickup tokens, merchant rating reviews. |
| **🏪 Merchant** | Tap **🏪 Merchant** | `merchant@foodhero.my` | `FoodHero123!` | • **Clean Slate**: 0 Listings, 0 Reviews, Grand Green Cafe (Pavilion I).<br/>• **Capabilities**: Real-time merchant dashboard (live revenue & inventory from DB), create/manage surplus listings with photo picker, integrated ZXing camera QR scanner to verify student pickups. |

---

### 📲 How to Log In (Step-by-Step)

```
       Step 1                     Step 2                     Step 3
 ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
 │ [🎓 Student]    │  ──►  │ Email & Password│  ──►  │  [ Login ]      │
 │ [🏪 Merchant]   │       │ student@...     │       │                 │
 └─────────────────┘       └─────────────────┘       └─────────────────┘
  Select Role Pill           Enter Credentials         Launch Workspace
```

1. **Select Role Pill**: Tap either **🎓 Student** or **🏪 Merchant** at the top selector bar on the login screen.
2. **Enter Credentials**: Fill in the corresponding email and password (`FoodHero123!`), or your own registered credentials.
3. **Tap Login**: Click the **Login** button. The app queries Supabase GoTrue Auth, downloads your profile from `public.profiles`, and routes to the appropriate role workspace ([`StudentHomeActivity`](file:///d:/UTAR/Group13-FoodHero/MAD_Group13_FoodHero/app/src/main/java/com/uccd3223/group13/foodhero/ui/StudentHomeActivity.java) or [`MerchantHomeActivity`](file:///d:/UTAR/Group13-FoodHero/MAD_Group13_FoodHero/app/src/main/java/com/uccd3223/group13/foodhero/ui/MerchantHomeActivity.java)).

> [!TIP]
> **No Email Verification Barrier**: Pre-seeded accounts and all newly registered accounts are auto-confirmed in Supabase GoTrue Auth. No email confirmation link or OTP is required.

---

### 🔄 Create Your Own Accounts (Instant Self-Registration)

You can create brand new Student or Merchant accounts directly within the app:
1. Tap **"Don't have an account? Register"** on the login screen.
2. Select **🎓 Student** (enter Name, Student ID, Faculty, Email, Password) or **🏪 Merchant** (enter Name, Business Name, Campus Location, Stall Details, Email, Password).
3. Tap **Register**.
4. The database trigger automatically creates your profile in `public.profiles` and outlet in `public.merchants` with clean, zero initial stats. All subsequent actions immediately update Supabase in real-time.


---

## 🗄️ Database Setup (CLI & Dashboard)

The database runs on **Supabase (PostgreSQL 15+)** with **Row Level Security (RLS)** and automatic cascading lifecycle deletion.

### Schema Blueprint

```
auth.users (Supabase Managed GoTrue Auth)
  │
  ▼ [ON DELETE CASCADE]
public.profiles (Student / Merchant Profile & Impact Stats)
  │
  ├──► public.user_locations (Campus Geofencing State)
  │
  ├──► public.merchants (Campus Outlet Registry)
  │      │
  │      ▼ [ON DELETE CASCADE]
  │    public.listings (Surplus Mystery Bags & Inventory)
  │      │
  │      ▼ [ON DELETE CASCADE]
  ├────► public.orders (Reservations & QR Token Redemptions)
  │      │
  │      ├──► public.reviews (Star Ratings & Aggregate Rollup)
  │      └──► public.reward_redemptions (Eco Discount Ledger)
  │
  └──► public.notifications (In-App Realtime Notification Stream)
```

### Applying the Schema:
1. Open [`supabase/schema.sql`](supabase/schema.sql).
2. Go to **[Supabase Dashboard SQL Editor](https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new)**.
3. Paste the entire file and click **Run** to set up tables, RLS policies, triggers, and UTAR Kampar campus boundary landmarks.
4. The schema is **100% idempotent**—you can re-run it anytime to reset data safely.

---

## 🏗️ System Architecture

FoodHero follows modern Android architecture using the **Repository Pattern** and offline-first caching:

```
┌──────────────────────────────────────────────────────────────────┐
│                           UI LAYER                               │
│  - Activities (Auth, StudentHome, MerchantHome, OrderVerify)     │
│  - Fragments (Explore, Map, ActiveOrders, Profile, Dashboard)   │
│  - Material Design 3 Components & Edge-to-Edge System Insets     │
└─────────────────────────────────▲────────────────────────────────┘
                                  │ LiveData / Callbacks
┌─────────────────────────────────┴────────────────────────────────┐
│                       REPOSITORY LAYER                           │
│  - AuthRepository: Supabase Auth GoTrue + Local Token Management │
│  - ListingRepository: Network-first + Room cache fallbacks       │
│  - OrderRepository: Atomic reservation & QR verification         │
│  - NotificationRepository: Realtime WebSocket subscriptions      │
└─────────────────▲──────────────────────────────▲─────────────────┘
                  │ Local Room Cache             │ PostgREST / Realtime
┌─────────────────┴──────────────┐ ┌─────────────┴─────────────────┐
│         LOCAL STORAGE          │ │        SUPABASE CLOUD         │
│  - FoodHeroDatabase (Room)     │ │  - PostgREST RESTful APIs     │
│  - Encrypted SharedPrefs       │ │  - Supabase Realtime (WS)     │
│  - Offline DashboardCacheDao   │ │  - Supabase Storage Bucket    │
└────────────────────────────────┘ └───────────────────────────────┘
```

---

## 🚀 Interactive Feature Walkthrough

### 🎓 Student User Journey
1. **Launch & Log In**: Open the app, select the **🎓 Student** pill, enter `student@foodhero.my` / `FoodHero123!`, and tap **Login**.
2. **Explore Feed**: View active surplus mystery bags sorted by distance and price (≤ RM10).
3. **Interactive Map**: Open the **Map Tab** to see the UTAR Kampar campus boundary and walk routes to **Student Pavilion I** or **Pavilion II**.
4. **Reserve Meal**: Tap **Rescue Meal**, complete the DuitNow QR simulation, and confirm your reservation.
5. **Redeem with QR**: Open **Active Orders** to display your time-sensitive single-use cryptographic QR code.

### 🏪 Merchant User Journey
1. **Switch Account**: From the Student profile screen, tap **Logout**. On the login screen, select the **🏪 Merchant** pill, enter `merchant@foodhero.my` / `FoodHero123!`, and tap **Login**.
2. **Merchant Dashboard**: View real-time surplus bags remaining, orders awaiting pickup, and total revenue recovered.
3. **Publish Listing**: Tap **+ New Listing**, enter the meal title, set discounted price (≤ RM10), select pickup window, and publish.
4. **Scan & Verify**: Tap **Scan QR** to open the integrated **ZXing Camera Scanner**. Scan the student's order QR code to verify and instantly complete the transaction.

---

## 🧪 CLI Diagnostics & Testing

Use these terminal commands to verify your setup before launching:

<details>
<summary><b>1. Test Supabase Auth & API Connectivity</b></summary>

```powershell
# PowerShell: Test Supabase PostgREST connectivity
$apiKey = (Get-Content secrets.properties | Where-Object { $_ -like "SUPABASE_ANON_KEY=*" }).Split("=")[1].Trim()
$url = (Get-Content secrets.properties | Where-Object { $_ -like "SUPABASE_URL=*" }).Split("=")[1].Trim()

Invoke-RestMethod -Uri "$url/rest/v1/listings?select=id,title,discounted_price&status=eq.active" -Headers @{ apikey = $apiKey } | Format-Table
```
</details>

<details>
<summary><b>2. Stream Live Android Logs Filtered by FoodHero</b></summary>

```bash
# Stream logcat messages strictly from FoodHero
adb logcat -v time -s "AuthRepository" "ListingRepository" "SupabaseClient" "FoodHero"
```
</details>

<details>
<summary><b>3. Clear App Storage & Reset Session via CLI</b></summary>

```bash
# Clear app cache and preferences for clean re-test
adb shell pm clear com.uccd3223.group13.foodhero
```
</details>

---

## 📁 Repository Structure

```
MAD_Group13_FoodHero/
├── app/
│   ├── src/main/
│   │   ├── java/com/uccd3223/group13/foodhero/
│   │   │   ├── data/
│   │   │   │   ├── local/          # Room DB, DAOs, Entities, Cache
│   │   │   │   ├── model/          # Domain Models (Listing, Order, Profile, etc.)
│   │   │   │   ├── remote/         # Retrofit Services, Supabase REST & WebSocket
│   │   │   │   ├── repository/     # Auth, Listing, Order, Notification Repositories
│   │   │   │   └── session/        # Encrypted SessionManager
│   │   │   ├── ui/                 # Activities, Fragments, Adapters, ViewHolders
│   │   │   └── util/               # SystemBarUtils, GeoUtils, QRGenerator
│   │   └── res/                    # Layouts, Drawables, Values, Navigation
│   └── build.gradle
├── supabase/
│   ├── schema.sql                  # Single Master PostgreSQL Schema, Triggers & Campus Boundaries
│   └── supabase_setup_guide.md     # In-depth architectural documentation
├── secrets.properties.example      # Secrets template
├── build.gradle
└── README.md
```

---

## 👥 Project Team (Group 13 - UCCD3223)

| Name | Student ID | Role | Core Contributions |
|---|---|---|---|
| **Chai Boon Hong** | `22ACB01234` | Lead Developer | System Architecture, Database Schema, Supabase Auth/REST, Student Workflows, Google Maps Routing Engine, QR Engine |
| **Fong Chee Hou** | `22ACB05678` | Developer | Merchant UI Workflows, Listing Management, ZXing Camera Scanner Integration, Quality Assurance |

---

## 📄 License & Academic Declaration
Developed as part of the **UCCD3223 Mobile Applications Development** curriculum at **Universiti Tunku Abdul Rahman (UTAR)**. All rights reserved.
