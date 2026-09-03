# FoodHero - Supabase Authentication & Realtime Setup Guide

This guide walks you through configuring **Supabase Authentication**, **Google OAuth Provider**, and **PostgreSQL Realtime Notifications** (without FCM) for the FoodHero Android application.

---

## 1. Database Configuration (SQL Editor)

Open your Supabase project dashboard:
👉 **[Supabase Dashboard - SQL Editor](https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new)**

Paste and run the following script:

```sql
-- ============================================================================
-- 1. Enable Supabase Realtime for Notifications (Without FCM)
-- ============================================================================
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
ALTER TABLE notifications REPLICA IDENTITY FULL;

-- ============================================================================
-- 2. Row Level Security (RLS) for Profiles
-- ============================================================================
-- Allow authenticated users to insert & upsert their own profile row
DO $$ BEGIN
  CREATE POLICY "Users can insert own profile" ON profiles 
      FOR INSERT WITH CHECK (auth.uid() = id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================================
-- 3. Automatic Profile Creation Trigger (Email & Google Sign-In)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name, role)
  VALUES (
    new.id,
    new.email,
    COALESCE(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)),
    COALESCE((new.raw_user_meta_data->>'role')::user_role, 'student'::user_role)
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    updated_at = NOW();
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
```

---

## 2. Supabase Email Authentication Settings

To allow immediate student and merchant login without blocking on email confirmation emails:

1. In the Supabase Dashboard, open **Authentication** ➔ **Providers** ➔ **Email**.
2. Uncheck **"Confirm email"**.
3. Click **Save**.

---

## 3. Enable Google Sign-In Provider

### Step 3.1: Create Google Cloud OAuth Credentials

1. Open the [Google Cloud Console Credentials](https://console.cloud.google.com/apis/credentials).
2. Click **Create Credentials** ➔ **OAuth client ID**.
3. Select Application type: **Web application**.
4. Name: `FoodHero Web Client`.
5. Under **Authorized redirect URIs**, add:
   ```
   https://qouifvxsnevpqzafkdbf.supabase.co/auth/v1/callback
   ```
6. Click **Create**, then copy:
   - **Client ID** (e.g., `1234567890-abcdef.apps.googleusercontent.com`)
   - **Client Secret**

### Step 3.2: Configure Google Provider in Supabase

1. In your Supabase Dashboard, navigate to **Authentication** ➔ **Providers** ➔ **Google**.
2. Toggle **Enable Google provider** to ON.
3. Paste your **Client ID** and **Client Secret**.
4. Click **Save**.

### Step 3.3: Add Client ID to Android Project

In your local `secrets.properties` file:

```properties
GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
```

---

## 4. How Realtime Notifications Work Without FCM

The app communicates directly with Supabase via WebSocket:

```
PostgreSQL (INSERT on 'notifications' table)
   ↓ (WAL Replication)
Supabase Realtime Server (wss://qouifvxsnevpqzafkdbf.supabase.co/realtime/v1/websocket)
   ↓ (Phoenix v2 postgres_changes channel)
FoodHero Android App (SupabaseRealtimeClient)
   ↓
1. In-App Notification Bell & Badge Count (+1)
2. Android Heads-Up Notification (NotificationManager + Sound + Vibration)
```

No Google Play Services FCM or Firebase project configuration is needed!
