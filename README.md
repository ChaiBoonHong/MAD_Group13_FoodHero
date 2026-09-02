<div align="center">

<img src="foodhero-logo.jpeg" alt="FoodHero Logo" width="180" style="border-radius: 36px; box-shadow: 0 10px 30px rgba(0,0,0,0.15);" />

# 🍱 FoodHero (MAD_Group13)
### *Save Food. Save Money. Save the Planet.* 🌱
**UTAR Kampar Campus Surplus Food Rescue & Marketplace Platform**

[![Android](https://img.shields.io/badge/Platform-Android_SDK_28%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java_11-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Supabase](https://img.shields.io/badge/Backend-Supabase_PostgreSQL-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)](https://supabase.com)
[![Google Maps](https://img.shields.io/badge/Maps-Google_Maps_SDK-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)](https://developers.google.com/maps)
[![Architecture](https://img.shields.io/badge/Architecture-Clean_Repository_Pattern-blueviolet?style=for-the-badge)](https://developer.android.com/topic/architecture)

<br/>

</div>

---

## 🌟 Overview

**FoodHero** is a hyper-localized surplus food rescue platform engineered specifically for the **Universiti Tunku Abdul Rahman (UTAR) Kampar Campus** community. 

Every day, campus eateries and local bakeries discard edible surplus food simply because of closing hours. At the same time, university students seek high-quality, budget-friendly meal options. **FoodHero bridges this gap** through a real-time, geofenced, anti-fraud food reservation ecosystem.

```
+-----------------------------------------------------------------------------------+
|                                  FOODHERO ECOSYSTEM                               |
|                                                                                   |
|   🏪 Merchants (Grand Green, Pav I, etc.)           🎓 UTAR Students              |
|        │                                                 │                        |
|        ├─ Post Surprise Bags (<= RM10)                   ├─ Discover Nearby Meals |
|        ├─ Real-time Stock Management                     ├─ Reserve Under 30s     |
|        └─ Camera QR Verification Engine                  ├─ Campus Map Navigation |
|                     ▲                                    └─ Earn Carbon Eco-Points|
|                     │                                                ▲            |
|                     └───► [ Supabase Cloud BaaS (PostgreSQL) ] ◄─────┘            |
|                                ├── Row-Level Security (RLS)                       |
|                                ├── Real-time Event Streams                        |
|                                └── Atomic Stock Transaction Engine                |
+-----------------------------------------------------------------------------------+
```

---

## 🚀 Key Features

### 🎓 For Students
- 🔍 **Live Surplus Discovery Feed**: Browse surplus bento boxes, bakeries, and snacks with live countdown timers (`1h 30m left`) and stock indicators.
- 🗺️ **Geofenced UTAR Kampar Campus Map**: Real-time walking, cycling, and campus shuttle route guidance directly to merchant stalls.
- ⚡ **Atomic 1-Click Reservation**: Instant reservation lock preventing race conditions or overselling.
- 🛡️ **Fraud-Proof QR Pickup Tokens**: Single-use, time-sensitive cryptographic QR tokens generated on-device for secure redemption.
- 🌳 **Eco-Hero Impact Tracker**: Real-time calculation of rescued meals, money saved (RM), and CO₂ emissions diverted with interactive Eco-Tree badges.

### 🏪 For Merchants
- 📦 **Rapid Listing Creation**: Publish surplus mystery bags within 15 seconds with photo upload or instant photo URL preview.
- 📷 **Built-in ZXing QR Scanner**: Validate student tokens on pickup in < 1 second with automated single-use invalidation.
- 📊 **Merchant Analytics Dashboard**: Track daily recovered revenue, food diverted (kg), customer ratings, and active order queues.
- 🔔 **Instant Order Alerts**: Foreground Supabase Realtime alerts & Android `WorkManager` background polling notifications.

---

## 🏗️ System Architecture

FoodHero follows Android Modern Architecture principles with a robust **Repository Pattern**:

```
 ┌────────────────────────────────────────────────────────┐
 │                      UI Layer                          │
 │   Activities, Fragments, RecyclerView Adapters, M3     │
 └───────────────────────────▲────────────────────────────┘
                             │ View Binding / Observers
 ┌───────────────────────────┴────────────────────────────┐
 │                   Repository Layer                     │
 │   ListingRepository, OrderRepository, AuthRepository   │
 └─────────────▲────────────────────────────▲─────────────┘
               │ Local Cache                │ REST / Realtime
 ┌─────────────┴──────────────┐ ┌───────────┴─────────────┐
 │    Local Storage (Room)    │ │   Remote Backend (Cloud)│
 │  - Offline Listings Cache  │ │  - Supabase REST API    │
 │  - Encrypted SharedPrefs   │ │  - Supabase Realtime WS │
 │  - WorkManager Workers     │ │  - Google Maps & Routes │
 └────────────────────────────┘ └─────────────────────────┘
```

---

## 🛠️ Tech Stack & Libraries

| Category | Technology | Purpose |
|---|---|---|
| **Language** | Java 11 | Core Android application development |
| **Minimum SDK** | Android 9.0 (API 28) | High modern device coverage with native hardware security |
| **Target SDK** | Android 16 (API 36) | Latest Android performance and privacy standards |
| **Backend & Auth** | [Supabase](https://supabase.com/) | PostgreSQL database, Row Level Security (RLS), Realtime WebSocket |
| **Networking** | Retrofit 2 + OkHttp 4 | High-performance REST communication with Supabase PostgREST |
| **Local Persistence** | Android Room Database | Offline capability and fast UI caching |
| **Image Loading** | Bumptech Glide | Smooth asynchronous image caching and transformation |
| **Maps & Location** | Google Play Services Maps & Location | UTAR Kampar campus boundary rendering and routing polyline |
| **QR Engine** | ZXing Core + JourneyApps Scanner | Dynamic QR token generation and real-time camera scanning |
| **Background Sync** | Android Jetpack WorkManager | Reliable background notification sync and low-stock polling |

---

## 📦 Getting Started & Setup

### 1. Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer
- **JDK 11+** installed and configured
- A physical Android device or emulator running **API 28+**
- Active **Supabase** project and **Google Maps API Key**

### 2. Clone the Repository
```bash
git clone https://github.com/ChaiBoonHong/MAD_Group13_FoodHero.git
cd MAD_Group13_FoodHero
```

### 3. Configure Secrets
Create a `secrets.properties` file in the root directory (or copy from `secrets.properties.example`):

```properties
# Supabase Configuration
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-supabase-anon-key

# Google Maps SDK
MAPS_API_KEY=AIzaSyYourGoogleMapsApiKeyHere
```

### 4. Database Setup (Supabase)
Run the SQL migration scripts located in `/supabase/migrations` inside your Supabase SQL Editor to initialize:
- `profiles`, `listings`, `orders`, `reviews`, and `notifications` tables.
- Row Level Security (RLS) policies for Student & Merchant role isolation.
- Atomic stock decrement triggers.

### 5. Build and Run
```bash
# Build Debug APK via Gradle
./gradlew assembleDebug

# Install to connected ADB device
./gradlew installDebug
```

---

## 👥 Project Team (Group 13 - UCCD3223)

| Name | Role | Core Contributions |
|---|---|---|
| **Chai Boon Hong** | Lead Developer (80%) | App Architecture, Supabase Data Contracts, Student Workflow, Map Routing Engine, QR Security Tokenizer |
| **Fong Chee Hou** | Developer (20%) | Merchant UI Workflow, Merchant Listing Management, Scanner UI Integration |

---

## 📄 License
This project is developed as part of the **UCCD3223 Mobile Applications Development** course at Universiti Tunku Abdul Rahman (UTAR). All rights reserved.
