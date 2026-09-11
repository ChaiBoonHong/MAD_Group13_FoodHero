-- Reviews are inserted by authenticated students, but the aggregation trigger
-- must update the merchant rollup and create its notification atomically.
ALTER FUNCTION public.process_review_submission()
    SECURITY DEFINER;
ALTER FUNCTION public.process_review_submission()
    SET search_path = public, pg_temp;

REVOKE ALL ON FUNCTION public.process_review_submission() FROM PUBLIC, anon, authenticated;
