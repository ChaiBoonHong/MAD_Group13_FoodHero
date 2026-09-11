-- Merchant pickup locations must reference the authoritative active landmark catalogue.
CREATE OR REPLACE FUNCTION public.enforce_merchant_campus_landmark()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    landmark public.campus_landmarks%ROWTYPE;
BEGIN
    SELECT * INTO landmark
    FROM public.campus_landmarks
    WHERE campus_id = NEW.campus_id
      AND is_active = TRUE
      AND (
          lower(trim(name)) = lower(trim(NEW.campus_location))
          OR (
              abs(latitude - NEW.latitude) < 0.000001
              AND abs(longitude - NEW.longitude) < 0.000001
          )
      )
    ORDER BY
      CASE WHEN lower(trim(name)) = lower(trim(NEW.campus_location)) THEN 0 ELSE 1 END,
      name
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Select an active pickup landmark in the selected campus';
    END IF;

    NEW.campus_location := landmark.name;
    NEW.latitude := landmark.latitude;
    NEW.longitude := landmark.longitude;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_merchant_campus_landmark ON public.merchants;
CREATE TRIGGER trg_enforce_merchant_campus_landmark
BEFORE INSERT OR UPDATE OF campus_id, campus_location, latitude, longitude
ON public.merchants
FOR EACH ROW
EXECUTE FUNCTION public.enforce_merchant_campus_landmark();

REVOKE ALL ON FUNCTION public.enforce_merchant_campus_landmark() FROM PUBLIC, anon, authenticated;
