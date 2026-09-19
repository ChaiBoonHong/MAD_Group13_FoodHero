REVOKE UPDATE ON public.merchants FROM authenticated;
DROP POLICY IF EXISTS "Merchants can update own record" ON public.merchants;;
