-- Merchant registration happens before an authenticated session exists.
-- Campus landmarks are non-sensitive public reference data; expose only rows
-- explicitly marked active while retaining RLS for every other operation.
GRANT SELECT ON TABLE public.campus_landmarks TO anon;

ALTER TABLE public.campus_landmarks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Registration reads active landmarks" ON public.campus_landmarks;
CREATE POLICY "Registration reads active landmarks"
ON public.campus_landmarks FOR SELECT TO anon
USING (is_active);
