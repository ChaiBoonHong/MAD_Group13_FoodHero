-- ============================================================================
-- FoodHero Database Migration: 001_initial_schema.sql
-- Complete Supabase Schema, RLS Policies, Triggers & Seed Data for UTAR Kampar
-- ============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. ENUMS
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

-- 3. PROFILES TABLE
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    role user_role NOT NULL DEFAULT 'student',
    full_name TEXT NOT NULL,
    student_id TEXT,
    faculty TEXT,
    eco_points INT NOT NULL DEFAULT 0 CHECK (eco_points >= 0),
    meals_rescued INT NOT NULL DEFAULT 0 CHECK (meals_rescued >= 0),
    money_saved NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (money_saved >= 0),
    co2_prevented NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (co2_prevented >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. ALLOWLISTS (Demo verification)
CREATE TABLE IF NOT EXISTS student_allowlist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    student_id TEXT NOT NULL,
    faculty TEXT NOT NULL,
    full_name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS merchant_allowlist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    business_name TEXT NOT NULL,
    campus_location TEXT NOT NULL
);

-- 5. MERCHANTS TABLE
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    business_name TEXT NOT NULL,
    campus_location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    closing_time TEXT NOT NULL DEFAULT '18:00',
    rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    total_reviews INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. SERVICE AREAS (Authoritative UTAR Kampar Campus Polygon)
CREATE TABLE IF NOT EXISTS service_areas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    center_latitude DOUBLE PRECISION NOT NULL DEFAULT 4.336214,
    center_longitude DOUBLE PRECISION NOT NULL DEFAULT 101.142111,
    polygon_coordinates JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 7. CAMPUS LANDMARKS (Approved UTAR Kampar locations)
CREATE TABLE IF NOT EXISTS campus_landmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    category TEXT NOT NULL, -- 'entrance', 'academic_block', 'student_pavilion', 'landmark'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8. LISTINGS TABLE
CREATE TABLE IF NOT EXISTS listings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
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

-- 9. ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_code TEXT UNIQUE NOT NULL,
    student_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
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

-- 10. REVIEWS TABLE
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID UNIQUE NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. USER LOCATIONS
CREATE TABLE IF NOT EXISTS user_locations (
    student_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    is_inside_campus BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    recipient_role user_role NOT NULL DEFAULT 'student',
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    event_type TEXT NOT NULL,
    related_listing_id UUID REFERENCES listings(id) ON DELETE SET NULL,
    related_order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. REWARD REDEMPTIONS TABLE
CREATE TABLE IF NOT EXISTS reward_redemptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    points_deducted INT NOT NULL CHECK (points_deducted > 0),
    discount_amount NUMERIC(10, 2) NOT NULL CHECK (discount_amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 14. BUSINESS LOGIC FUNCTIONS & TRIGGERS
-- ============================================================================

-- Function: Atomic Order Reservation & Inventory Reduction
CREATE OR REPLACE FUNCTION process_order_reservation()
RETURNS TRIGGER AS $$
DECLARE
    curr_stock INT;
    listing_rec RECORD;
BEGIN
    -- Lock and check listing
    SELECT * INTO listing_rec FROM listings WHERE id = NEW.listing_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Listing not found.';
    END IF;
    
    IF listing_rec.status != 'active' THEN
        RAISE EXCEPTION 'Listing is no longer active.';
    END IF;
    
    IF listing_rec.remaining_quantity < NEW.quantity THEN
        RAISE EXCEPTION 'Insufficient stock available. Remaining: %', listing_rec.remaining_quantity;
    END IF;

    -- Decrement stock atomically
    curr_stock := listing_rec.remaining_quantity - NEW.quantity;
    UPDATE listings 
    SET remaining_quantity = curr_stock,
        status = CASE WHEN curr_stock = 0 THEN 'sold_out'::listing_status ELSE status END,
        updated_at = NOW()
    WHERE id = NEW.listing_id;

    -- Deduct points if used
    IF NEW.reward_points_used > 0 THEN
        UPDATE profiles
        SET eco_points = eco_points - NEW.reward_points_used
        WHERE id = NEW.student_id;

        INSERT INTO reward_redemptions (student_id, order_id, points_deducted, discount_amount)
        VALUES (NEW.student_id, NEW.id, NEW.reward_points_used, NEW.reward_discount_amount);
    END IF;

    -- Send notification to merchant about new reservation
    INSERT INTO notifications (recipient_id, recipient_role, title, message, event_type, related_listing_id, related_order_id)
    VALUES (
        listing_rec.merchant_id, 
        'merchant', 
        'New Surplus Bag Reservation!', 
        'Order #' || NEW.order_code || ' reserved for ' || NEW.quantity || ' item(s).',
        'reservation_created',
        NEW.listing_id,
        NEW.id
    );

    -- If stock reached 0, alert merchant
    IF curr_stock = 0 THEN
        INSERT INTO notifications (recipient_id, recipient_role, title, message, event_type, related_listing_id)
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
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_process_order_reservation ON orders;
CREATE TRIGGER trg_process_order_reservation
BEFORE INSERT ON orders
FOR EACH ROW EXECUTE FUNCTION process_order_reservation();

-- Function: Process Order Completion & Award Points
CREATE OR REPLACE FUNCTION process_order_completion()
RETURNS TRIGGER AS $$
DECLARE
    listing_rec RECORD;
    earned_pts INT;
    saved_amt NUMERIC(10, 2);
    co2_amt NUMERIC(10, 2);
BEGIN
    IF OLD.status = 'reserved' AND NEW.status = 'completed' THEN
        SELECT * INTO listing_rec FROM listings WHERE id = NEW.listing_id;
        earned_pts := NEW.quantity * 10;
        saved_amt := (NEW.total_original_price - NEW.final_paid_price);
        co2_amt := (listing_rec.co2_kg_per_item * NEW.quantity);

        -- Award points & update student eco stats
        UPDATE profiles
        SET eco_points = eco_points + earned_pts,
            meals_rescued = meals_rescued + NEW.quantity,
            money_saved = money_saved + saved_amt,
            co2_prevented = co2_prevented + co2_amt,
            updated_at = NOW()
        WHERE id = NEW.student_id;

        -- Notify student
        INSERT INTO notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
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
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_process_order_completion ON orders;
CREATE TRIGGER trg_process_order_completion
AFTER UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION process_order_completion();

-- Function: Process Review & Update Merchant Rating
CREATE OR REPLACE FUNCTION process_review_submission()
RETURNS TRIGGER AS $$
DECLARE
    avg_r NUMERIC(3, 2);
    tot_r INT;
BEGIN
    SELECT AVG(rating)::NUMERIC(3, 2), COUNT(*) INTO avg_r, tot_r 
    FROM reviews WHERE merchant_id = NEW.merchant_id;

    UPDATE merchants 
    SET rating = avg_r, total_reviews = tot_r 
    WHERE id = NEW.merchant_id;

    -- Notify merchant of new review
    INSERT INTO notifications (recipient_id, recipient_role, title, message, event_type, related_order_id)
    VALUES (
        (SELECT owner_id FROM merchants WHERE id = NEW.merchant_id),
        'merchant',
        'New Customer Review Received',
        'A student rated your surplus meal ' || NEW.rating || ' stars.',
        'review_received',
        NEW.order_id
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_process_review_submission ON reviews;
CREATE TRIGGER trg_process_review_submission
AFTER INSERT ON reviews
FOR EACH ROW EXECUTE FUNCTION process_review_submission();

-- ============================================================================
-- 15. ROW LEVEL SECURITY (RLS)
-- ============================================================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_areas ENABLE ROW LEVEL SECURITY;
ALTER TABLE campus_landmarks ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE reward_redemptions ENABLE ROW LEVEL SECURITY;

-- Public read for service areas and campus landmarks
CREATE POLICY "Public read service areas" ON service_areas FOR SELECT USING (true);
CREATE POLICY "Public read campus landmarks" ON campus_landmarks FOR SELECT USING (true);

-- Profiles policies
CREATE POLICY "Users can read all profiles" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Merchants policies
CREATE POLICY "Public read merchants" ON merchants FOR SELECT USING (true);
CREATE POLICY "Merchants can insert own record" ON merchants FOR INSERT WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "Merchants can update own record" ON merchants FOR UPDATE USING (auth.uid() = owner_id);

-- Listings policies
CREATE POLICY "Public read active listings" ON listings FOR SELECT USING (true);
CREATE POLICY "Merchants can manage own listings" ON listings FOR ALL USING (
    merchant_id IN (SELECT id FROM merchants WHERE owner_id = auth.uid())
);

-- Orders policies
CREATE POLICY "Students and Merchants can read relevant orders" ON orders FOR SELECT USING (
    auth.uid() = student_id OR 
    merchant_id IN (SELECT id FROM merchants WHERE owner_id = auth.uid())
);
CREATE POLICY "Students can create orders" ON orders FOR INSERT WITH CHECK (auth.uid() = student_id);
CREATE POLICY "Students and Merchants can update relevant orders" ON orders FOR UPDATE USING (
    auth.uid() = student_id OR 
    merchant_id IN (SELECT id FROM merchants WHERE owner_id = auth.uid())
);

-- Reviews policies
CREATE POLICY "Public read reviews" ON reviews FOR SELECT USING (true);
CREATE POLICY "Students can insert review for own order" ON reviews FOR INSERT WITH CHECK (auth.uid() = student_id);

-- Notifications policies
CREATE POLICY "Users can read own notifications" ON notifications FOR SELECT USING (auth.uid() = recipient_id);
CREATE POLICY "Users can update own notifications" ON notifications FOR UPDATE USING (auth.uid() = recipient_id);

-- ============================================================================
-- 16. SEED DATA (UTAR KAMPAR CAMPUS)
-- ============================================================================

-- Seed UTAR Kampar Campus Boundary Polygon
INSERT INTO service_areas (name, center_latitude, center_longitude, polygon_coordinates, is_active)
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

-- Seed UTAR Kampar Landmarks
INSERT INTO campus_landmarks (name, category, latitude, longitude) VALUES
('East Gate (Main Entrance)', 'entrance', 4.338500, 101.146500),
('West Gate (Hostel / Sport Complex Entrance)', 'entrance', 4.332800, 101.137200),
('North Gate', 'entrance', 4.343200, 101.141500),
('Student Pavilion I (Cafeteria)', 'student_pavilion', 4.335800, 101.141200),
('Student Pavilion II (Cafeteria)', 'student_pavilion', 4.337500, 101.143800),
('Block A - Heritage Hall', 'landmark', 4.339200, 101.144500),
('Dewan Tun Dr Ling Liong Sik', 'landmark', 4.338800, 101.143500),
('Block N - FICT (Faculty of Information & Communication Tech)', 'academic_block', 4.336500, 101.140200),
('Block K - FEGT (Faculty of Engineering & Green Tech)', 'academic_block', 4.335200, 101.139500),
('Block D - FBF (Faculty of Business & Finance)', 'academic_block', 4.337800, 101.142000),
('UTAR Kampar Library', 'academic_block', 4.338200, 101.144000),
('Sports Complex & Gymnasium', 'landmark', 4.333500, 101.138000)
ON CONFLICT DO NOTHING;

-- Seed Demo Allowlists
INSERT INTO student_allowlist (email, student_id, faculty, full_name) VALUES
('student.demo@utar.edu.my', '22ACB01234', 'FICT', 'Chai Boon Hong (Student Demo)'),
('student2.demo@utar.edu.my', '22ACB05678', 'FBF', 'Fong Chee Hou (Student Demo 2)')
ON CONFLICT DO NOTHING;

INSERT INTO merchant_allowlist (email, business_name, campus_location) VALUES
('merchant.demo@utar.edu.my', 'Grand Green Cafe', 'Student Pavilion I, Cafeteria Stn 3'),
('bakery.demo@utar.edu.my', 'Kampar Campus Bakery', 'Student Pavilion II, Ground Floor')
ON CONFLICT DO NOTHING;
