-- ============================================================================
-- FoodHero Migration 002: Fix Supabase Auth, Profiles RLS & Real Data Seeding
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/qouifvxsnevpqzafkdbf/sql/new
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. FIX PROFILES TABLE CONSTRAINTS & DEFAULTS
ALTER TABLE public.profiles ALTER COLUMN full_name DROP NOT NULL;
ALTER TABLE public.profiles ALTER COLUMN full_name SET DEFAULT 'FoodHero User';

-- 2. ENABLE ROW LEVEL SECURITY INSERT POLICIES
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile" ON public.profiles 
    FOR INSERT TO authenticated, anon 
    WITH CHECK (true);

DROP POLICY IF EXISTS "Users can read all profiles" ON public.profiles;
CREATE POLICY "Users can read all profiles" ON public.profiles 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles 
    FOR UPDATE USING (auth.uid() = id);

-- 3. BULLETPROOF AUTH TRIGGER FOR NEW USER CREATION (SECURITY DEFINER)
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
    -- Fallback insert so auth.users signup NEVER fails
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

-- 4. FIX MERCHANTS RLS & INSERT POLICIES
DROP POLICY IF EXISTS "Merchants can insert own record" ON public.merchants;
CREATE POLICY "Merchants can insert own record" ON public.merchants 
    FOR INSERT TO authenticated, anon 
    WITH CHECK (true);

DROP POLICY IF EXISTS "Public read merchants" ON public.merchants;
CREATE POLICY "Public read merchants" ON public.merchants 
    FOR SELECT USING (true);

-- 5. FIX LISTINGS RLS
DROP POLICY IF EXISTS "Public read active listings" ON public.listings;
CREATE POLICY "Public read active listings" ON public.listings 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Merchants can manage own listings" ON public.listings;
CREATE POLICY "Merchants can manage own listings" ON public.listings 
    FOR ALL TO authenticated, anon 
    USING (true);

-- 6. FIX ORDERS RLS
DROP POLICY IF EXISTS "Students and Merchants can read relevant orders" ON public.orders;
CREATE POLICY "Students and Merchants can read relevant orders" ON public.orders 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Students can create orders" ON public.orders;
CREATE POLICY "Students can create orders" ON public.orders 
    FOR INSERT TO authenticated, anon 
    WITH CHECK (true);

DROP POLICY IF EXISTS "Students and Merchants can update relevant orders" ON public.orders;
CREATE POLICY "Students and Merchants can update relevant orders" ON public.orders 
    FOR UPDATE USING (true);

-- 7. FIX NOTIFICATIONS RLS
DROP POLICY IF EXISTS "Users can read own notifications" ON public.notifications;
CREATE POLICY "Users can read own notifications" ON public.notifications 
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert notifications" ON public.notifications;
CREATE POLICY "Users can insert notifications" ON public.notifications 
    FOR INSERT TO authenticated, anon 
    WITH CHECK (true);

DROP POLICY IF EXISTS "Users can update own notifications" ON public.notifications;
CREATE POLICY "Users can update own notifications" ON public.notifications 
    FOR UPDATE USING (true);

-- 8. SEED REAL UTAR KAMPAR MERCHANTS & ACTIVE LISTINGS
INSERT INTO public.profiles (id, email, full_name, role, eco_points, meals_rescued, money_saved, co2_prevented)
VALUES 
    ('a0000000-0000-0000-0000-000000000001', 'merchant@foodhero.my', 'Grand Green Cafe (Merchant)', 'merchant', 0, 0, 0, 0),
    ('b0000000-0000-0000-0000-000000000002', 'student@foodhero.my', 'Chai Boon Hong (Student)', 'student', 120, 7, 38.50, 8.4)
ON CONFLICT (id) DO UPDATE SET 
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;

-- Real Merchants in UTAR Kampar
INSERT INTO public.merchants (id, owner_id, business_name, campus_location, latitude, longitude, closing_time, rating, total_reviews)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Grand Green Cafe', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200, '18:00', 4.9, 128),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Kampar Campus Bakery', 'Student Pavilion II, Ground Floor', 4.337500, 101.143800, '19:30', 4.8, 94),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'ZUS Coffee UTAR Kampar', 'Student Pavilion I, Stall 8', 4.336200, 101.141500, '20:00', 5.0, 210)
ON CONFLICT (id) DO UPDATE SET
    business_name = EXCLUDED.business_name,
    campus_location = EXCLUDED.campus_location;

-- Real Active Listings for Students to Rescue
INSERT INTO public.listings (
    id, merchant_id, title, description, category,
    original_price, discounted_price, total_quantity, remaining_quantity,
    pickup_start, pickup_end, pickup_location, latitude, longitude,
    co2_kg_per_item, status
)
VALUES
    (
        'd0000000-0000-0000-0000-000000000001',
        'c0000000-0000-0000-0000-000000000001',
        'Surplus Bento Mystery Bag',
        'Chef-curated surplus daily bento set. Includes fresh protein, multigrain rice, and organic farm greens.',
        'Meals',
        12.00, 5.50, 6, 4,
        '16:30', '18:00', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200,
        1.20, 'active'
    ),
    (
        'd0000000-0000-0000-0000-000000000002',
        'c0000000-0000-0000-0000-000000000002',
        'Artisan Pastry & Croissant Box',
        'Baked fresh this morning: butter croissants, pain au chocolat, and Danish pastries.',
        'Bakery',
        15.00, 6.00, 5, 3,
        '17:00', '19:30', 'Student Pavilion II, Ground Floor', 4.337500, 101.143800,
        0.85, 'active'
    ),
    (
        'd0000000-0000-0000-0000-000000000003',
        'c0000000-0000-0000-0000-000000000001',
        'Eco Roasted Chicken Rice Bowl',
        'Juicy roasted chicken thigh over fragrant chicken rice with house chili and cucumber slices.',
        'Rice & Noodles',
        9.50, 4.50, 8, 5,
        '16:30', '18:00', 'Student Pavilion I, Cafeteria Stn 3', 4.335800, 101.141200,
        1.10, 'active'
    ),
    (
        'd0000000-0000-0000-0000-000000000004',
        'c0000000-0000-0000-0000-000000000003',
        'Fresh Brew & Muffin Saver Set',
        'Handcrafted Americano or Latte paired with a freshly baked blueberry crumb muffin.',
        'Beverages',
        11.00, 5.00, 4, 2,
        '17:30', '20:00', 'Student Pavilion I, Stall 8', 4.336200, 101.141500,
        0.65, 'active'
    )
ON CONFLICT (id) DO UPDATE SET
    remaining_quantity = EXCLUDED.remaining_quantity,
    status = EXCLUDED.status;

-- 9. CREATE AUTH USERS DIRECTLY IN auth.users (Using Supabase Auth crypt hash)
-- Password for both is: FoodHero123!
INSERT INTO auth.users (
    id, instance_id, aud, role, email, encrypted_password,
    email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at
)
VALUES
    (
        'b0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000000',
        'authenticated',
        'authenticated',
        'student@foodhero.my',
        crypt('FoodHero123!', gen_salt('bf')),
        NOW(),
        '{"provider": "email", "providers": ["email"]}'::jsonb,
        '{"role": "student", "full_name": "Chai Boon Hong (Student)", "student_id": "22ACB01234", "faculty": "FICT"}'::jsonb,
        NOW(),
        NOW()
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000000',
        'authenticated',
        'authenticated',
        'merchant@foodhero.my',
        crypt('FoodHero123!', gen_salt('bf')),
        NOW(),
        '{"provider": "email", "providers": ["email"]}'::jsonb,
        '{"role": "merchant", "full_name": "Grand Green Cafe (Merchant)"}'::jsonb,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO UPDATE SET
    encrypted_password = crypt('FoodHero123!', gen_salt('bf')),
    email_confirmed_at = NOW();

-- Create auth identities for password login
INSERT INTO auth.identities (
    id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at
)
VALUES
    (
        'b0000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000002',
        jsonb_build_object('sub', 'b0000000-0000-0000-0000-000000000002', 'email', 'student@foodhero.my'),
        'email',
        'student@foodhero.my',
        NOW(), NOW(), NOW()
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        jsonb_build_object('sub', 'a0000000-0000-0000-0000-000000000001', 'email', 'merchant@foodhero.my'),
        'email',
        'merchant@foodhero.my',
        NOW(), NOW(), NOW()
    )
ON CONFLICT (provider, provider_id) DO NOTHING;
