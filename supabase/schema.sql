-- ============================================================================
-- FOODHERO MASTER DATABASE SCHEMA (UTAR KAMPAR CAMPUS)
-- Complete, Clean, Production-Grade Architecture for Supabase (PostgreSQL)
-- 
-- Single source of truth. Idempotent execution.
-- Paste & Run in Supabase SQL Editor:
-- https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new
-- ============================================================================

-- ============================================================================
-- SECTION 1: EXTENSIONS & CUSTOM TYPES
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('student', 'merchant');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE listing_status AS ENUM ('active', 'sold_out', 'expired', 'draft');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE order_status AS ENUM ('awaiting_payment', 'pending_verification', 'reserved', 'rejected', 'completed', 'cancelled', 'expired');
EXCEPTION WHEN duplicate_object THEN
    BEGIN
        ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'awaiting_payment' BEFORE 'reserved';
        ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'pending_verification' BEFORE 'reserved';
        ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'rejected' AFTER 'reserved';
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
END $$;

DO $$ BEGIN
    CREATE TYPE image_source_type AS ENUM ('storage', 'external_url', 'none');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================================
-- SECTION 2: DOMAIN 1 - IDENTITY & ACCESS MANAGEMENT
-- ============================================================================

-- 2.1 PROFILES (Core User Model)
-- Strictly linked to Supabase auth.users with ON DELETE CASCADE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    role user_role NOT NULL DEFAULT 'student',
    full_name TEXT NOT NULL DEFAULT 'FoodHero User',
    student_id TEXT,
    faculty TEXT,
    eco_points INT NOT NULL DEFAULT 0 CHECK (eco_points >= 0),
    meals_rescued INT NOT NULL DEFAULT 0 CHECK (meals_rescued >= 0),
    money_saved NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (money_saved >= 0),
    co2_prevented NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (co2_prevented >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Ensure cascade delete is applied if table already existed previously
DO $$ BEGIN
    ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;
    ALTER TABLE public.profiles
        ADD CONSTRAINT profiles_id_fkey
        FOREIGN KEY (id) REFERENCES auth.users(id)
        ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- ============================================================================
-- SECTION 3: DOMAIN 2 - CAMPUS GEOLOCATION & MAPPING
-- ============================================================================

-- 3.1 SERVICE AREAS (Authoritative UTAR Kampar Campus Polygon)
CREATE TABLE IF NOT EXISTS public.service_areas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    center_latitude DOUBLE PRECISION NOT NULL DEFAULT 4.336214,
    center_longitude DOUBLE PRECISION NOT NULL DEFAULT 101.142111,
    polygon_coordinates JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3.2 CAMPUS LANDMARKS (Approved Landmark Hotspots)
CREATE TABLE IF NOT EXISTS public.campus_landmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    category TEXT NOT NULL, -- 'entrance', 'academic_block', 'student_pavilion', 'landmark'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3.3 USER LOCATIONS (Live Student Geofence State)
CREATE TABLE IF NOT EXISTS public.user_locations (
    student_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_inside_campus BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- SECTION 4: DOMAIN 3 - MERCHANTS & SURPLUS LISTINGS
-- ============================================================================

-- 4.1 MERCHANTS (Cafeteria Stalls & Outlets)
CREATE TABLE IF NOT EXISTS public.merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    business_name TEXT NOT NULL,
    campus_location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL DEFAULT 4.336214,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 101.142111,
    closing_time TEXT NOT NULL DEFAULT '18:00',
    rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    total_reviews INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$ BEGIN
    ALTER TABLE public.merchants DROP CONSTRAINT IF EXISTS merchants_owner_id_fkey;
    ALTER TABLE public.merchants
        ADD CONSTRAINT merchants_owner_id_fkey
        FOREIGN KEY (owner_id) REFERENCES public.profiles(id)
        ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE public.merchants
        ADD CONSTRAINT merchants_owner_id_unique
        UNIQUE (owner_id);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- 4.2 LISTINGS (Surplus Mystery Bags & Food Items)
CREATE TABLE IF NOT EXISTS public.listings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES public.merchants(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL DEFAULT 'Meals',
    original_price NUMERIC(10, 2) NOT NULL CHECK (original_price > 0),
    discounted_price NUMERIC(10, 2) NOT NULL CHECK (discounted_price > 0 AND discounted_price <= 10.00 AND discounted_price < original_price),
    remaining_quantity INT NOT NULL DEFAULT 1 CHECK (remaining_quantity >= 0),
    total_quantity INT NOT NULL DEFAULT 1 CHECK (total_quantity >= remaining_quantity),
    image_source image_source_type NOT NULL DEFAULT 'none',
    image_url TEXT,
    storage_path TEXT,
    pickup_start TEXT NOT NULL, -- e.g. "16:00"
    pickup_end TEXT NOT NULL,   -- e.g. "18:00"
    pickup_location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    co2_kg_per_item NUMERIC(5, 2) NOT NULL DEFAULT 1.20,
    status listing_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- SECTION 5: DOMAIN 4 - ORDERS & REWARDS
-- ============================================================================

-- 5.1 ORDERS (Surplus Food Reservations)
CREATE TABLE IF NOT EXISTS public.orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_code TEXT UNIQUE NOT NULL,
    student_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    merchant_id UUID NOT NULL REFERENCES public.merchants(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    total_original_price NUMERIC(10, 2) NOT NULL,
    total_discounted_price NUMERIC(10, 2) NOT NULL,
    reward_points_used INT NOT NULL DEFAULT 0 CHECK (reward_points_used >= 0),
    reward_discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    final_paid_price NUMERIC(10, 2) NOT NULL CHECK (final_paid_price >= 0),
    pickup_start TEXT NOT NULL,
    pickup_end TEXT NOT NULL,
    pickup_token TEXT NOT NULL,
    status order_status NOT NULL DEFAULT 'awaiting_payment',
    payment_expires_at BIGINT,
    payment_receipt_url TEXT,
    payment_method TEXT NOT NULL DEFAULT 'DUITNOW_QR',
    payment_reference TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Ensure newly added columns exist if table was already created in Supabase
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_expires_at BIGINT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_receipt_url TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_method TEXT NOT NULL DEFAULT 'DUITNOW_QR';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_reference TEXT;

-- 5.2 REWARD REDEMPTIONS (Audit Trail for Eco Point Discounts)
CREATE TABLE IF NOT EXISTS public.reward_redemptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    points_deducted INT NOT NULL CHECK (points_deducted > 0),
    discount_amount NUMERIC(10, 2) NOT NULL CHECK (discount_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- SECTION 6: DOMAIN 5 - REVIEWS & NOTIFICATIONS
-- ============================================================================

-- 6.1 REVIEWS (Student Feedback & Ratings)
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID UNIQUE NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES public.listings(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    merchant_id UUID NOT NULL REFERENCES public.merchants(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6.2 NOTIFICATIONS (Realtime In-App Notifications)
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    recipient_role user_role NOT NULL DEFAULT 'student',
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    event_type TEXT NOT NULL,
    related_listing_id UUID REFERENCES public.listings(id) ON DELETE SET NULL,
    related_order_id UUID REFERENCES public.orders(id) ON DELETE SET NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- SECTION 7: PERFORMANCE INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);
CREATE INDEX IF NOT EXISTS idx_merchants_owner ON public.merchants(owner_id);
CREATE INDEX IF NOT EXISTS idx_listings_merchant_status ON public.listings(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_listings_status ON public.listings(status);
CREATE INDEX IF NOT EXISTS idx_orders_student ON public.orders(student_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON public.orders(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_code ON public.orders(order_code);
CREATE INDEX IF NOT EXISTS idx_orders_token ON public.orders(pickup_token);
CREATE INDEX IF NOT EXISTS idx_reviews_merchant ON public.reviews(merchant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications(recipient_id, is_read);

-- ============================================================================
-- SECTION 8: AUTOMATED BUSINESS LOGIC TRIGGERS
-- ============================================================================

-- 8.1 TRIGGER: Auto-create Profile and Merchant Outlet on Auth Signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    user_full_name TEXT;
    user_role_val public.user_role;
    user_student_id TEXT;
    user_faculty TEXT;
    biz_name TEXT;
    camp_loc TEXT;
BEGIN
    user_full_name := COALESCE(
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'name',
        split_part(NEW.email, '@', 1)
    );
    
    BEGIN
        user_role_val := (NEW.raw_user_meta_data->>'role')::public.user_role;
    EXCEPTION WHEN OTHERS THEN
        user_role_val := 'student'::public.user_role;
    END;

    user_student_id := NEW.raw_user_meta_data->>'student_id';
    user_faculty := NEW.raw_user_meta_data->>'faculty';
    biz_name := COALESCE(NEW.raw_user_meta_data->>'business_name', user_full_name, 'Merchant Outlet');
    camp_loc := COALESCE(NEW.raw_user_meta_data->>'campus_location', 'Student Pavilion I, Cafeteria');

    -- Insert clean profile (0 initial stats)
    INSERT INTO public.profiles (
        id, email, full_name, role, student_id, faculty,
        eco_points, meals_rescued, money_saved, co2_prevented
    )
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(user_full_name, 'FoodHero User'),
        COALESCE(user_role_val, 'student'::public.user_role),
        user_student_id,
        user_faculty,
        0, 0, 0.00, 0.00
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        full_name = COALESCE(EXCLUDED.full_name, profiles.full_name),
        role = EXCLUDED.role,
        student_id = COALESCE(EXCLUDED.student_id, profiles.student_id),
        faculty = COALESCE(EXCLUDED.faculty, profiles.faculty),
        updated_at = NOW();

    -- If merchant, auto-create their corresponding merchant record
    IF user_role_val = 'merchant' THEN
        INSERT INTO public.merchants (
            owner_id, business_name, campus_location, latitude, longitude, closing_time, rating, total_reviews
        )
        VALUES (
            NEW.id, biz_name, camp_loc, 4.336214, 101.142111, '18:00', 0.00, 0
        )
        ON CONFLICT (owner_id) DO UPDATE
        SET business_name = EXCLUDED.business_name,
            campus_location = EXCLUDED.campus_location;
    END IF;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    INSERT INTO public.profiles (id, email, full_name, role)
    VALUES (NEW.id, NEW.email, split_part(NEW.email, '@', 1), 'student'::public.user_role)
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 8.2 TRIGGER: Atomic Inventory Reservation
CREATE OR REPLACE FUNCTION public.process_order_reservation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    curr_stock INT;
    listing_rec RECORD;
BEGIN
    SELECT * INTO listing_rec FROM public.listings WHERE id = NEW.listing_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Listing not found.';
    END IF;
    
    IF listing_rec.status != 'active' THEN
        RAISE EXCEPTION 'Listing is no longer active.';
    END IF;
    
    IF listing_rec.remaining_quantity < NEW.quantity THEN
        RAISE EXCEPTION 'Insufficient stock. Remaining: %', listing_rec.remaining_quantity;
    END IF;

    curr_stock := listing_rec.remaining_quantity - NEW.quantity;
    UPDATE public.listings 
    SET remaining_quantity = curr_stock,
        status = CASE WHEN curr_stock = 0 THEN 'sold_out'::listing_status ELSE status END,
        updated_at = NOW()
    WHERE id = NEW.listing_id;

    IF NEW.reward_points_used > 0 THEN
        UPDATE public.profiles
        SET eco_points = eco_points - NEW.reward_points_used
        WHERE id = NEW.student_id;

        INSERT INTO public.reward_redemptions (student_id, order_id, points_deducted, discount_amount)
        VALUES (NEW.student_id, NEW.id, NEW.reward_points_used, NEW.reward_discount_amount);
    END IF;

    INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_listing_id, related_order_id)
    VALUES (
        listing_rec.merchant_id, 
        'merchant', 
        'New Surplus Bag Reservation!', 
        'Order #' || NEW.order_code || ' reserved for ' || NEW.quantity || ' item(s).',
        'reservation_created',
        NEW.listing_id,
        NEW.id
    );

    IF curr_stock = 0 THEN
        INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_listing_id)
        VALUES (
            listing_rec.merchant_id, 
            'merchant', 
            'Listing Sold Out', 
            '"' || listing_rec.title || '" has sold out!',
            'listing_sold_out',
            NEW.listing_id
        );
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_process_order_reservation ON public.orders;
CREATE TRIGGER trg_process_order_reservation
    BEFORE INSERT ON public.orders
    FOR EACH ROW EXECUTE FUNCTION public.process_order_reservation();

-- 8.3 TRIGGER: Order Status Progression & Restitution
CREATE OR REPLACE FUNCTION public.process_order_status_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    listing_rec RECORD;
    earned_pts INT;
    saved_amt NUMERIC(10, 2);
    co2_amt NUMERIC(10, 2);
BEGIN
    -- Completed order -> Award Eco-points, update student stats
    IF OLD.status != 'completed' AND NEW.status = 'completed' THEN
        SELECT * INTO listing_rec FROM public.listings WHERE id = NEW.listing_id;
        earned_pts := NEW.quantity * 10;
        saved_amt := (NEW.total_original_price - NEW.final_paid_price);
        co2_amt := (COALESCE(listing_rec.co2_kg_per_item, 1.20) * NEW.quantity);

        UPDATE public.profiles
        SET eco_points = eco_points + earned_pts,
            meals_rescued = meals_rescued + NEW.quantity,
            money_saved = money_saved + saved_amt,
            co2_prevented = co2_prevented + co2_amt,
            updated_at = NOW()
        WHERE id = NEW.student_id;

        INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
        VALUES (
            NEW.student_id,
            'student',
            'Pickup Completed! +10 Points Earned',
            'Thank you for rescuing surplus food! You prevented ' || co2_amt || 'kg CO2.',
            'order_completed',
            NEW.id
        );
    END IF;

    -- Order cancelled / expired / rejected -> Return stock to listing & refund reward points
    IF OLD.status IN ('awaiting_payment', 'pending_verification', 'reserved') AND NEW.status IN ('cancelled', 'expired', 'rejected') THEN
        UPDATE public.listings 
        SET remaining_quantity = remaining_quantity + NEW.quantity,
            status = 'active',
            updated_at = NOW()
        WHERE id = NEW.listing_id;

        IF NEW.reward_points_used > 0 THEN
            UPDATE public.profiles
            SET eco_points = eco_points + NEW.reward_points_used
            WHERE id = NEW.student_id;

            DELETE FROM public.reward_redemptions WHERE order_id = NEW.id;
        END IF;

        IF NEW.status = 'expired' THEN
            INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
            VALUES (
                NEW.student_id,
                'student',
                'Order Expired',
                'Order #' || NEW.order_code || ' expired because payment was not completed in time.',
                'order_expired',
                NEW.id
            );
        ELSIF NEW.status = 'rejected' THEN
            INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
            VALUES (
                NEW.student_id,
                'student',
                'Payment Slip Rejected',
                'The merchant could not verify your receipt for Order #' || NEW.order_code || '. Stock has been restored.',
                'payment_rejected',
                NEW.id
            );
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_process_order_completion ON public.orders;
DROP TRIGGER IF EXISTS trg_process_order_status_change ON public.orders;
CREATE TRIGGER trg_process_order_status_change
    AFTER UPDATE ON public.orders
    FOR EACH ROW EXECUTE FUNCTION public.process_order_status_change();

-- 8.4 TRIGGER: Review Aggregation & Rating Rollup
CREATE OR REPLACE FUNCTION public.process_review_submission()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    avg_r NUMERIC(3, 2);
    tot_r INT;
BEGIN
    SELECT AVG(rating)::NUMERIC(3, 2), COUNT(*) INTO avg_r, tot_r 
    FROM public.reviews WHERE merchant_id = NEW.merchant_id;

    UPDATE public.merchants 
    SET rating = COALESCE(avg_r, 0.00), total_reviews = COALESCE(tot_r, 0)
    WHERE id = NEW.merchant_id;

    INSERT INTO public.notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
    VALUES (
        (SELECT owner_id FROM public.merchants WHERE id = NEW.merchant_id),
        'merchant',
        'New Customer Review Received',
        'A student rated your surplus meal ' || NEW.rating || ' stars.',
        'review_received',
        NEW.order_id
    );

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_process_review_submission ON public.reviews;
CREATE TRIGGER trg_process_review_submission
    AFTER INSERT ON public.reviews
    FOR EACH ROW EXECUTE FUNCTION public.process_review_submission();

-- ============================================================================
-- SECTION 9: ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_areas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_landmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reward_redemptions ENABLE ROW LEVEL SECURITY;

-- Profiles
DROP POLICY IF EXISTS "Public read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can read all profiles" ON public.profiles;
CREATE POLICY "Public read profiles" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Public insert profiles" ON public.profiles FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Public update profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Public update profiles" ON public.profiles FOR UPDATE TO authenticated, anon USING (true) WITH CHECK (true);

-- Merchants
DROP POLICY IF EXISTS "Public read merchants" ON public.merchants;
CREATE POLICY "Public read merchants" ON public.merchants FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert merchants" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can insert own record" ON public.merchants;
CREATE POLICY "Public insert merchants" ON public.merchants FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Public update merchants" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;
CREATE POLICY "Public update merchants" ON public.merchants FOR UPDATE TO authenticated, anon USING (true) WITH CHECK (true);

-- Listings
DROP POLICY IF EXISTS "Public read active listings" ON public.listings;
CREATE POLICY "Public read active listings" ON public.listings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert listings" ON public.listings;
DROP POLICY IF EXISTS "Merchants can manage own listings" ON public.listings;
CREATE POLICY "Public insert listings" ON public.listings FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Public update listings" ON public.listings;
CREATE POLICY "Public update listings" ON public.listings FOR UPDATE TO authenticated, anon USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public delete listings" ON public.listings;
CREATE POLICY "Public delete listings" ON public.listings FOR DELETE TO authenticated, anon USING (true);

-- Orders
DROP POLICY IF EXISTS "Public read orders" ON public.orders;
DROP POLICY IF EXISTS "Students and Merchants can read relevant orders" ON public.orders;
CREATE POLICY "Public read orders" ON public.orders FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert orders" ON public.orders;
DROP POLICY IF EXISTS "Students can create orders" ON public.orders;
CREATE POLICY "Public insert orders" ON public.orders FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Public update orders" ON public.orders;
DROP POLICY IF EXISTS "Students and Merchants can update relevant orders" ON public.orders;
CREATE POLICY "Public update orders" ON public.orders FOR UPDATE TO authenticated, anon USING (true) WITH CHECK (true);

-- Reviews
DROP POLICY IF EXISTS "Public read reviews" ON public.reviews;
CREATE POLICY "Public read reviews" ON public.reviews FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Students can insert reviews" ON public.reviews;
CREATE POLICY "Public insert reviews" ON public.reviews FOR INSERT TO authenticated, anon WITH CHECK (true);

-- Notifications
DROP POLICY IF EXISTS "Public read notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can read own notifications" ON public.notifications;
CREATE POLICY "Public read notifications" ON public.notifications FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can insert notifications" ON public.notifications;
CREATE POLICY "Public insert notifications" ON public.notifications FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Public update notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
CREATE POLICY "Public update notifications" ON public.notifications FOR UPDATE TO authenticated, anon USING (true) WITH CHECK (true);

-- Landmarks & Service Areas
DROP POLICY IF EXISTS "Public read service areas" ON public.service_areas;
CREATE POLICY "Public read service areas" ON public.service_areas FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read campus landmarks" ON public.campus_landmarks;
CREATE POLICY "Public read campus landmarks" ON public.campus_landmarks FOR SELECT USING (true);

-- Realtime publication
DO $$ BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.listings;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
ALTER TABLE public.notifications REPLICA IDENTITY FULL;
ALTER TABLE public.orders REPLICA IDENTITY FULL;
ALTER TABLE public.listings REPLICA IDENTITY FULL;

-- ============================================================================
-- SECTION 10: UTAR KAMPAR CAMPUS REFERENCE BOUNDARIES & LANDMARKS ONLY
-- (No hardcoded demo users, merchants, or listings - Clean Database)
-- ============================================================================

-- 10.1 UTAR Kampar Campus Polygon
INSERT INTO public.service_areas (name, center_latitude, center_longitude, polygon_coordinates, is_active)
VALUES (
    'UTAR Kampar Campus',
    4.336214,
    101.142111,
    '[
        {"latitude": 4.344500, "longitude": 101.135000},
        {"latitude": 4.344500, "longitude": 101.150000},
        {"latitude": 4.330000, "longitude": 101.150000},
        {"latitude": 4.327000, "longitude": 101.143000},
        {"latitude": 4.330000, "longitude": 101.135000}
    ]'::jsonb,
    TRUE
) ON CONFLICT DO NOTHING;

-- 10.2 UTAR Kampar Campus Landmarks
INSERT INTO public.campus_landmarks (name, category, latitude, longitude) VALUES
    ('Student Pavilion I (Cafeteria)', 'student_pavilion', 4.335800, 101.141200),
    ('Student Pavilion II (Cafeteria)', 'student_pavilion', 4.337500, 101.143800),
    ('Block N - FICT', 'academic_block', 4.336500, 101.140200),
    ('Block K - FEGT', 'academic_block', 4.335200, 101.139500),
    ('Block D - FBF', 'academic_block', 4.337800, 101.142000)
ON CONFLICT DO NOTHING;

