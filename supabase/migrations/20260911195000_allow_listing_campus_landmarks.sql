-- Listings may use any active landmark in the approved merchant's campus.
-- Snap the client-supplied coordinates and label to the authoritative landmark row.
CREATE OR REPLACE FUNCTION public.enforce_listing_merchant_location()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    m public.merchants%ROWTYPE;
    landmark public.campus_landmarks%ROWTYPE;
BEGIN
    SELECT * INTO m
    FROM public.merchants
    WHERE id = NEW.merchant_id
      AND owner_id = auth.uid()
      AND status = 'approved';

    IF NOT FOUND OR m.campus_id IS NULL THEN
        RAISE EXCEPTION 'Approved campus merchant required';
    END IF;

    SELECT * INTO landmark
    FROM public.campus_landmarks
    WHERE campus_id = m.campus_id
      AND is_active = TRUE
      AND (
          lower(trim(name)) = lower(trim(NEW.pickup_location))
          OR (
              abs(latitude - NEW.latitude) < 0.000001
              AND abs(longitude - NEW.longitude) < 0.000001
          )
      )
    ORDER BY
      CASE WHEN lower(trim(name)) = lower(trim(NEW.pickup_location)) THEN 0 ELSE 1 END,
      name
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Select an active pickup landmark in the merchant campus';
    END IF;

    NEW.campus_id := m.campus_id;
    NEW.pickup_location := landmark.name;
    NEW.latitude := landmark.latitude;
    NEW.longitude := landmark.longitude;
    RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.enforce_listing_merchant_location() FROM PUBLIC, anon, authenticated;

