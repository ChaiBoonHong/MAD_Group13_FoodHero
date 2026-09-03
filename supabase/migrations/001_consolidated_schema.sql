-- ============================================================================
-- FOODHERO CONSOLIDATED DATABASE SCHEMA (UTAR KAMPAR CAMPUS)
-- Complete, Clean, Production-Grade Architecture for Supabase (PostgreSQL)
-- 
-- Single source of truth. Idempotent execution.
-- Run in Supabase SQL Editor:
-- https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new
-- ============================================================================

-- 1. EXTENSIONS & CUSTOM TYPES
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('student', 'merchant');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE listing_status AS ENUM ('active', 'sold_out', 'expired', 'draft');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE order_status AS ENUM ('reserved', 'completed', 'cancelled', 'expired');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE image_source_type AS ENUM ('storage', 'external_url', 'none');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 2. DOMAIN: IDENTITY & ACCESS MANAGEMENT
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

DO $$ BEGIN
    ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;
    ALTER TABLE public.profiles
        ADD CONSTRAINT profiles_id_fkey
        FOREIGN KEY (id) REFERENCES auth.users(id)
        ON DELETE CASCADE;
EXCEPTION WHEN OTHERS THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.student_allowlist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    student_id TEXT NOT NULL,
    faculty TEXT NOT NULL,
    full_name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS public.merchant_allowlist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    business_name TEXT NOT NULL,
    campus_location TEXT NOT NULL
);

-- 3. DOMAIN: CAMPUS GEOLOCATION & MAPPING
CREATE TABLE IF NOT EXISTS public.service_areas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    center_latitude DOUBLE PRECISION NOT NULL DEFAULT 4.336214,
    center_longitude DOUBLE PRECISION NOT NULL DEFAULT 101.142111,
    polygon_coordinates JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.campus_landmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.user_locations (
    student_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_inside_campus BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. DOMAIN: MERCHANTS & LISTINGS
CREATE TABLE IF NOT EXISTS public.merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    business_name TEXT NOT NULL,
    campus_location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    closing_time TEXT NOT NULL DEFAULT '18:00',
    rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
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
    pickup_start TEXT NOT NULL,
    pickup_end TEXT NOT NULL,
    pickup_location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    co2_kg_per_item NUMERIC(5, 2) NOT NULL DEFAULT 1.20,
    status listing_status NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. DOMAIN: ORDERS & REWARDS
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
    status order_status NOT NULL DEFAULT 'reserved',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.reward_redemptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    points_deducted INT NOT NULL CHECK (points_deducted > 0),
    discount_amount NUMERIC(10, 2) NOT NULL CHECK (discount_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. DOMAIN: REVIEWS & NOTIFICATIONS
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

-- 7. PERFORMANCE INDEXES
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);
CREATE INDEX IF NOT EXISTS idx_merchants_owner ON public.merchants(owner_id);
CREATE INDEX IF NOT EXISTS idx_listings_merchant_status ON public.listings(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_listings_status ON public.listings(status);
CREATE INDEX IF NOT EXISTS idx_orders_student ON public.orders(student_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON public.orders(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_code ON public.orders(order_code);
CREATE INDEX IF NOT EXISTS idx_reviews_merchant ON public.reviews(merchant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications(recipient_id, is_read);

-- 8. TRIGGERS
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
        100, 5, 27.50, 6.0
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        full_name = COALESCE(EXCLUDED.full_name, profiles.full_name),
        updated_at = NOW();

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

CREATE OR REPLACE FUNCTION public.process_order_completion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    listing_rec RECORD;
    earned_pts INT;
    saved_amt NUMERIC(10, 2);
    co2_amt NUMERIC(10, 2);
BEGIN
    IF OLD.status = 'reserved' AND NEW.status = 'completed' THEN
        SELECT * INTO listing_rec FROM public.listings WHERE id = NEW.listing_id;
        earned_pts := NEW.quantity * 10;
        saved_amt := (NEW.total_original_price - NEW.final_paid_price);
        co2_amt := (listing_rec.co2_kg_per_item * NEW.quantity);

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
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_process_order_completion ON public.orders;
CREATE TRIGGER trg_process_order_completion
    AFTER UPDATE ON public.orders
    FOR EACH ROW EXECUTE FUNCTION public.process_order_completion();

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
    SET rating = avg_r, total_reviews = tot_r 
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

-- 9. ROW LEVEL SECURITY
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

DROP POLICY IF EXISTS "Users can read all profiles" ON public.profiles;
CREATE POLICY "Users can read all profiles" ON public.profiles FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile" ON public.profiles FOR INSERT TO authenticated, anon WITH CHECK (true);
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

DROP POLICY IF EXISTS "Public read merchants" ON public.merchants;
CREATE POLICY "Public read merchants" ON public.merchants FOR SELECT USING (true);
DROP POLICY IF EXISTS "Merchants can insert own record" ON public.merchants;
CREATE POLICY "Merchants can insert own record" ON public.merchants FOR INSERT TO authenticated, anon WITH CHECK (true);
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;
CREATE POLICY "Merchants can update own record" ON public.merchants FOR UPDATE USING (auth.uid() = owner_id);

DROP POLICY IF EXISTS "Public read active listings" ON public.listings;
CREATE POLICY "Public read active listings" ON public.listings FOR SELECT USING (true);
DROP POLICY IF EXISTS "Merchants can manage own listings" ON public.listings;
CREATE POLICY "Merchants can manage own listings" ON public.listings FOR ALL TO authenticated, anon USING (true);

DROP POLICY IF EXISTS "Students and Merchants can read relevant orders" ON public.orders;
CREATE POLICY "Students and Merchants can read relevant orders" ON public.orders FOR SELECT USING (true);
DROP POLICY IF EXISTS "Students can create orders" ON public.orders;
CREATE POLICY "Students can create orders" ON public.orders FOR INSERT TO authenticated, anon WITH CHECK (true);
DROP POLICY IF EXISTS "Students and Merchants can update relevant orders" ON public.orders;
CREATE POLICY "Students and Merchants can update relevant orders" ON public.orders FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Public read reviews" ON public.reviews;
CREATE POLICY "Public read reviews" ON public.reviews FOR SELECT USING (true);
DROP POLICY IF EXISTS "Students can insert reviews" ON public.reviews;
CREATE POLICY "Students can insert reviews" ON public.reviews FOR INSERT TO authenticated, anon WITH CHECK (true);

DROP POLICY IF EXISTS "Users can read own notifications" ON public.notifications;
CREATE POLICY "Users can read own notifications" ON public.notifications FOR SELECT USING (true);
DROP POLICY IF EXISTS "Users can insert notifications" ON public.notifications;
CREATE POLICY "Users can insert notifications" ON public.notifications FOR INSERT TO authenticated, anon WITH CHECK (true);
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
CREATE POLICY "Users can update own notifications" ON public.notifications FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Public read service areas" ON public.service_areas;
CREATE POLICY "Public read service areas" ON public.service_areas FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public read campus landmarks" ON public.campus_landmarks;
CREATE POLICY "Public read campus landmarks" ON public.campus_landmarks FOR SELECT USING (true);

DO $$ BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
ALTER TABLE public.notifications REPLICA IDENTITY FULL;

-- 10. SEED DATA (UTAR KAMPAR)
UPDATE auth.users
SET
    confirmation_token = COALESCE(confirmation_token, ''),
    recovery_token = COALESCE(recovery_token, ''),
    email_change_token_new = COALESCE(email_change_token_new, ''),
    email_change = COALESCE(email_change, ''),
    email_change_token_current = COALESCE(email_change_token_current, ''),
    phone = CASE WHEN phone = '' THEN NULL ELSE phone END,
    phone_change = COALESCE(phone_change, ''),
    phone_change_token = COALESCE(phone_change_token, ''),
    reauthentication_token = COALESCE(reauthentication_token, '');

DELETE FROM auth.identities 
WHERE user_id IN (SELECT id FROM auth.users WHERE email IN ('student@foodhero.my', 'merchant@foodhero.my'));

DELETE FROM auth.users 
WHERE email IN ('student@foodhero.my', 'merchant@foodhero.my');

DELETE FROM public.profiles 
WHERE email IN ('student@foodhero.my', 'merchant@foodhero.my');

INSERT INTO auth.users (
    id, instance_id, aud, role, email, encrypted_password, email_confirmed_at,
    confirmation_token, recovery_token, email_change_token_new, email_change,
    email_change_token_current, phone, phone_change, phone_change_token,
    reauthentication_token, raw_app_meta_data, raw_user_meta_data, is_super_admin,
    created_at, updated_at
)
VALUES
    (
        'b0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'authenticated', 'authenticated', 'student@foodhero.my',
        crypt('FoodHero123!', gen_salt('bf')),
        NOW(), '', '', '', '', '', NULL, '', '', '',
        '{"provider": "email", "providers": ["email"]}'::jsonb,
        '{"role": "student", "full_name": "Chai Boon Hong (Student)", "student_id": "22ACB01234", "faculty": "FICT"}'::jsonb,
        FALSE, NOW(), NOW()
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000000',
        'authenticated', 'authenticated', 'merchant@foodhero.my',
        crypt('FoodHero123!', gen_salt('bf')),
        NOW(), '', '', '', '', '', NULL, '', '', '',
        '{"provider": "email", "providers": ["email"]}'::jsonb,
        '{"role": "merchant", "full_name": "Grand Green Cafe (Merchant)"}'::jsonb,
        FALSE, NOW(), NOW()
    );

INSERT INTO auth.identities (
    id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
)
VALUES
    (
        'b0000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000002',
        jsonb_build_object('sub', 'b0000000-0000-0000-0000-000000000002', 'email', 'student@foodhero.my', 'email_verified', true),
        'email', 'student@foodhero.my', NOW(), NOW(), NOW()
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        jsonb_build_object('sub', 'a0000000-0000-0000-0000-000000000001', 'email', 'merchant@foodhero.my', 'email_verified', true),
        'email', 'merchant@foodhero.my', NOW(), NOW(), NOW()
    );

INSERT INTO public.profiles (
    id, email, full_name, role, student_id, faculty, eco_points, meals_rescued, money_saved, co2_prevented
)
VALUES 
    ('b0000000-0000-0000-0000-000000000002', 'student@foodhero.my', 'Chai Boon Hong (Student)', 'student', '22ACB01234', 'FICT', 120, 7, 38.50, 8.4),
    ('a0000000-0000-0000-0000-000000000001', 'merchant@foodhero.my', 'Grand Green Cafe (Merchant)', 'merchant', NULL, NULL, 0, 0, 0.00, 0.0)
ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name, role = EXCLUDED.role;

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

INSERT INTO public.campus_landmarks (name, category, latitude, longitude) VALUES
    ('East Gate (Main Entrance)', 'entrance', 4.338500, 101.146500),
    ('West Gate (Hostel / Sport Complex Entrance)', 'entrance', 4.332800, 101.137200),
    ('North Gate', 'entrance', 4.343200, 101.141500),
    ('Student Pavilion I (Cafeteria)', 'student_pavilion', 4.335800, 101.141200),
    ('Student Pavilion II (Cafeteria)', 'student_pavilion', 4.337500, 101.143800),
    ('Block A - Heritage Hall', 'landmark', 4.339200, 101.144500),
    ('Dewan Tun Dr Ling Liong Sik', 'landmark', 4.338800, 101.143500),
    ('Block N - FICT', 'academic_block', 4.336500, 101.140200),
    ('Block K - FEGT', 'academic_block', 4.335200, 101.139500),
    ('Block D - FBF', 'academic_block', 4.337800, 101.142000),
    ('UTAR Kampar Library', 'academic_block', 4.338200, 101.144000),
    ('Sports Complex & Gymnasium', 'landmark', 4.333500, 101.138000)
ON CONFLICT DO NOTHING;

INSERT INTO public.merchants (id, owner_id, business_name, campus_location, latitude, longitude, closing_time, rating, total_reviews)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Grand Green Cafe', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200, '18:00', 4.9, 128),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Kampar Campus Bakery', 'Student Pavilion II, Ground Floor', 4.337500, 101.143800, '19:30', 4.8, 94),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'ZUS Coffee UTAR Kampar', 'Student Pavilion I, Stall 8', 4.336200, 101.141500, '20:00', 5.0, 210)
ON CONFLICT (id) DO UPDATE SET business_name = EXCLUDED.business_name, campus_location = EXCLUDED.campus_location;

INSERT INTO public.listings (
    id, merchant_id, title, description, category, original_price, discounted_price, total_quantity, remaining_quantity,
    pickup_start, pickup_end, pickup_location, latitude, longitude, co2_kg_per_item, status
)
VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Surplus Bento Mystery Bag', 'Chef-curated surplus daily bento set. Fresh protein, multigrain rice, and organic greens.', 'Meals', 12.00, 5.50, 6, 4, '16:30', '18:00', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200, 1.20, 'active'),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Artisan Pastry & Croissant Box', 'Freshly baked butter croissants, pain au chocolat, and Danish pastries.', 'Bakery', 15.00, 6.00, 5, 3, '17:00', '19:30', 'Student Pavilion II, Ground Floor', 4.337500, 101.143800, 0.85, 'active'),
    ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'Eco Roasted Chicken Rice Bowl', 'Juicy roasted chicken thigh over fragrant chicken rice with house chili.', 'Rice & Noodles', 9.50, 4.50, 8, 5, '16:30', '18:00', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200, 1.10, 'active'),
    ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003', 'Fresh Brew & Muffin Saver Set', 'Handcrafted Americano or Latte paired with a freshly baked blueberry crumb muffin.', 'Beverages', 11.00, 5.00, 4, 2, '17:30', '20:00', 'Student Pavilion I, Stall 8', 4.336200, 101.141500, 0.65, 'active')
ON CONFLICT (id) DO UPDATE SET remaining_quantity = EXCLUDED.remaining_quantity, status = EXCLUDED.status;

INSERT INTO public.student_allowlist (email, student_id, faculty, full_name) VALUES
    ('student@foodhero.my', '22ACB01234', 'FICT', 'Chai Boon Hong (Student)'),
    ('student.demo@utar.edu.my', '22ACB05678', 'FICT', 'Demo Student UTAR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO public.merchant_allowlist (email, business_name, campus_location) VALUES
    ('merchant@foodhero.my', 'Grand Green Cafe', 'Student Pavilion I, Cafeteria Stn 3'),
    ('merchant.demo@utar.edu.my', 'Kampar Campus Bakery', 'Student Pavilion II, Ground Floor')
ON CONFLICT (email) DO NOTHING;
