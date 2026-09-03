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

[⚡ 60-Second Quick Start](#-60-second-quick-start-cli-first) • [🏗️ Architecture](#-system-architecture) • [🗄️ Database Setup](#-database-setup-cli--dashboard) • [🔑 Demo Accounts](#-pre-seeded-demo-accounts) • [🧪 CLI Testing](#-cli-diagnostics--testing)

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
Run the master database schema to initialize tables, cascade foreign keys, and seed realistic UTAR Kampar campus merchants:

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

## 🔑 Pre-Seeded Demo Accounts

The app includes a **"Quick Account Fill"** bar on the login screen. Click either button to auto-fill and log in immediately:

| Persona | Quick Fill Email | Password | Pre-seeded Features |
|---|---|---|---|
| **🎓 Student** | `student@foodhero.my` | `FoodHero123!` | 120 Eco-Points, 7 Rescued Meals, RM38.50 Saved, FICT Faculty |
| **🏪 Merchant** | `merchant@foodhero.my` | `FoodHero123!` | Grand Green Cafe (Pavilion I), 3 Stalls, 4 Active Surplus Bags |

> **Note:** Both accounts are pre-verified. You do not need to confirm via email.

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
3. Paste the entire file and click **Run**.
4. The script is **100% idempotent**—you can re-run it anytime to reset and clean data safely.

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
1. **Launch App**: Open the app and click **Student (Demo)** on the login screen.
2. **Explore Feed**: View active surplus mystery bags sorted by distance and price (≤ RM10).
3. **Interactive Map**: Open the **Map Tab** to see UTAR Kampar campus boundary and walk routes to **Student Pavilion I** or **Pavilion II**.
4. **Reserve Meal**: Tap **Rescue Meal** to initiate an atomic 1-click reservation.
5. **Redeem with QR**: Open **Active Orders** to display your time-sensitive single-use cryptographic QR code.

### 🏪 Merchant User Journey
1. **Switch Account**: Log out and tap **Merchant (Demo)** on the login screen.
2. **Merchant Dashboard**: View today's surplus bags remaining, orders reserved, and revenue recovered.
3. **Publish Listing**: Tap **+ New Listing**, enter item title, set discounted price (e.g. RM 5.50), select pickup window, and publish.
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
│   ├── schema.sql                  # Master Consolidated PostgreSQL Schema & Seeds
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
