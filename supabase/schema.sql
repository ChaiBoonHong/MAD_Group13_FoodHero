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
    name TEXT NOT NULL UNIQUE,
    center_latitude DOUBLE PRECISION NOT NULL DEFAULT 4.336214,
    center_longitude DOUBLE PRECISION NOT NULL DEFAULT 101.142111,
    polygon_coordinates JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$ BEGIN
    DELETE FROM public.service_areas a
    USING public.service_areas b
    WHERE a.ctid < b.ctid AND a.name = b.name;

    ALTER TABLE public.service_areas
        ADD CONSTRAINT service_areas_name_unique UNIQUE (name);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- 3.2 CAMPUS LANDMARKS (Approved Landmark Hotspots)
CREATE TABLE IF NOT EXISTS public.campus_landmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL, -- 'entrance', 'academic_block', 'student_pavilion', 'landmark'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

DO $$ BEGIN
    DELETE FROM public.campus_landmarks a
    USING public.campus_landmarks b
    WHERE a.ctid < b.ctid AND a.name = b.name;

    ALTER TABLE public.campus_landmarks
        ADD CONSTRAINT campus_landmarks_name_key UNIQUE (name);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

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
    DELETE FROM public.merchants a
    USING public.merchants b
    WHERE a.ctid < b.ctid AND a.owner_id = b.owner_id;

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

-- Profiles: authenticated users may resolve display names, but may only write their own row.
DROP POLICY IF EXISTS "Public read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can read all profiles" ON public.profiles;
DROP POLICY IF EXISTS "Authenticated read profiles" ON public.profiles;
CREATE POLICY "Authenticated read profiles" ON public.profiles FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "Public insert profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile" ON public.profiles FOR INSERT TO authenticated WITH CHECK (id = auth.uid());
DROP POLICY IF EXISTS "Public update profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE TO authenticated USING (id = auth.uid()) WITH CHECK (id = auth.uid());

-- Merchants
DROP POLICY IF EXISTS "Public read merchants" ON public.merchants;
CREATE POLICY "Public read merchants" ON public.merchants FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert merchants" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can insert own record" ON public.merchants;
CREATE POLICY "Merchants can insert own record" ON public.merchants FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());

DROP POLICY IF EXISTS "Public update merchants" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;
CREATE POLICY "Merchants can update own record" ON public.merchants FOR UPDATE TO authenticated USING (owner_id = auth.uid()) WITH CHECK (owner_id = auth.uid());

DROP POLICY IF EXISTS "Public delete merchants" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can delete own record" ON public.merchants;
CREATE POLICY "Merchants can delete own record" ON public.merchants FOR DELETE TO authenticated USING (owner_id = auth.uid());

-- Listings
DROP POLICY IF EXISTS "Public read active listings" ON public.listings;
CREATE POLICY "Public read active listings" ON public.listings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert listings" ON public.listings;
DROP POLICY IF EXISTS "Merchants can manage own listings" ON public.listings;
DROP POLICY IF EXISTS "Merchants can insert own listings" ON public.listings;
DROP POLICY IF EXISTS "Approved merchants insert own listings" ON public.listings;
CREATE POLICY "Merchants can insert own listings" ON public.listings FOR INSERT TO authenticated WITH CHECK (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
);

DROP POLICY IF EXISTS "Public update listings" ON public.listings;
DROP POLICY IF EXISTS "Merchants can update own listings" ON public.listings;
DROP POLICY IF EXISTS "Approved merchants update own listings" ON public.listings;
CREATE POLICY "Merchants can update own listings" ON public.listings FOR UPDATE TO authenticated USING (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
) WITH CHECK (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
);

DROP POLICY IF EXISTS "Public delete listings" ON public.listings;
DROP POLICY IF EXISTS "Merchants can delete own listings" ON public.listings;
CREATE POLICY "Merchants can delete own listings" ON public.listings FOR DELETE TO authenticated USING (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
);

-- Orders
DROP POLICY IF EXISTS "Public read orders" ON public.orders;
DROP POLICY IF EXISTS "Students and Merchants can read relevant orders" ON public.orders;
DROP POLICY IF EXISTS "Students and merchants read relevant orders" ON public.orders;
CREATE POLICY "Students and merchants read relevant orders" ON public.orders FOR SELECT TO authenticated USING (
    student_id = auth.uid() OR EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
);

DROP POLICY IF EXISTS "Public insert orders" ON public.orders;
DROP POLICY IF EXISTS "Students can create orders" ON public.orders;
DROP POLICY IF EXISTS "Students create own orders" ON public.orders;
CREATE POLICY "Students create own orders" ON public.orders FOR INSERT TO authenticated WITH CHECK (student_id = auth.uid());

DROP POLICY IF EXISTS "Public update orders" ON public.orders;
DROP POLICY IF EXISTS "Students and Merchants can update relevant orders" ON public.orders;
DROP POLICY IF EXISTS "Students and merchants update relevant orders" ON public.orders;
CREATE POLICY "Students and merchants update relevant orders" ON public.orders FOR UPDATE TO authenticated USING (
    student_id = auth.uid() OR EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
) WITH CHECK (
    student_id = auth.uid() OR EXISTS (SELECT 1 FROM public.merchants m WHERE m.id = merchant_id AND m.owner_id = auth.uid())
);

-- Reviews
DROP POLICY IF EXISTS "Public read reviews" ON public.reviews;
CREATE POLICY "Public read reviews" ON public.reviews FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Students can insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Students can insert review for own order" ON public.reviews;
DROP POLICY IF EXISTS "Students insert own reviews" ON public.reviews;
CREATE POLICY "Students insert own reviews" ON public.reviews FOR INSERT TO authenticated WITH CHECK (
    student_id = (SELECT auth.uid()) AND EXISTS (
      SELECT 1 FROM public.orders o WHERE o.id=order_id AND o.student_id=(SELECT auth.uid())
        AND o.status='completed' AND o.merchant_id=reviews.merchant_id AND o.listing_id=reviews.listing_id));

CREATE INDEX IF NOT EXISTS idx_notifications_related_listing ON public.notifications(related_listing_id);
CREATE INDEX IF NOT EXISTS idx_notifications_related_order ON public.notifications(related_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_listing ON public.orders(listing_id);
CREATE INDEX IF NOT EXISTS idx_reviews_listing ON public.reviews(listing_id);
CREATE INDEX IF NOT EXISTS idx_reviews_student ON public.reviews(student_id);
CREATE INDEX IF NOT EXISTS idx_reward_redemptions_order ON public.reward_redemptions(order_id);
CREATE INDEX IF NOT EXISTS idx_reward_redemptions_student ON public.reward_redemptions(student_id);

-- Notifications
DROP POLICY IF EXISTS "Public read notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can read own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users read own notifications" ON public.notifications;
CREATE POLICY "Users read own notifications" ON public.notifications FOR SELECT TO authenticated USING (recipient_id = auth.uid());

DROP POLICY IF EXISTS "Public insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can insert notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users insert own notifications" ON public.notifications;
CREATE POLICY "Users insert own notifications" ON public.notifications FOR INSERT TO authenticated WITH CHECK (recipient_id = auth.uid());

DROP POLICY IF EXISTS "Public update notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
DROP POLICY IF EXISTS "Users update own notifications" ON public.notifications;
CREATE POLICY "Users update own notifications" ON public.notifications FOR UPDATE TO authenticated USING (recipient_id = auth.uid()) WITH CHECK (recipient_id = auth.uid());

-- Landmarks & Service Areas
DROP POLICY IF EXISTS "Public read service areas" ON public.service_areas;
CREATE POLICY "Public read service areas" ON public.service_areas FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read campus landmarks" ON public.campus_landmarks;
CREATE POLICY "Public read campus landmarks" ON public.campus_landmarks FOR SELECT USING (true);

-- Storage buckets and ownership policies. Listing images are public catalogue
-- assets; payment receipts require an authenticated session to read.
INSERT INTO storage.buckets (id, name, public)
VALUES ('listing-images', 'listing-images', TRUE),
       ('payment-receipts', 'payment-receipts', FALSE)
ON CONFLICT (id) DO UPDATE SET public = EXCLUDED.public;

DROP POLICY IF EXISTS "Public read listing images" ON storage.objects;
CREATE POLICY "Public read listing images" ON storage.objects FOR SELECT
USING (bucket_id = 'listing-images');
DROP POLICY IF EXISTS "Users upload own listing images" ON storage.objects;
CREATE POLICY "Users upload own listing images" ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'listing-images' AND (storage.foldername(name))[1] = auth.uid()::text);
DROP POLICY IF EXISTS "Users update own listing images" ON storage.objects;
CREATE POLICY "Users update own listing images" ON storage.objects FOR UPDATE TO authenticated
USING (bucket_id = 'listing-images' AND owner_id = auth.uid()::text)
WITH CHECK (bucket_id = 'listing-images' AND owner_id = auth.uid()::text);
DROP POLICY IF EXISTS "Users delete own listing images" ON storage.objects;
CREATE POLICY "Users delete own listing images" ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'listing-images' AND owner_id = auth.uid()::text);

DROP POLICY IF EXISTS "Authenticated read payment receipts" ON storage.objects;
DROP POLICY IF EXISTS "Order participants read payment receipts" ON storage.objects;
CREATE POLICY "Authenticated read payment receipts" ON storage.objects FOR SELECT TO authenticated
USING (bucket_id = 'payment-receipts');
DROP POLICY IF EXISTS "Students upload own payment receipts" ON storage.objects;
CREATE POLICY "Students upload own payment receipts" ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'payment-receipts' AND (storage.foldername(name))[1] = auth.uid()::text);
DROP POLICY IF EXISTS "Students delete own payment receipts" ON storage.objects;
CREATE POLICY "Students delete own payment receipts" ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'payment-receipts' AND owner_id = auth.uid()::text);

-- Realtime publication
DO $$ BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.orders;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.listings;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
ALTER TABLE public.notifications REPLICA IDENTITY FULL;

-- ============================================================================
-- SECTION 11: TRANSACTIONAL SPRINT HARDENING
-- FH-101 .. FH-105. These declarations intentionally come last so they also
-- harden projects that previously ran an older revision of this master file.
-- ============================================================================

DO $$ BEGIN
    CREATE TYPE institution_affiliation AS ENUM ('student', 'staff', 'member');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE merchant_approval_status AS ENUM ('pending', 'approved', 'rejected', 'suspended');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS public.institution_email_domains (
    domain TEXT PRIMARY KEY CHECK (domain = lower(trim(domain)) AND domain !~ '@'),
    institution_code TEXT NOT NULL,
    institution_name TEXT NOT NULL,
    affiliation_type institution_affiliation NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    verification_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed data is configuration, not an authorization shortcut. Add or deactivate
-- rows through the Dashboard as institutional eligibility changes.
INSERT INTO public.institution_email_domains
    (domain, institution_code, institution_name, affiliation_type, verification_note)
VALUES
    ('1utar.my', 'UTAR', 'Universiti Tunku Abdul Rahman', 'student', 'UTAR student email'),
    ('utar.edu.my', 'UTAR', 'Universiti Tunku Abdul Rahman', 'staff', 'UTAR staff email'),
    ('utp.edu.my', 'UTP', 'Universiti Teknologi PETRONAS', 'member', 'Shared institutional domain'),
    ('nottingham.edu.my', 'UNM', 'University of Nottingham Malaysia', 'member', 'Shared institutional domain')
ON CONFLICT (domain) DO UPDATE SET
    institution_code = EXCLUDED.institution_code,
    institution_name = EXCLUDED.institution_name,
    affiliation_type = EXCLUDED.affiliation_type,
    verification_note = EXCLUDED.verification_note;

ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS institution_code TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS institution_name TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS institution_affiliation institution_affiliation;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS status merchant_approval_status NOT NULL DEFAULT 'pending';
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS stall_description TEXT;
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS contact_phone TEXT;
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS duitnow_display_name TEXT;
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS duitnow_qr_path TEXT;
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS service_area_id UUID REFERENCES public.service_areas(id);
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;

ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS receipt_storage_path TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS receipt_submitted_at TIMESTAMPTZ;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_verified_at TIMESTAMPTZ;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS pickup_expires_at TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_pickup_token_unique ON public.orders(pickup_token);

-- Exact domain lookup used by the client before sign-up. Final authority remains
-- handle_new_user() and the verified auth.users record.
CREATE OR REPLACE FUNCTION public.lookup_institution_domain(p_email TEXT)
RETURNS TABLE (institution_code TEXT, institution_name TEXT, affiliation_type institution_affiliation)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
    SELECT d.institution_code, d.institution_name, d.affiliation_type
    FROM public.institution_email_domains d
    WHERE d.domain = lower(split_part(trim(p_email), '@', 2))
      AND length(trim(p_email)) - length(replace(trim(p_email), '@', '')) = 1
      AND d.is_active;
$$;

-- Auth metadata records only the requested workspace. Institution and
-- affiliation always come from the server registry, never from the Android app.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    requested_role public.user_role := 'student';
    domain_row public.institution_email_domains%ROWTYPE;
    user_full_name TEXT;
BEGIN
    user_full_name := COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'full_name'), ''), split_part(NEW.email, '@', 1));
    IF NEW.raw_user_meta_data->>'requested_role' = 'merchant' THEN
        requested_role := 'merchant';
    END IF;

    SELECT * INTO domain_row FROM public.institution_email_domains
    WHERE domain = lower(split_part(trim(NEW.email), '@', 2)) AND is_active;
    IF requested_role = 'student' AND NOT FOUND THEN
        RAISE EXCEPTION 'Unsupported institutional email domain';
    END IF;

    INSERT INTO public.profiles
        (id, email, role, full_name, student_id, faculty, institution_code,
         institution_name, institution_affiliation, email_verified_at)
    VALUES
        (NEW.id, lower(NEW.email), requested_role, user_full_name,
         CASE WHEN requested_role = 'student' THEN NEW.raw_user_meta_data->>'student_id' END,
         CASE WHEN requested_role = 'student' THEN NEW.raw_user_meta_data->>'faculty' END,
         domain_row.institution_code, domain_row.institution_name,
         domain_row.affiliation_type, NEW.email_confirmed_at)
    ON CONFLICT (id) DO UPDATE SET
        email = EXCLUDED.email,
        full_name = EXCLUDED.full_name,
        email_verified_at = EXCLUDED.email_verified_at,
        updated_at = NOW();

    IF requested_role = 'merchant' THEN
        INSERT INTO public.merchants
            (owner_id, business_name, campus_location, latitude, longitude, status)
        VALUES
            (NEW.id,
             COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'business_name'), ''), user_full_name),
             COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'campus_location'), ''), 'Application incomplete'),
             4.336214, 101.142111, 'pending')
        ON CONFLICT (owner_id) DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.sync_verified_email()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF NEW.email_confirmed_at IS DISTINCT FROM OLD.email_confirmed_at THEN
        UPDATE public.profiles SET email_verified_at = NEW.email_confirmed_at, updated_at = NOW()
        WHERE id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS on_auth_user_email_verified ON auth.users;
CREATE TRIGGER on_auth_user_email_verified AFTER UPDATE OF email_confirmed_at ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.sync_verified_email();

-- Retire the insert trigger: reservations must go through reserve_listing so
-- identifiers, prices, expiry, merchant and stock all come from locked rows.
DROP TRIGGER IF EXISTS trg_process_order_reservation ON public.orders;

CREATE OR REPLACE FUNCTION public.reserve_listing(p_listing_id UUID, p_quantity INT, p_use_reward_points BOOLEAN DEFAULT FALSE)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    l public.listings%ROWTYPE;
    p public.profiles%ROWTYPE;
    result public.orders%ROWTYPE;
    points_used INT := 0;
    reward_discount NUMERIC(10,2) := 0;
    token TEXT := encode(gen_random_bytes(24), 'hex');
BEGIN
    IF auth.uid() IS NULL OR p_quantity IS NULL OR p_quantity <= 0 THEN RAISE EXCEPTION 'Invalid reservation'; END IF;
    SELECT * INTO p FROM public.profiles WHERE id = auth.uid() FOR UPDATE;
    IF NOT FOUND OR p.role <> 'student' OR p.email_verified_at IS NULL THEN RAISE EXCEPTION 'Verified student account required'; END IF;
    SELECT * INTO l FROM public.listings WHERE id = p_listing_id FOR UPDATE;
    IF NOT FOUND OR l.status <> 'active' OR l.remaining_quantity < p_quantity THEN RAISE EXCEPTION 'Listing unavailable or insufficient stock'; END IF;
    IF p_use_reward_points AND p.eco_points >= 100 THEN
        points_used := 100;
        reward_discount := least(5.00, l.discounted_price * p_quantity);
    END IF;
    INSERT INTO public.orders
        (order_code, student_id, listing_id, merchant_id, quantity, total_original_price,
         total_discounted_price, reward_points_used, reward_discount_amount, final_paid_price,
         pickup_start, pickup_end, pickup_token, status, payment_expires_at, payment_reference)
    VALUES
        ('FH-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8)), auth.uid(), l.id, l.merchant_id,
         p_quantity, l.original_price*p_quantity, l.discounted_price*p_quantity, points_used, reward_discount,
         greatest(0, l.discounted_price*p_quantity-reward_discount), l.pickup_start, l.pickup_end, token,
         'awaiting_payment', (extract(epoch FROM (clock_timestamp()+interval '10 minutes'))*1000)::BIGINT,
         'FH-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 10)))
    RETURNING * INTO result;
    UPDATE public.listings SET remaining_quantity = remaining_quantity-p_quantity,
        status = CASE WHEN remaining_quantity-p_quantity = 0 THEN 'sold_out' ELSE status END, updated_at=NOW()
    WHERE id=l.id;
    IF points_used > 0 THEN
        UPDATE public.profiles SET eco_points=eco_points-points_used WHERE id=auth.uid();
        INSERT INTO public.reward_redemptions(student_id,order_id,points_deducted,discount_amount)
        VALUES(auth.uid(),result.id,points_used,reward_discount);
    END IF;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION public.submit_payment_receipt(p_order_id UUID, p_storage_path TEXT)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF p_storage_path IS NULL OR p_storage_path !~ ('^' || auth.uid()::text || '/' || p_order_id::text || '/') THEN
        RAISE EXCEPTION 'Invalid receipt path';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM storage.objects WHERE bucket_id='payment-receipts' AND name=p_storage_path) THEN
        RAISE EXCEPTION 'Uploaded receipt was not found';
    END IF;
    UPDATE public.orders SET status='pending_verification', receipt_storage_path=p_storage_path,
        payment_receipt_url=p_storage_path, receipt_submitted_at=NOW()
    WHERE id=p_order_id AND student_id=auth.uid() AND status='awaiting_payment'
      AND payment_expires_at > (extract(epoch FROM clock_timestamp())*1000)::BIGINT
    RETURNING * INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Order is not awaiting payment or has expired'; END IF;
    INSERT INTO public.notifications(recipient_id,recipient_role,title,message,event_type,related_order_id)
    SELECT m.owner_id,'merchant','Payment receipt submitted','Order #'||result.order_code||' is ready for review.',
           'payment_submitted',result.id FROM public.merchants m WHERE m.id=result.merchant_id;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION public.decide_payment_receipt(p_order_id UUID, p_approved BOOLEAN, p_rejection_reason TEXT DEFAULT NULL)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF NOT p_approved AND length(trim(COALESCE(p_rejection_reason,''))) < 3 THEN RAISE EXCEPTION 'Rejection reason is required'; END IF;
    UPDATE public.orders o SET
        status=CASE WHEN p_approved THEN 'reserved'::order_status ELSE 'rejected'::order_status END,
        payment_verified_at=CASE WHEN p_approved THEN NOW() ELSE NULL END,
        rejection_reason=CASE WHEN p_approved THEN NULL ELSE trim(p_rejection_reason) END,
        pickup_expires_at=CASE WHEN p_approved THEN NOW()+interval '24 hours' ELSE NULL END
    WHERE o.id=p_order_id AND o.status='pending_verification'
      AND EXISTS (SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid() AND m.status='approved')
    RETURNING o.* INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Receipt already decided or merchant is not authorized'; END IF;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION public.expire_unpaid_order(p_order_id UUID)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    UPDATE public.orders SET status='expired'
    WHERE id=p_order_id AND student_id=auth.uid() AND status='awaiting_payment'
      AND payment_expires_at <= (extract(epoch FROM clock_timestamp())*1000)::BIGINT
    RETURNING * INTO result;
    IF NOT FOUND THEN
        SELECT * INTO result FROM public.orders WHERE id=p_order_id AND student_id=auth.uid();
    END IF;
    RETURN result;
END;
$$;

CREATE OR REPLACE FUNCTION public.complete_pickup(p_token TEXT)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF p_token IS NULL OR length(trim(p_token)) < 32 THEN RAISE EXCEPTION 'Malformed pickup token'; END IF;
    UPDATE public.orders o SET status='completed', completed_at=NOW()
    WHERE o.pickup_token=trim(p_token) AND o.status='reserved'
      AND (o.pickup_expires_at IS NULL OR o.pickup_expires_at > NOW())
      AND EXISTS (SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid() AND m.status='approved')
    RETURNING o.* INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Invalid, expired, used, or unauthorized pickup token'; END IF;
    RETURN result;
END;
$$;

-- Commerce writes are RPC-only. RLS still governs reads of returned rows.
DROP POLICY IF EXISTS "Students create own orders" ON public.orders;
DROP POLICY IF EXISTS "Students and merchants update relevant orders" ON public.orders;
REVOKE INSERT, UPDATE, DELETE ON public.orders FROM authenticated;
GRANT EXECUTE ON FUNCTION public.lookup_institution_domain(TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.reserve_listing(UUID,INT,BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_payment_receipt(UUID,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.decide_payment_receipt(UUID,BOOLEAN,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.expire_unpaid_order(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_pickup(TEXT) TO authenticated;

ALTER TABLE public.institution_email_domains ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Active institution domains are readable" ON public.institution_email_domains;
CREATE POLICY "Active institution domains are readable" ON public.institution_email_domains FOR SELECT
USING (is_active);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
REVOKE INSERT, UPDATE ON public.profiles FROM authenticated;
GRANT UPDATE (full_name, student_id, faculty, updated_at) ON public.profiles TO authenticated;

-- Approval state, QR path and service-area assignment are reviewer-controlled.
REVOKE UPDATE ON public.merchants FROM authenticated;
GRANT UPDATE (business_name, campus_location, latitude, longitude, closing_time,
              stall_description, contact_phone, duitnow_display_name, duitnow_qr_path, terms_accepted_at)
ON public.merchants TO authenticated;
DROP POLICY IF EXISTS "Merchants can insert own record" ON public.merchants;
DROP POLICY IF EXISTS "Merchants can delete own record" ON public.merchants;
REVOKE INSERT, DELETE ON public.merchants FROM authenticated;

-- Pending/rejected merchants may maintain their application, but only approved
-- merchants may publish listings or process commerce operations.
DROP POLICY IF EXISTS "Merchants can insert own listings" ON public.listings;
CREATE POLICY "Approved merchants insert own listings" ON public.listings FOR INSERT TO authenticated WITH CHECK (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id=merchant_id AND m.owner_id=auth.uid() AND m.status='approved'));
DROP POLICY IF EXISTS "Merchants can update own listings" ON public.listings;
CREATE POLICY "Approved merchants update own listings" ON public.listings FOR UPDATE TO authenticated USING (
    EXISTS (SELECT 1 FROM public.merchants m WHERE m.id=merchant_id AND m.owner_id=auth.uid() AND m.status='approved'))
WITH CHECK (EXISTS (SELECT 1 FROM public.merchants m WHERE m.id=merchant_id AND m.owner_id=auth.uid() AND m.status='approved'));

-- A receipt is readable only by its student or by the associated merchant.
DROP POLICY IF EXISTS "Authenticated read payment receipts" ON storage.objects;
CREATE POLICY "Order participants read payment receipts" ON storage.objects FOR SELECT TO authenticated USING (
    bucket_id='payment-receipts' AND (
      (storage.foldername(name))[1]=auth.uid()::text OR EXISTS (
        SELECT 1 FROM public.orders o JOIN public.merchants m ON m.id=o.merchant_id
        WHERE o.receipt_storage_path=name AND m.owner_id=auth.uid()))) ;

INSERT INTO storage.buckets (id, name, public)
VALUES ('merchant-payment-qrs', 'merchant-payment-qrs', FALSE)
ON CONFLICT (id) DO UPDATE SET public=FALSE;
DROP POLICY IF EXISTS "Merchants upload own DuitNow QR" ON storage.objects;
CREATE POLICY "Merchants upload own DuitNow QR" ON storage.objects FOR INSERT TO authenticated WITH CHECK (
    bucket_id='merchant-payment-qrs' AND (storage.foldername(name))[1]=auth.uid()::text);
DROP POLICY IF EXISTS "Order participants read DuitNow QR" ON storage.objects;
CREATE POLICY "Order participants read DuitNow QR" ON storage.objects FOR SELECT TO authenticated USING (
    bucket_id='merchant-payment-qrs' AND EXISTS (
      SELECT 1 FROM public.merchants m WHERE m.duitnow_qr_path=name AND
        (m.owner_id=auth.uid() OR EXISTS (SELECT 1 FROM public.orders o WHERE o.merchant_id=m.id AND o.student_id=auth.uid()))));
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
) ON CONFLICT (name) DO UPDATE
SET center_latitude = EXCLUDED.center_latitude,
    center_longitude = EXCLUDED.center_longitude,
    polygon_coordinates = EXCLUDED.polygon_coordinates,
    is_active = EXCLUDED.is_active;

-- 10.2 UTAR Kampar Campus Landmarks (Exact Google Maps Coordinates)
INSERT INTO public.campus_landmarks (name, category, latitude, longitude) VALUES
    ('Block C - Student Pavilion I', 'student_pavilion', 4.337243, 101.142379),
    ('Block K - Student Pavilion II', 'student_pavilion', 4.341959, 101.141229),
    ('Cafeteria D & E', 'student_pavilion', 4.338326, 101.144057),
    ('Tin Road UTAR Cafe', 'student_pavilion', 4.339827, 101.142947),
    ('Block N - FICT', 'academic_block', 4.338707, 101.136712),
    ('Block G - Faculty of Science', 'academic_block', 4.335900, 101.140200)
ON CONFLICT (name) DO UPDATE 
SET latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    category = EXCLUDED.category;

-- ============================================================================
-- SECTION 12: MULTI-CAMPUS AND MULTI-ROLE CONTRACT
-- ============================================================================

ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ready_for_pickup';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'payment_rejected';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'no_show';

CREATE TABLE IF NOT EXISTS public.institutions (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.campuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    institution_code TEXT NOT NULL REFERENCES public.institutions(code),
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    boundary_coordinates JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_main BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (institution_code, name)
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    role user_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS public.student_affiliations (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    institutional_email TEXT NOT NULL UNIQUE,
    institution_code TEXT NOT NULL REFERENCES public.institutions(code),
    campus_id UUID NOT NULL REFERENCES public.campuses(id),
    affiliation_type institution_affiliation NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (institutional_email = lower(trim(institutional_email)))
);

CREATE TABLE IF NOT EXISTS public.institution_verification_challenges (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    institutional_email TEXT NOT NULL,
    code_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_after TIMESTAMPTZ NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add columns before defining functions that reference them. Keeping these
-- statements idempotent allows this consolidated development schema to be
-- applied to both fresh and existing projects.
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_active_role user_role;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.listings ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.service_areas ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.campus_landmarks ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);

ALTER TABLE public.institutions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campuses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_affiliations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.institution_verification_challenges ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Active institutions are readable" ON public.institutions;
CREATE POLICY "Active institutions are readable" ON public.institutions FOR SELECT TO authenticated USING(is_active);
DROP POLICY IF EXISTS "Active campuses are readable" ON public.campuses;
CREATE POLICY "Active campuses are readable" ON public.campuses FOR SELECT TO authenticated USING(is_active);
DROP POLICY IF EXISTS "Users read own roles" ON public.user_roles;
CREATE POLICY "Users read own roles" ON public.user_roles FOR SELECT TO authenticated USING(user_id=auth.uid());
DROP POLICY IF EXISTS "Users read own affiliation" ON public.student_affiliations;
CREATE POLICY "Users read own affiliation" ON public.student_affiliations FOR SELECT TO authenticated USING(user_id=auth.uid());

CREATE OR REPLACE FUNCTION public.issue_institution_verification(p_user_id UUID,p_email TEXT,p_code TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE domain_row public.institution_email_domains%ROWTYPE;
BEGIN
    SELECT * INTO domain_row FROM public.institution_email_domains
      WHERE domain=lower(split_part(trim(p_email),'@',2)) AND is_active;
    IF NOT FOUND THEN RAISE EXCEPTION 'Unsupported institutional email domain'; END IF;
    IF EXISTS(SELECT 1 FROM public.institution_verification_challenges WHERE user_id=p_user_id AND resend_after>NOW()) THEN
        RAISE EXCEPTION 'Please wait before requesting another code';
    END IF;
    INSERT INTO public.institution_verification_challenges(user_id,institutional_email,code_hash,expires_at,resend_after,failed_attempts)
    VALUES(p_user_id,lower(trim(p_email)),crypt(p_code,gen_salt('bf')),NOW()+interval '10 minutes',NOW()+interval '60 seconds',0)
    ON CONFLICT(user_id) DO UPDATE SET institutional_email=EXCLUDED.institutional_email,code_hash=EXCLUDED.code_hash,
        expires_at=EXCLUDED.expires_at,resend_after=EXCLUDED.resend_after,failed_attempts=0,created_at=NOW();
END; $$;

-- Make access intent explicit for every RLS table.

DROP POLICY IF EXISTS "Block direct student allowlist access" ON public.student_allowlist;
CREATE POLICY "Block direct student allowlist access" ON public.student_allowlist
FOR ALL TO anon, authenticated USING (FALSE) WITH CHECK (FALSE);

DROP POLICY IF EXISTS "Block direct merchant allowlist access" ON public.merchant_allowlist;
CREATE POLICY "Block direct merchant allowlist access" ON public.merchant_allowlist
FOR ALL TO anon, authenticated USING (FALSE) WITH CHECK (FALSE);

DROP POLICY IF EXISTS "Students read own reward redemptions" ON public.reward_redemptions;
CREATE POLICY "Students read own reward redemptions" ON public.reward_redemptions
FOR SELECT TO authenticated USING (student_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "Users read own location" ON public.user_locations;
CREATE POLICY "Users read own location" ON public.user_locations FOR SELECT TO authenticated
USING (student_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "Users insert own location" ON public.user_locations;
CREATE POLICY "Users insert own location" ON public.user_locations FOR INSERT TO authenticated
WITH CHECK (student_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "Users update own location" ON public.user_locations;
CREATE POLICY "Users update own location" ON public.user_locations FOR UPDATE TO authenticated
USING (student_id = (SELECT auth.uid())) WITH CHECK (student_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "Users delete own location" ON public.user_locations;
CREATE POLICY "Users delete own location" ON public.user_locations FOR DELETE TO authenticated
USING (student_id = (SELECT auth.uid()));
REVOKE ALL ON FUNCTION public.issue_institution_verification(UUID,TEXT,TEXT) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.issue_institution_verification(UUID,TEXT,TEXT) TO service_role;

DROP FUNCTION IF EXISTS public.confirm_institution_verification(TEXT,TEXT);
CREATE OR REPLACE FUNCTION public.confirm_institution_verification(p_email TEXT,p_code TEXT,p_student_id TEXT,p_faculty TEXT)
RETURNS public.profiles LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE challenge public.institution_verification_challenges%ROWTYPE; domain_row public.institution_email_domains%ROWTYPE;
        campus UUID; result public.profiles%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF length(trim(coalesce(p_student_id,'')))<3 OR length(trim(coalesce(p_faculty,'')))<2 THEN
        RAISE EXCEPTION 'Student ID and faculty are required';
    END IF;
    SELECT * INTO challenge FROM public.institution_verification_challenges WHERE user_id=auth.uid() FOR UPDATE;
    IF NOT FOUND OR challenge.institutional_email<>lower(trim(p_email)) OR challenge.expires_at<=NOW() OR challenge.failed_attempts>=5 THEN
        RAISE EXCEPTION 'Verification code is invalid or expired';
    END IF;
    IF crypt(p_code,challenge.code_hash)<>challenge.code_hash THEN
        UPDATE public.institution_verification_challenges SET failed_attempts=failed_attempts+1 WHERE user_id=auth.uid();
        RAISE EXCEPTION 'Verification code is invalid or expired';
    END IF;
    SELECT * INTO domain_row FROM public.institution_email_domains WHERE domain=lower(split_part(trim(p_email),'@',2)) AND is_active;
    SELECT id INTO campus FROM public.campuses WHERE institution_code=domain_row.institution_code AND is_main AND is_active LIMIT 1;
    IF campus IS NULL THEN RAISE EXCEPTION 'Institution campus is not configured'; END IF;
    INSERT INTO public.user_roles(user_id,role) VALUES(auth.uid(),'student') ON CONFLICT DO NOTHING;
    INSERT INTO public.student_affiliations(user_id,institutional_email,institution_code,campus_id,affiliation_type,verified_at)
    VALUES(auth.uid(),lower(trim(p_email)),domain_row.institution_code,campus,domain_row.affiliation_type,NOW())
    ON CONFLICT(user_id) DO UPDATE SET institutional_email=EXCLUDED.institutional_email,institution_code=EXCLUDED.institution_code,
      campus_id=EXCLUDED.campus_id,affiliation_type=EXCLUDED.affiliation_type,verified_at=NOW();
    UPDATE public.profiles SET student_id=trim(p_student_id),faculty=trim(p_faculty),campus_id=campus,institution_code=domain_row.institution_code,institution_name=domain_row.institution_name,
      institution_affiliation=domain_row.affiliation_type,last_active_role='student',updated_at=NOW() WHERE id=auth.uid() RETURNING * INTO result;
    DELETE FROM public.institution_verification_challenges WHERE user_id=auth.uid();
    RETURN result;
END; $$;
REVOKE ALL ON FUNCTION public.confirm_institution_verification(TEXT,TEXT,TEXT,TEXT) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.confirm_institution_verification(TEXT,TEXT,TEXT,TEXT) TO authenticated;

ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS last_active_role user_role;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.merchants ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.listings ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.service_areas ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.campus_landmarks ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS campus_id UUID REFERENCES public.campuses(id);

INSERT INTO public.institutions(code,name) VALUES
('UTAR','Universiti Tunku Abdul Rahman'),('TARUMT','Tunku Abdul Rahman University of Management and Technology'),
('SUNWAY','Sunway University'),('TAYLORS','Taylor''s University'),('MMU','Multimedia University'),
('APU','Asia Pacific University'),('UNIKL','Universiti Kuala Lumpur'),('UCSI','UCSI University'),
('UNITEN','Universiti Tenaga Nasional'),('UTP','Universiti Teknologi PETRONAS'),
('INTI','INTI International University'),('CURTIN','Curtin University Malaysia'),
('SWINBURNE','Swinburne University of Technology Sarawak'),('UNM','University of Nottingham Malaysia'),
('MONASH','Monash University Malaysia'),('UM','Universiti Malaya'),('UKM','Universiti Kebangsaan Malaysia'),
('USM','Universiti Sains Malaysia'),('UTM','Universiti Teknologi Malaysia'),('UPM','Universiti Putra Malaysia'),
('UITM','Universiti Teknologi MARA'),('UUM','Universiti Utara Malaysia'),
('IIUM','International Islamic University Malaysia'),('UNIMAS','Universiti Malaysia Sarawak'),
('UMS','Universiti Malaysia Sabah'),('UMPSA','Universiti Malaysia Pahang Al-Sultan Abdullah'),
('UTHM','Universiti Tun Hussein Onn Malaysia'),('UTEM','Universiti Teknikal Malaysia Melaka'),
('UNIMAP','Universiti Malaysia Perlis'),('USIM','Universiti Sains Islam Malaysia'),
('UNISZA','Universiti Sultan Zainal Abidin'),('UMK','Universiti Malaysia Kelantan'),
('UPSI','Universiti Pendidikan Sultan Idris'),('UMT','Universiti Malaysia Terengganu'),
('UPNM','Universiti Pertahanan Nasional Malaysia')
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name, is_active=TRUE;

-- One initial main-campus record per institution. Coordinates are reference
-- configuration and should be reviewed by the Supabase operator when a campus
-- changes its official address or boundary.
INSERT INTO public.campuses(institution_code,name,address,latitude,longitude,boundary_coordinates,is_main) VALUES
('UTAR','Kampar Campus','Jalan Universiti, Bandar Barat, 31900 Kampar, Perak',4.3395,101.1422,'[]',TRUE),
('TARUMT','Kuala Lumpur Main Campus','Jalan Genting Kelang, Setapak, 53300 Kuala Lumpur',3.2150,101.7280,'[]',TRUE),
('SUNWAY','Sunway Campus','No. 5 Jalan Universiti, Bandar Sunway, 47500 Selangor',3.0680,101.6030,'[]',TRUE),
('TAYLORS','Lakeside Campus','No. 1 Jalan Taylor''s, 47500 Subang Jaya, Selangor',3.0647,101.6168,'[]',TRUE),
('MMU','Cyberjaya Campus','Persiaran Multimedia, 63100 Cyberjaya, Selangor',2.9270,101.6420,'[]',TRUE),
('APU','Technology Park Malaysia Campus','Jalan Teknologi 5, Bukit Jalil, 57000 Kuala Lumpur',3.0550,101.7000,'[]',TRUE),
('UNIKL','City Campus','1016 Jalan Sultan Ismail, 50250 Kuala Lumpur',3.1620,101.6960,'[]',TRUE),
('UCSI','Kuala Lumpur Campus','No. 1 Jalan Menara Gading, 56000 Kuala Lumpur',3.0790,101.7330,'[]',TRUE),
('UNITEN','Putrajaya Campus','Jalan IKRAM-UNITEN, 43000 Kajang, Selangor',2.9780,101.7310,'[]',TRUE),
('UTP','Seri Iskandar Campus','Persiaran UTP, 32610 Seri Iskandar, Perak',4.3860,100.9700,'[]',TRUE),
('INTI','Nilai Campus','Persiaran Perdana BBN, 71800 Nilai, Negeri Sembilan',2.8130,101.7580,'[]',TRUE),
('CURTIN','Miri Campus','CDT 250, 98009 Miri, Sarawak',4.5070,114.0160,'[]',TRUE),
('SWINBURNE','Kuching Campus','Jalan Simpang Tiga, 93350 Kuching, Sarawak',1.5350,110.3570,'[]',TRUE),
('UNM','Semenyih Campus','Jalan Broga, 43500 Semenyih, Selangor',2.9440,101.8740,'[]',TRUE),
('MONASH','Malaysia Campus','Jalan Lagoon Selatan, Bandar Sunway, 47500 Selangor',3.0650,101.6000,'[]',TRUE),
('UM','Kuala Lumpur Main Campus','Jalan Universiti, 50603 Kuala Lumpur',3.1200,101.6540,'[]',TRUE),
('UKM','Bangi Main Campus','43600 UKM Bangi, Selangor',2.9300,101.7780,'[]',TRUE),
('USM','Penang Main Campus','11800 USM, Pulau Pinang',5.3560,100.3020,'[]',TRUE),
('UTM','Johor Bahru Main Campus','81310 Skudai, Johor',1.5590,103.6380,'[]',TRUE),
('UPM','Serdang Main Campus','43400 UPM Serdang, Selangor',2.9920,101.7160,'[]',TRUE),
('UITM','Shah Alam Main Campus','40450 Shah Alam, Selangor',3.0690,101.5030,'[]',TRUE),
('UUM','Sintok Main Campus','06010 Sintok, Kedah',6.4590,100.5060,'[]',TRUE),
('IIUM','Gombak Campus','Jalan Gombak, 53100 Kuala Lumpur',3.2530,101.7340,'[]',TRUE),
('UNIMAS','Kota Samarahan Main Campus','94300 Kota Samarahan, Sarawak',1.4640,110.4280,'[]',TRUE),
('UMS','Kota Kinabalu Main Campus','Jalan UMS, 88400 Kota Kinabalu, Sabah',6.0360,116.1180,'[]',TRUE),
('UMPSA','Pekan Campus','26600 Pekan, Pahang',3.5450,103.4290,'[]',TRUE),
('UTHM','Parit Raja Main Campus','86400 Parit Raja, Batu Pahat, Johor',1.8580,103.0860,'[]',TRUE),
('UTEM','Durian Tunggal Main Campus','Hang Tuah Jaya, 76100 Durian Tunggal, Melaka',2.3130,102.3210,'[]',TRUE),
('UNIMAP','Pauh Putra Main Campus','02600 Arau, Perlis',6.4620,100.3510,'[]',TRUE),
('USIM','Nilai Main Campus','Bandar Baru Nilai, 71800 Nilai, Negeri Sembilan',2.8440,101.7800,'[]',TRUE),
('UNISZA','Gong Badak Campus','21300 Kuala Nerus, Terengganu',5.7650,102.6270,'[]',TRUE),
('UMK','Bachok Campus','16300 Bachok, Kelantan',6.1640,102.2830,'[]',TRUE),
('UPSI','Sultan Abdul Jalil Shah Campus','35900 Tanjong Malim, Perak',3.6850,101.5240,'[]',TRUE),
('UMT','Kuala Nerus Main Campus','21030 Kuala Nerus, Terengganu',5.4070,103.0870,'[]',TRUE),
('UPNM','Sungai Besi Main Campus','Kem Sungai Besi, 57000 Kuala Lumpur',3.0490,101.7270,'[]',TRUE)
ON CONFLICT(institution_code,name) DO UPDATE SET address=EXCLUDED.address,latitude=EXCLUDED.latitude,
longitude=EXCLUDED.longitude,is_main=TRUE,is_active=TRUE;

UPDATE public.campuses
SET boundary_coordinates = jsonb_build_array(
    jsonb_build_object('latitude', round((latitude + 0.02)::numeric, 6), 'longitude', round((longitude - 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude + 0.02)::numeric, 6), 'longitude', round((longitude + 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude - 0.02)::numeric, 6), 'longitude', round((longitude + 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude - 0.02)::numeric, 6), 'longitude', round((longitude - 0.02)::numeric, 6))
)
WHERE boundary_coordinates IS NULL OR jsonb_typeof(boundary_coordinates) <> 'array' OR jsonb_array_length(boundary_coordinates) < 3;

UPDATE public.campuses
SET boundary_coordinates = jsonb_build_array(
    jsonb_build_object('latitude', 4.355000, 'longitude', 101.125000),
    jsonb_build_object('latitude', 4.355000, 'longitude', 101.160000),
    jsonb_build_object('latitude', 4.325000, 'longitude', 101.160000),
    jsonb_build_object('latitude', 4.325000, 'longitude', 101.125000)
)
WHERE institution_code = 'UTAR';

DO $$ DECLARE utar_campus UUID;
BEGIN
    SELECT id INTO utar_campus FROM public.campuses WHERE institution_code='UTAR' AND is_main LIMIT 1;
    UPDATE public.profiles SET campus_id=utar_campus WHERE campus_id IS NULL AND institution_code='UTAR';
    UPDATE public.merchants SET campus_id=utar_campus WHERE campus_id IS NULL;
    UPDATE public.listings l SET campus_id=m.campus_id FROM public.merchants m
      WHERE l.merchant_id=m.id AND l.campus_id IS NULL;
    UPDATE public.orders o SET campus_id=l.campus_id FROM public.listings l
      WHERE o.listing_id=l.id AND o.campus_id IS NULL;
    UPDATE public.service_areas SET campus_id=utar_campus WHERE campus_id IS NULL AND name='UTAR Kampar Campus';
    UPDATE public.campus_landmarks SET campus_id=utar_campus WHERE campus_id IS NULL;
END $$;

INSERT INTO public.institution_email_domains(domain,institution_code,institution_name,affiliation_type,verification_note) VALUES
('student.tarc.edu.my','TARUMT','Tunku Abdul Rahman University of Management and Technology','student','Student domain'),
('tarc.edu.my','TARUMT','Tunku Abdul Rahman University of Management and Technology','staff','Staff domain'),
('imail.sunway.edu.my','SUNWAY','Sunway University','student','Student domain'),('sunway.edu.my','SUNWAY','Sunway University','staff','Staff domain'),
('sd.taylors.edu.my','TAYLORS','Taylor''s University','student','Student domain'),('taylors.edu.my','TAYLORS','Taylor''s University','staff','Staff domain'),
('student.mmu.edu.my','MMU','Multimedia University','student','Student domain'),('soffice.mmu.edu.my','MMU','Multimedia University','student','Student domain'),('mmu.edu.my','MMU','Multimedia University','staff','Staff domain'),
('mail.apu.edu.my','APU','Asia Pacific University','student','Student domain'),('apu.edu.my','APU','Asia Pacific University','staff','Staff domain'),
('s.unikl.edu.my','UNIKL','Universiti Kuala Lumpur','student','Student domain'),('unikl.edu.my','UNIKL','Universiti Kuala Lumpur','staff','Staff domain'),
('student.ucsiuniversity.edu.my','UCSI','UCSI University','student','Student domain'),('ucsiuniversity.edu.my','UCSI','UCSI University','staff','Staff domain'),
('student.uniten.edu.my','UNITEN','Universiti Tenaga Nasional','student','Student domain'),('uniten.edu.my','UNITEN','Universiti Tenaga Nasional','staff','Staff domain'),
('student.newinti.edu.my','INTI','INTI International University','student','Student domain'),('newinti.edu.my','INTI','INTI International University','staff','Staff domain'),
('student.curtin.edu.my','CURTIN','Curtin University Malaysia','student','Student domain'),('curtin.edu.my','CURTIN','Curtin University Malaysia','staff','Staff domain'),
('students.swinburne.edu.my','SWINBURNE','Swinburne University of Technology Sarawak','student','Student domain'),('swinburne.edu.my','SWINBURNE','Swinburne University of Technology Sarawak','staff','Staff domain'),
('student.monash.edu','MONASH','Monash University Malaysia','student','Student domain'),('monash.edu','MONASH','Monash University Malaysia','staff','Staff domain'),
('siswa.um.edu.my','UM','Universiti Malaya','student','Student domain'),('um.edu.my','UM','Universiti Malaya','staff','Staff domain'),
('siswa.ukm.edu.my','UKM','Universiti Kebangsaan Malaysia','student','Student domain'),('ukm.edu.my','UKM','Universiti Kebangsaan Malaysia','staff','Staff domain'),
('student.usm.my','USM','Universiti Sains Malaysia','student','Student domain'),('usm.my','USM','Universiti Sains Malaysia','staff','Staff domain'),
('graduate.utm.my','UTM','Universiti Teknologi Malaysia','student','Student domain'),('live.utm.my','UTM','Universiti Teknologi Malaysia','student','Student domain'),('utm.my','UTM','Universiti Teknologi Malaysia','staff','Staff domain'),
('student.upm.edu.my','UPM','Universiti Putra Malaysia','student','Student domain'),('upm.edu.my','UPM','Universiti Putra Malaysia','staff','Staff domain'),
('student.uitm.edu.my','UITM','Universiti Teknologi MARA','student','Student domain'),('isiswa.uitm.edu.my','UITM','Universiti Teknologi MARA','student','Student domain'),('uitm.edu.my','UITM','Universiti Teknologi MARA','staff','Staff domain'),
('student.uum.edu.my','UUM','Universiti Utara Malaysia','student','Student domain'),('uum.edu.my','UUM','Universiti Utara Malaysia','staff','Staff domain'),
('student.iium.edu.my','IIUM','International Islamic University Malaysia','student','Student domain'),('live.iium.edu.my','IIUM','International Islamic University Malaysia','student','Student domain'),('iium.edu.my','IIUM','International Islamic University Malaysia','staff','Staff domain'),
('siswa.unimas.my','UNIMAS','Universiti Malaysia Sarawak','student','Student domain'),('unimas.my','UNIMAS','Universiti Malaysia Sarawak','staff','Staff domain'),
('student.ums.edu.my','UMS','Universiti Malaysia Sabah','student','Student domain'),('ums.edu.my','UMS','Universiti Malaysia Sabah','staff','Staff domain'),
('adab.umpsa.edu.my','UMPSA','Universiti Malaysia Pahang Al-Sultan Abdullah','student','Student domain'),('student.umpsa.edu.my','UMPSA','Universiti Malaysia Pahang Al-Sultan Abdullah','student','Student domain'),('umpsa.edu.my','UMPSA','Universiti Malaysia Pahang Al-Sultan Abdullah','staff','Staff domain'),
('siswa.uthm.edu.my','UTHM','Universiti Tun Hussein Onn Malaysia','student','Student domain'),('uthm.edu.my','UTHM','Universiti Tun Hussein Onn Malaysia','staff','Staff domain'),
('student.utem.edu.my','UTEM','Universiti Teknikal Malaysia Melaka','student','Student domain'),('utem.edu.my','UTEM','Universiti Teknikal Malaysia Melaka','staff','Staff domain'),
('studentmail.unimap.edu.my','UNIMAP','Universiti Malaysia Perlis','student','Student domain'),('student.unimap.edu.my','UNIMAP','Universiti Malaysia Perlis','student','Student domain'),('unimap.edu.my','UNIMAP','Universiti Malaysia Perlis','staff','Staff domain'),
('raudah.usim.edu.my','USIM','Universiti Sains Islam Malaysia','student','Student domain'),('usim.edu.my','USIM','Universiti Sains Islam Malaysia','staff','Staff domain'),
('siswa.unisza.edu.my','UNISZA','Universiti Sultan Zainal Abidin','student','Student domain'),('unisza.edu.my','UNISZA','Universiti Sultan Zainal Abidin','staff','Staff domain'),
('siswa.umk.edu.my','UMK','Universiti Malaysia Kelantan','student','Student domain'),('umk.edu.my','UMK','Universiti Malaysia Kelantan','staff','Staff domain'),
('siswa.upsi.edu.my','UPSI','Universiti Pendidikan Sultan Idris','student','Student domain'),('upsi.edu.my','UPSI','Universiti Pendidikan Sultan Idris','staff','Staff domain'),
('ocean.umt.edu.my','UMT','Universiti Malaysia Terengganu','student','Student domain'),('umt.edu.my','UMT','Universiti Malaysia Terengganu','staff','Staff domain'),
('siswa.upnm.edu.my','UPNM','Universiti Pertahanan Nasional Malaysia','student','Student domain'),('student.upnm.edu.my','UPNM','Universiti Pertahanan Nasional Malaysia','student','Student domain'),('upnm.edu.my','UPNM','Universiti Pertahanan Nasional Malaysia','staff','Staff domain')
ON CONFLICT (domain) DO UPDATE SET institution_code=EXCLUDED.institution_code,institution_name=EXCLUDED.institution_name,affiliation_type=EXCLUDED.affiliation_type,verification_note=EXCLUDED.verification_note,is_active=TRUE;

-- Shared domains retained explicitly as member affiliations.
UPDATE public.institution_email_domains SET institution_code='UTP', institution_name='Universiti Teknologi PETRONAS', affiliation_type='member' WHERE domain='utp.edu.my';
UPDATE public.institution_email_domains SET institution_code='UNM', institution_name='University of Nottingham Malaysia', affiliation_type='member' WHERE domain='nottingham.edu.my';

-- Reconcile confirmed legacy Student accounts after the complete domain and
-- campus registry has been seeded. This prevents a valid existing Student
-- from retaining a NULL campus merely because the legacy profile predated it.
UPDATE public.profiles p SET
  campus_id=c.id,institution_code=d.institution_code,institution_name=d.institution_name,
  institution_affiliation=d.affiliation_type,email_verified_at=u.email_confirmed_at,updated_at=NOW()
FROM auth.users u
JOIN public.institution_email_domains d ON d.domain=lower(split_part(u.email,'@',2)) AND d.is_active
JOIN public.campuses c ON c.institution_code=d.institution_code AND c.is_main AND c.is_active
WHERE p.id=u.id AND u.email_confirmed_at IS NOT NULL
  AND EXISTS(SELECT 1 FROM public.user_roles r WHERE r.user_id=p.id AND r.role='student');

INSERT INTO public.student_affiliations(user_id,institutional_email,institution_code,campus_id,affiliation_type,verified_at)
SELECT p.id,lower(u.email),p.institution_code,p.campus_id,p.institution_affiliation,u.email_confirmed_at
FROM public.profiles p JOIN auth.users u ON u.id=p.id
WHERE p.campus_id IS NOT NULL AND p.institution_code IS NOT NULL AND u.email_confirmed_at IS NOT NULL
  AND EXISTS(SELECT 1 FROM public.user_roles r WHERE r.user_id=p.id AND r.role='student')
ON CONFLICT(user_id) DO UPDATE SET institutional_email=EXCLUDED.institutional_email,
  institution_code=EXCLUDED.institution_code,campus_id=EXCLUDED.campus_id,
  affiliation_type=EXCLUDED.affiliation_type,verified_at=EXCLUDED.verified_at;

CREATE INDEX IF NOT EXISTS idx_profiles_campus ON public.profiles(campus_id);
CREATE INDEX IF NOT EXISTS idx_merchants_campus ON public.merchants(campus_id);
CREATE INDEX IF NOT EXISTS idx_listings_campus_status ON public.listings(campus_id,status);
CREATE INDEX IF NOT EXISTS idx_orders_campus ON public.orders(campus_id,status);

ALTER TABLE public.institutions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campuses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_affiliations ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Active institutions are readable" ON public.institutions;
CREATE POLICY "Active institutions are readable" ON public.institutions FOR SELECT TO authenticated USING (is_active);
DROP POLICY IF EXISTS "Active campuses are readable" ON public.campuses;
CREATE POLICY "Active campuses are readable" ON public.campuses FOR SELECT TO authenticated USING (is_active);
GRANT SELECT ON TABLE public.institutions TO authenticated;
GRANT SELECT ON TABLE public.campuses TO authenticated;
DROP POLICY IF EXISTS "Users read own roles" ON public.user_roles;
CREATE POLICY "Users read own roles" ON public.user_roles FOR SELECT TO authenticated USING (user_id=auth.uid());
DROP POLICY IF EXISTS "Users read own affiliation" ON public.student_affiliations;
CREATE POLICY "Users read own affiliation" ON public.student_affiliations FOR SELECT TO authenticated USING (user_id=auth.uid());

-- Existing accounts receive their legacy role. New registration functions add
-- roles idempotently without creating a second Auth user.
INSERT INTO public.user_roles(user_id,role) SELECT id,role FROM public.profiles ON CONFLICT DO NOTHING;
UPDATE public.profiles SET last_active_role=COALESCE(last_active_role,role);

CREATE OR REPLACE FUNCTION public.switch_active_role(p_role user_role)
RETURNS public.profiles LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.profiles%ROWTYPE;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id=auth.uid() AND role=p_role) THEN
        RAISE EXCEPTION 'Role is not available for this account';
    END IF;
    UPDATE public.profiles SET last_active_role=p_role, updated_at=NOW() WHERE id=auth.uid() RETURNING * INTO result;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.sync_verified_email()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
BEGIN
    IF NEW.email_confirmed_at IS DISTINCT FROM OLD.email_confirmed_at THEN
        UPDATE public.profiles SET email_verified_at=NEW.email_confirmed_at,updated_at=NOW() WHERE id=NEW.id;
        UPDATE public.student_affiliations SET verified_at=NEW.email_confirmed_at WHERE user_id=NEW.id;
    END IF;
    RETURN NEW;
END; $$;

CREATE OR REPLACE FUNCTION public.process_order_status_change()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE listing_rec public.listings%ROWTYPE; earned_pts INT; saved_amt NUMERIC(10,2); co2_amt NUMERIC(10,2);
BEGIN
    IF OLD.status<>'completed' AND NEW.status='completed' THEN
        SELECT * INTO listing_rec FROM public.listings WHERE id=NEW.listing_id;
        earned_pts:=NEW.quantity*10; saved_amt:=NEW.total_original_price-NEW.final_paid_price;
        co2_amt:=COALESCE(listing_rec.co2_kg_per_item,1.20)*NEW.quantity;
        UPDATE public.profiles SET eco_points=eco_points+earned_pts,meals_rescued=meals_rescued+NEW.quantity,
            money_saved=money_saved+saved_amt,co2_prevented=co2_prevented+co2_amt,updated_at=NOW() WHERE id=NEW.student_id;
        INSERT INTO public.notifications(recipient_id,recipient_role,campus_id,title,message,event_type,related_order_id)
        VALUES(NEW.student_id,'student',NEW.campus_id,'Pickup completed',
            'Thank you for rescuing surplus food. Your impact has been updated.','order_completed',NEW.id);
    END IF;
    IF OLD.status IN('awaiting_payment','pending_verification','reserved','ready_for_pickup')
       AND NEW.status IN('cancelled','expired','rejected','payment_rejected') THEN
        UPDATE public.listings SET remaining_quantity=remaining_quantity+NEW.quantity,status='active',updated_at=NOW() WHERE id=NEW.listing_id;
        IF NEW.reward_points_used>0 THEN
            UPDATE public.profiles SET eco_points=eco_points+NEW.reward_points_used WHERE id=NEW.student_id;
            DELETE FROM public.reward_redemptions WHERE order_id=NEW.id;
        END IF;
        INSERT INTO public.notifications(recipient_id,recipient_role,campus_id,title,message,event_type,related_order_id)
        VALUES(NEW.student_id,'student',NEW.campus_id,
            CASE WHEN NEW.status='expired' THEN 'Order expired' WHEN NEW.status IN('rejected','payment_rejected') THEN 'Payment rejected' ELSE 'Order cancelled' END,
            CASE WHEN NEW.status IN('rejected','payment_rejected') THEN COALESCE(NEW.rejection_reason,'The merchant rejected the receipt.') ELSE 'Reserved stock has been released.' END,
            NEW.status::text,NEW.id);
    END IF;
    RETURN NEW;
END; $$;

CREATE OR REPLACE FUNCTION public.campus_contains_point(p_campus_id UUID, p_latitude DOUBLE PRECISION, p_longitude DOUBLE PRECISION)
RETURNS BOOLEAN LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=public AS $$
DECLARE boundary JSONB; vertex_count INT; i INT; j INT; inside BOOLEAN := FALSE;
        c_lat DOUBLE PRECISION; c_lng DOUBLE PRECISION;
        xi DOUBLE PRECISION; yi DOUBLE PRECISION; xj DOUBLE PRECISION; yj DOUBLE PRECISION;
BEGIN
    IF p_latitude NOT BETWEEN -90 AND 90 OR p_longitude NOT BETWEEN -180 AND 180 THEN RETURN FALSE; END IF;
    SELECT boundary_coordinates, latitude, longitude INTO boundary, c_lat, c_lng FROM public.campuses WHERE id=p_campus_id AND is_active;
    IF c_lat IS NULL OR c_lng IS NULL THEN RETURN FALSE; END IF;
    
    vertex_count := CASE WHEN boundary IS NOT NULL AND jsonb_typeof(boundary) = 'array' THEN jsonb_array_length(boundary) ELSE 0 END;
    
    IF vertex_count >= 3 THEN
        j := vertex_count - 1;
        FOR i IN 0..vertex_count-1 LOOP
            xi := (boundary->i->>'longitude')::DOUBLE PRECISION; yi := (boundary->i->>'latitude')::DOUBLE PRECISION;
            xj := (boundary->j->>'longitude')::DOUBLE PRECISION; yj := (boundary->j->>'latitude')::DOUBLE PRECISION;
            IF ((yi > p_latitude) <> (yj > p_latitude)) AND
               (p_longitude < (xj - xi) * (p_latitude - yi) / NULLIF(yj - yi, 0) + xi) THEN
                inside := NOT inside;
            END IF;
            j := i;
        END LOOP;
        IF inside THEN RETURN TRUE; END IF;
    END IF;
    
    RETURN (6371 * 2 * asin(sqrt(power(sin(radians(p_latitude - c_lat) / 2), 2) +
            cos(radians(c_lat)) * cos(radians(p_latitude)) * power(sin(radians(p_longitude - c_lng) / 2), 2)))) <= 5.0;
EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN RETURN FALSE;
END; $$;
REVOKE ALL ON FUNCTION public.campus_contains_point(UUID,DOUBLE PRECISION,DOUBLE PRECISION) FROM PUBLIC,anon,authenticated;

-- Merchant profile/setup changes must use complete_merchant_registration so a
-- campus, coordinate or private QR path cannot bypass server validation.
REVOKE UPDATE ON public.merchants FROM authenticated;
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;

CREATE OR REPLACE FUNCTION public.complete_merchant_registration(
    p_business_name TEXT, p_stall_description TEXT, p_contact_phone TEXT,
    p_duitnow_display_name TEXT, p_duitnow_qr_path TEXT, p_campus_id UUID,
    p_campus_location TEXT, p_latitude DOUBLE PRECISION, p_longitude DOUBLE PRECISION)
RETURNS public.merchants LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.merchants%ROWTYPE;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id=auth.uid() AND email_confirmed_at IS NOT NULL) THEN
        RAISE EXCEPTION 'Verified email required';
    END IF;
    IF length(trim(COALESCE(p_business_name,'')))<2 OR length(trim(COALESCE(p_stall_description,'')))<10
       OR length(trim(COALESCE(p_contact_phone,'')))<7
       OR length(trim(COALESCE(p_duitnow_display_name,'')))<2 OR p_duitnow_qr_path IS NULL
       OR p_campus_id IS NULL OR length(trim(COALESCE(p_campus_location,'')))<2 THEN
        RAISE EXCEPTION 'Complete merchant information is required';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.campuses WHERE id=p_campus_id AND is_active) THEN
        RAISE EXCEPTION 'Active campus required';
    END IF;
    IF NOT public.campus_contains_point(p_campus_id,p_latitude,p_longitude) THEN
        RAISE EXCEPTION 'Merchant pin is outside the configured campus boundary, or that boundary has not been reviewed';
    END IF;
    IF p_duitnow_qr_path !~ ('^'||auth.uid()::text||'/') THEN RAISE EXCEPTION 'Invalid DuitNow QR path'; END IF;
    IF NOT EXISTS (SELECT 1 FROM storage.objects WHERE bucket_id='merchant-payment-qrs' AND name=p_duitnow_qr_path) THEN
        RAISE EXCEPTION 'Uploaded DuitNow QR was not found';
    END IF;
    INSERT INTO public.user_roles(user_id,role) VALUES(auth.uid(),'merchant') ON CONFLICT DO NOTHING;
    INSERT INTO public.merchants(owner_id,business_name,stall_description,contact_phone,duitnow_display_name,
        duitnow_qr_path,campus_id,campus_location,latitude,longitude,status,terms_accepted_at)
    VALUES(auth.uid(),trim(p_business_name),trim(p_stall_description),trim(p_contact_phone),trim(p_duitnow_display_name),
        p_duitnow_qr_path,p_campus_id,trim(p_campus_location),p_latitude,p_longitude,'approved',NOW())
    ON CONFLICT(owner_id) DO UPDATE SET business_name=EXCLUDED.business_name,stall_description=EXCLUDED.stall_description,
        contact_phone=EXCLUDED.contact_phone,duitnow_display_name=EXCLUDED.duitnow_display_name,
        duitnow_qr_path=EXCLUDED.duitnow_qr_path,campus_id=EXCLUDED.campus_id,campus_location=EXCLUDED.campus_location,
        latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,status='approved',rejection_reason=NULL,terms_accepted_at=NOW()
    RETURNING * INTO result;
    UPDATE public.profiles SET last_active_role='merchant',campus_id=p_campus_id,updated_at=NOW() WHERE id=auth.uid();
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.enforce_listing_merchant_location()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE m public.merchants%ROWTYPE;
BEGIN
    SELECT * INTO m FROM public.merchants WHERE id=NEW.merchant_id AND owner_id=auth.uid() AND status='approved';
    IF NOT FOUND OR m.campus_id IS NULL THEN RAISE EXCEPTION 'Approved campus merchant required'; END IF;
    NEW.campus_id:=m.campus_id; NEW.pickup_location:=m.campus_location;
    NEW.latitude:=m.latitude; NEW.longitude:=m.longitude;
    RETURN NEW;
END; $$;
DROP TRIGGER IF EXISTS trg_enforce_listing_merchant_location ON public.listings;
CREATE TRIGGER trg_enforce_listing_merchant_location BEFORE INSERT OR UPDATE OF merchant_id,campus_id,pickup_location,latitude,longitude
ON public.listings FOR EACH ROW EXECUTE FUNCTION public.enforce_listing_merchant_location();

CREATE OR REPLACE FUNCTION public.reserve_listing(p_listing_id UUID,p_quantity INT,p_use_reward_points BOOLEAN DEFAULT FALSE)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE l public.listings%ROWTYPE; p public.profiles%ROWTYPE; result public.orders%ROWTYPE;
        points_used INT:=0; reward_discount NUMERIC(10,2):=0; token TEXT:=encode(gen_random_bytes(24),'hex');
BEGIN
    IF auth.uid() IS NULL OR p_quantity IS NULL OR p_quantity<=0 THEN RAISE EXCEPTION 'Invalid reservation'; END IF;
    SELECT * INTO p FROM public.profiles WHERE id=auth.uid() FOR UPDATE;
    IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id=auth.uid() AND role='student')
       OR p.email_verified_at IS NULL OR p.campus_id IS NULL THEN RAISE EXCEPTION 'Verified student campus required'; END IF;
    SELECT * INTO l FROM public.listings WHERE id=p_listing_id FOR UPDATE;
    IF NOT FOUND OR l.campus_id IS DISTINCT FROM p.campus_id OR l.status<>'active' OR l.remaining_quantity<p_quantity THEN
        RAISE EXCEPTION 'Listing unavailable, outside your campus, or insufficient stock';
    END IF;
    IF p_use_reward_points AND p.eco_points>=100 THEN points_used:=100; reward_discount:=least(5.00,l.discounted_price*p_quantity); END IF;
    INSERT INTO public.orders(order_code,student_id,listing_id,merchant_id,campus_id,quantity,total_original_price,
        total_discounted_price,reward_points_used,reward_discount_amount,final_paid_price,pickup_start,pickup_end,
        pickup_token,status,payment_expires_at,payment_reference)
    VALUES('FH-'||upper(substr(replace(gen_random_uuid()::text,'-',''),1,8)),auth.uid(),l.id,l.merchant_id,l.campus_id,
        p_quantity,l.original_price*p_quantity,l.discounted_price*p_quantity,points_used,reward_discount,
        greatest(0,l.discounted_price*p_quantity-reward_discount),l.pickup_start,l.pickup_end,token,'awaiting_payment',
        (extract(epoch FROM(clock_timestamp()+interval '10 minutes'))*1000)::BIGINT,
        'FH-'||upper(substr(replace(gen_random_uuid()::text,'-',''),1,10))) RETURNING * INTO result;
    UPDATE public.listings SET remaining_quantity=remaining_quantity-p_quantity,
        status=CASE WHEN remaining_quantity-p_quantity=0 THEN 'sold_out' ELSE status END,updated_at=NOW() WHERE id=l.id;
    IF points_used>0 THEN
        UPDATE public.profiles SET eco_points=eco_points-points_used WHERE id=auth.uid();
        INSERT INTO public.reward_redemptions(student_id,order_id,points_deducted,discount_amount)
        VALUES(auth.uid(),result.id,points_used,reward_discount);
    END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.decide_payment_receipt(p_order_id UUID,p_approved BOOLEAN,p_rejection_reason TEXT DEFAULT NULL)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF NOT p_approved AND length(trim(COALESCE(p_rejection_reason,'')))<3 THEN RAISE EXCEPTION 'Rejection reason is required'; END IF;
    UPDATE public.orders o SET status=CASE WHEN p_approved THEN 'ready_for_pickup'::order_status ELSE 'payment_rejected'::order_status END,
        payment_verified_at=CASE WHEN p_approved THEN NOW() ELSE NULL END,rejection_reason=CASE WHEN p_approved THEN NULL ELSE trim(p_rejection_reason) END,
        pickup_expires_at=CASE WHEN p_approved THEN NOW()+interval '24 hours' ELSE NULL END
    WHERE o.id=p_order_id AND o.status='pending_verification' AND EXISTS(
        SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid() AND m.status='approved')
    RETURNING o.* INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Receipt already decided or merchant is not authorized'; END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.expire_unpaid_order(p_order_id UUID)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    UPDATE public.orders SET status='expired'
    WHERE id=p_order_id AND student_id=auth.uid() AND status='awaiting_payment'
      AND payment_expires_at<=(extract(epoch FROM clock_timestamp())*1000)::BIGINT
    RETURNING * INTO result;
    IF NOT FOUND THEN
        SELECT * INTO result FROM public.orders
        WHERE id=p_order_id AND student_id=auth.uid() AND status='expired';
        IF NOT FOUND THEN RAISE EXCEPTION 'Order is not due for expiry'; END IF;
    END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.cancel_unpaid_order(p_order_id UUID)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    UPDATE public.orders SET status='cancelled'
    WHERE id=p_order_id AND student_id=auth.uid() AND status='awaiting_payment'
    RETURNING * INTO result;
    IF NOT FOUND THEN
        SELECT * INTO result FROM public.orders
        WHERE id=p_order_id AND student_id=auth.uid() AND status='cancelled';
        IF NOT FOUND THEN RAISE EXCEPTION 'Only an unpaid order can be cancelled'; END IF;
    END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.complete_pickup(p_token TEXT)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF p_token IS NULL OR length(trim(p_token))<32 THEN RAISE EXCEPTION 'Malformed pickup token'; END IF;
    UPDATE public.orders o SET status='completed',completed_at=NOW()
    WHERE o.pickup_token=trim(p_token) AND o.status='ready_for_pickup' AND o.pickup_expires_at>NOW()
      AND EXISTS(SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid() AND m.status='approved')
    RETURNING o.* INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Invalid, expired, used, or unauthorized pickup token'; END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.mark_no_show(p_order_id UUID)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    UPDATE public.orders o SET status='no_show'
    WHERE o.id=p_order_id AND o.status='ready_for_pickup' AND o.pickup_expires_at<=NOW()
      AND EXISTS(SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid())
    RETURNING o.* INTO result;
    IF NOT FOUND THEN
        SELECT o.* INTO result FROM public.orders o
        WHERE o.id=p_order_id AND o.status='no_show'
          AND EXISTS(SELECT 1 FROM public.merchants m WHERE m.id=o.merchant_id AND m.owner_id=auth.uid());
        IF NOT FOUND THEN RAISE EXCEPTION 'Order is not eligible or does not belong to this merchant'; END IF;
    END IF;
    RETURN result;
END; $$;

CREATE OR REPLACE FUNCTION public.reconcile_due_orders()
RETURNS INT LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE changed INT:=0; count_now INT;
BEGIN
    UPDATE public.orders SET status='expired'
      WHERE status='awaiting_payment' AND payment_expires_at<=(extract(epoch FROM clock_timestamp())*1000)::BIGINT;
    GET DIAGNOSTICS count_now=ROW_COUNT; changed:=changed+count_now;
    UPDATE public.orders SET status='no_show'
      WHERE status='ready_for_pickup' AND pickup_expires_at<=NOW();
    GET DIAGNOSTICS count_now=ROW_COUNT; changed:=changed+count_now;
    RETURN changed;
END; $$;

-- Campus isolation replaces the legacy public catalogue policy.
CREATE OR REPLACE FUNCTION public.current_campus_id()
RETURNS UUID LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $$
    SELECT campus_id FROM public.profiles WHERE id=auth.uid();
$$;
REVOKE ALL ON FUNCTION public.current_campus_id() FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.current_campus_id() TO authenticated;

DROP POLICY IF EXISTS "Public read active listings" ON public.listings;
DROP POLICY IF EXISTS "Campus members read listings" ON public.listings;
CREATE POLICY "Campus members read listings" ON public.listings FOR SELECT TO authenticated USING (
    campus_id=public.current_campus_id() OR
    EXISTS(SELECT 1 FROM public.merchants m WHERE m.id=merchant_id AND m.owner_id=auth.uid()));

DROP POLICY IF EXISTS "Authenticated read profiles" ON public.profiles;
DROP POLICY IF EXISTS "Campus members read profiles" ON public.profiles;
CREATE POLICY "Campus members read profiles" ON public.profiles FOR SELECT TO authenticated USING (
    id=auth.uid() OR (campus_id IS NOT NULL AND campus_id=public.current_campus_id()));
DROP POLICY IF EXISTS "Public read merchants" ON public.merchants;
DROP POLICY IF EXISTS "Campus members read merchants" ON public.merchants;
CREATE POLICY "Campus members read merchants" ON public.merchants FOR SELECT TO authenticated USING (
    owner_id=auth.uid() OR campus_id=public.current_campus_id());
DROP POLICY IF EXISTS "Public read reviews" ON public.reviews;
DROP POLICY IF EXISTS "Campus members read reviews" ON public.reviews;
CREATE POLICY "Campus members read reviews" ON public.reviews FOR SELECT TO authenticated USING (
    EXISTS(SELECT 1 FROM public.merchants m WHERE m.id=merchant_id AND
      (m.owner_id=auth.uid() OR m.campus_id=public.current_campus_id())));

DROP POLICY IF EXISTS "Public read service areas" ON public.service_areas;
DROP POLICY IF EXISTS "Campus members read service areas" ON public.service_areas;
CREATE POLICY "Campus members read service areas" ON public.service_areas FOR SELECT TO authenticated USING (
    campus_id=public.current_campus_id());
DROP POLICY IF EXISTS "Public read campus landmarks" ON public.campus_landmarks;
DROP POLICY IF EXISTS "Campus members read landmarks" ON public.campus_landmarks;
CREATE POLICY "Campus members read landmarks" ON public.campus_landmarks FOR SELECT TO authenticated USING (
    campus_id=public.current_campus_id());
GRANT SELECT ON TABLE public.campus_landmarks TO anon;
DROP POLICY IF EXISTS "Registration reads active landmarks" ON public.campus_landmarks;
CREATE POLICY "Registration reads active landmarks" ON public.campus_landmarks
FOR SELECT TO anon USING (is_active);

REVOKE ALL ON FUNCTION public.lookup_institution_domain(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.reserve_listing(UUID,INT,BOOLEAN) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.submit_payment_receipt(UUID,TEXT) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.decide_payment_receipt(UUID,BOOLEAN,TEXT) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.expire_unpaid_order(UUID) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.cancel_unpaid_order(UUID) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.complete_pickup(TEXT) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.switch_active_role(user_role) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.complete_merchant_registration(TEXT,TEXT,TEXT,TEXT,TEXT,UUID,TEXT,DOUBLE PRECISION,DOUBLE PRECISION) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.mark_no_show(UUID) FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.reconcile_due_orders() FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.lookup_institution_domain(TEXT) TO anon,authenticated;
GRANT EXECUTE ON FUNCTION public.reserve_listing(UUID,INT,BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_payment_receipt(UUID,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.decide_payment_receipt(UUID,BOOLEAN,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.expire_unpaid_order(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancel_unpaid_order(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_pickup(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.switch_active_role(user_role) TO authenticated;
GRANT EXECUTE ON FUNCTION public.complete_merchant_registration(TEXT,TEXT,TEXT,TEXT,TEXT,UUID,TEXT,DOUBLE PRECISION,DOUBLE PRECISION) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_no_show(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reconcile_due_orders() TO authenticated;

-- Trigger-only functions are never callable through the Data API.
REVOKE ALL ON FUNCTION public.sync_verified_email() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.process_order_status_change() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.enforce_listing_merchant_location() FROM PUBLIC,anon,authenticated;

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE requested_role user_role:=CASE WHEN NEW.raw_user_meta_data->>'requested_role'='merchant' THEN 'merchant'::user_role ELSE 'student'::user_role END;
        domain_row public.institution_email_domains%ROWTYPE; resolved_campus UUID; user_full_name TEXT;
BEGIN
    user_full_name:=COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'full_name'),''),split_part(NEW.email,'@',1));
    SELECT * INTO domain_row FROM public.institution_email_domains
      WHERE domain=lower(split_part(trim(NEW.email),'@',2)) AND is_active;
    IF requested_role='student' AND NOT FOUND THEN RAISE EXCEPTION 'Unsupported institutional email domain'; END IF;
    IF requested_role='student' THEN
        SELECT id INTO resolved_campus FROM public.campuses
          WHERE institution_code=domain_row.institution_code AND is_main AND is_active ORDER BY created_at LIMIT 1;
        IF resolved_campus IS NULL THEN RAISE EXCEPTION 'Institution campus is not configured'; END IF;
    END IF;
    INSERT INTO public.profiles(id,email,role,last_active_role,full_name,student_id,faculty,institution_code,
        institution_name,institution_affiliation,email_verified_at,campus_id)
    VALUES(NEW.id,lower(NEW.email),requested_role,requested_role,user_full_name,
        CASE WHEN requested_role='student' THEN NEW.raw_user_meta_data->>'student_id' END,
        CASE WHEN requested_role='student' THEN NEW.raw_user_meta_data->>'faculty' END,
        domain_row.institution_code,domain_row.institution_name,domain_row.affiliation_type,NEW.email_confirmed_at,resolved_campus)
    ON CONFLICT(id) DO UPDATE SET email=EXCLUDED.email,full_name=EXCLUDED.full_name,
        email_verified_at=EXCLUDED.email_verified_at,updated_at=NOW();
    INSERT INTO public.user_roles(user_id,role) VALUES(NEW.id,requested_role) ON CONFLICT DO NOTHING;
    IF requested_role='student' THEN
        INSERT INTO public.student_affiliations(user_id,institutional_email,institution_code,campus_id,affiliation_type,verified_at)
        VALUES(NEW.id,lower(NEW.email),domain_row.institution_code,resolved_campus,domain_row.affiliation_type,NEW.email_confirmed_at)
        ON CONFLICT(user_id) DO UPDATE SET verified_at=EXCLUDED.verified_at;
    END IF;
    RETURN NEW;
END; $$;

-- This must remain last: CREATE OR REPLACE can otherwise reset function settings.
DO $post_schema_function_security$
DECLARE function_name TEXT;
BEGIN
    FOREACH function_name IN ARRAY ARRAY[
        'process_order_completion', 'process_order_reservation',
        'process_order_status_change', 'process_review_submission'
    ] LOOP
        IF to_regprocedure(format('public.%I()', function_name)) IS NOT NULL THEN
            EXECUTE format('ALTER FUNCTION public.%I() SET search_path = public, pg_temp', function_name);
        END IF;
    END LOOP;
    FOREACH function_name IN ARRAY ARRAY['handle_new_user', 'rls_auto_enable'] LOOP
        IF to_regprocedure(format('public.%I()', function_name)) IS NOT NULL THEN
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I() FROM PUBLIC, anon, authenticated', function_name);
        END IF;
    END LOOP;
END;
$post_schema_function_security$;
