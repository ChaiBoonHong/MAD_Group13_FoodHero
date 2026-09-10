DROP FUNCTION IF EXISTS public.confirm_institution_verification(TEXT,TEXT);

CREATE OR REPLACE FUNCTION public.confirm_institution_verification(
    p_email TEXT,
    p_code TEXT,
    p_student_id TEXT,
    p_faculty TEXT
)
RETURNS public.profiles
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path=public
AS $$
DECLARE
    challenge public.institution_verification_challenges%ROWTYPE;
    domain_row public.institution_email_domains%ROWTYPE;
    campus UUID;
    result public.profiles%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    IF length(trim(coalesce(p_student_id,'')))<3 OR length(trim(coalesce(p_faculty,'')))<2 THEN
        RAISE EXCEPTION 'Student ID and faculty are required';
    END IF;
    SELECT * INTO challenge FROM public.institution_verification_challenges
      WHERE user_id=auth.uid() FOR UPDATE;
    IF NOT FOUND OR challenge.institutional_email<>lower(trim(p_email))
       OR challenge.expires_at<=NOW() OR challenge.failed_attempts>=5 THEN
        RAISE EXCEPTION 'Verification code is invalid or expired';
    END IF;
    IF crypt(p_code,challenge.code_hash)<>challenge.code_hash THEN
        UPDATE public.institution_verification_challenges
          SET failed_attempts=failed_attempts+1 WHERE user_id=auth.uid();
        RAISE EXCEPTION 'Verification code is invalid or expired';
    END IF;
    SELECT * INTO domain_row FROM public.institution_email_domains
      WHERE domain=lower(split_part(trim(p_email),'@',2)) AND is_active;
    IF NOT FOUND THEN RAISE EXCEPTION 'Institutional email domain is not supported'; END IF;
    SELECT id INTO campus FROM public.campuses
      WHERE institution_code=domain_row.institution_code AND is_main AND is_active LIMIT 1;
    IF campus IS NULL THEN RAISE EXCEPTION 'Institution campus is not configured'; END IF;
    INSERT INTO public.user_roles(user_id,role) VALUES(auth.uid(),'student') ON CONFLICT DO NOTHING;
    INSERT INTO public.student_affiliations(
      user_id,institutional_email,institution_code,campus_id,affiliation_type,verified_at)
    VALUES(auth.uid(),lower(trim(p_email)),domain_row.institution_code,campus,domain_row.affiliation_type,NOW())
    ON CONFLICT(user_id) DO UPDATE SET
      institutional_email=EXCLUDED.institutional_email,
      institution_code=EXCLUDED.institution_code,
      campus_id=EXCLUDED.campus_id,
      affiliation_type=EXCLUDED.affiliation_type,
      verified_at=NOW();
    UPDATE public.profiles SET
      student_id=trim(p_student_id), faculty=trim(p_faculty), campus_id=campus,
      institution_code=domain_row.institution_code, institution_name=domain_row.institution_name,
      institution_affiliation=domain_row.affiliation_type, last_active_role='student', updated_at=NOW()
      WHERE id=auth.uid() RETURNING * INTO result;
    DELETE FROM public.institution_verification_challenges WHERE user_id=auth.uid();
    RETURN result;
END;
$$;

REVOKE ALL ON FUNCTION public.confirm_institution_verification(TEXT,TEXT,TEXT,TEXT) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.confirm_institution_verification(TEXT,TEXT,TEXT,TEXT) TO authenticated;
