-- Seed initial polygon boundaries for all campuses and provide 5 km fallback
-- in campus_contains_point to prevent blocking merchant registration.

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
    
    -- Fallback to 5km radius around campus coordinate
    RETURN (6371 * 2 * asin(sqrt(power(sin(radians(p_latitude - c_lat) / 2), 2) +
            cos(radians(c_lat)) * cos(radians(p_latitude)) * power(sin(radians(p_longitude - c_lng) / 2), 2)))) <= 5.0;
EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN RETURN FALSE;
END; $$;
REVOKE ALL ON FUNCTION public.campus_contains_point(UUID, DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC, anon, authenticated;

-- Seed boundary polygons for all campuses currently having empty coordinates
UPDATE public.campuses
SET boundary_coordinates = jsonb_build_array(
    jsonb_build_object('latitude', round((latitude + 0.02)::numeric, 6), 'longitude', round((longitude - 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude + 0.02)::numeric, 6), 'longitude', round((longitude + 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude - 0.02)::numeric, 6), 'longitude', round((longitude + 0.02)::numeric, 6)),
    jsonb_build_object('latitude', round((latitude - 0.02)::numeric, 6), 'longitude', round((longitude - 0.02)::numeric, 6))
)
WHERE boundary_coordinates IS NULL OR jsonb_typeof(boundary_coordinates) <> 'array' OR jsonb_array_length(boundary_coordinates) < 3;

-- Specific boundary polygon for UTAR Kampar Campus
UPDATE public.campuses
SET boundary_coordinates = jsonb_build_array(
    jsonb_build_object('latitude', 4.355000, 'longitude', 101.125000),
    jsonb_build_object('latitude', 4.355000, 'longitude', 101.160000),
    jsonb_build_object('latitude', 4.325000, 'longitude', 101.160000),
    jsonb_build_object('latitude', 4.325000, 'longitude', 101.125000)
)
WHERE institution_code = 'UTAR';
