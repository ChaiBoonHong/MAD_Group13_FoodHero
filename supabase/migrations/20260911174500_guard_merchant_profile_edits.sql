-- All merchant profile edits, including payment QR and pickup coordinates, use
-- complete_merchant_registration. It validates ownership through auth.uid(),
-- verifies the Storage object, and checks the reviewed campus boundary.
REVOKE UPDATE ON public.merchants FROM authenticated;
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;
