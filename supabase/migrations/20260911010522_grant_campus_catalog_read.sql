-- Supabase projects created with automatic Data API exposure disabled require
-- explicit table privileges in addition to row-level security policies.
GRANT SELECT ON TABLE public.institutions TO authenticated;
GRANT SELECT ON TABLE public.campuses TO authenticated;

ALTER TABLE public.institutions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campuses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Active institutions are readable" ON public.institutions;
CREATE POLICY "Active institutions are readable"
ON public.institutions FOR SELECT TO authenticated
USING (is_active);

DROP POLICY IF EXISTS "Active campuses are readable" ON public.campuses;
CREATE POLICY "Active campuses are readable"
ON public.campuses FOR SELECT TO authenticated
USING (is_active);
