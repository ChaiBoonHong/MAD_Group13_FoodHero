REVOKE ALL ON FUNCTION public.current_campus_id() FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.current_campus_id() TO authenticated;
REVOKE ALL ON FUNCTION public.sync_verified_email() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.process_order_status_change() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.enforce_listing_merchant_location() FROM PUBLIC,anon,authenticated;;
