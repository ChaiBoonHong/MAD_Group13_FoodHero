CREATE OR REPLACE FUNCTION public.campus_contains_point(p_campus_id UUID,p_latitude DOUBLE PRECISION,p_longitude DOUBLE PRECISION)
RETURNS BOOLEAN LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=public AS $$
DECLARE boundary JSONB; vertex_count INT; i INT; j INT; inside BOOLEAN:=FALSE;
        xi DOUBLE PRECISION; yi DOUBLE PRECISION; xj DOUBLE PRECISION; yj DOUBLE PRECISION;
BEGIN
    IF p_latitude NOT BETWEEN -90 AND 90 OR p_longitude NOT BETWEEN -180 AND 180 THEN RETURN FALSE; END IF;
    SELECT boundary_coordinates INTO boundary FROM public.campuses WHERE id=p_campus_id AND is_active;
    IF boundary IS NULL OR jsonb_typeof(boundary)<>'array' THEN RETURN FALSE; END IF;
    vertex_count:=jsonb_array_length(boundary);
    IF vertex_count<3 THEN RETURN FALSE; END IF;
    j:=vertex_count-1;
    FOR i IN 0..vertex_count-1 LOOP
        xi:=(boundary->i->>'longitude')::DOUBLE PRECISION; yi:=(boundary->i->>'latitude')::DOUBLE PRECISION;
        xj:=(boundary->j->>'longitude')::DOUBLE PRECISION; yj:=(boundary->j->>'latitude')::DOUBLE PRECISION;
        IF ((yi>p_latitude)<>(yj>p_latitude)) AND
           (p_longitude < (xj-xi)*(p_latitude-yi)/NULLIF(yj-yi,0)+xi) THEN inside:=NOT inside; END IF;
        j:=i;
    END LOOP;
    RETURN inside;
EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range THEN RETURN FALSE;
END; $$;
REVOKE ALL ON FUNCTION public.campus_contains_point(UUID,DOUBLE PRECISION,DOUBLE PRECISION) FROM PUBLIC,anon,authenticated;

CREATE OR REPLACE FUNCTION public.complete_merchant_registration(
    p_business_name TEXT, p_stall_description TEXT, p_contact_phone TEXT,
    p_duitnow_display_name TEXT, p_duitnow_qr_path TEXT, p_campus_id UUID,
    p_campus_location TEXT, p_latitude DOUBLE PRECISION, p_longitude DOUBLE PRECISION)
RETURNS public.merchants LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.merchants%ROWTYPE;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id=auth.uid() AND email_confirmed_at IS NOT NULL) THEN
        RAISE EXCEPTION 'Verified email required';
    END IF;
    IF length(trim(COALESCE(p_business_name,'')))<2 OR length(trim(COALESCE(p_stall_description,'')))<10
       OR length(trim(COALESCE(p_contact_phone,'')))<7
       OR length(trim(COALESCE(p_duitnow_display_name,'')))<2 OR p_duitnow_qr_path IS NULL
       OR p_campus_id IS NULL OR length(trim(COALESCE(p_campus_location,'')))<2 THEN
        RAISE EXCEPTION 'Complete merchant information is required';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.campuses WHERE id=p_campus_id AND is_active) THEN
        RAISE EXCEPTION 'Active campus required';
    END IF;
    IF NOT public.campus_contains_point(p_campus_id,p_latitude,p_longitude) THEN
        RAISE EXCEPTION 'Merchant pin is outside the configured campus boundary, or that boundary has not been reviewed';
    END IF;
    IF p_duitnow_qr_path !~ ('^'||auth.uid()::text||'/') THEN RAISE EXCEPTION 'Invalid DuitNow QR path'; END IF;
    IF NOT EXISTS (SELECT 1 FROM storage.objects WHERE bucket_id='merchant-payment-qrs' AND name=p_duitnow_qr_path) THEN
        RAISE EXCEPTION 'Uploaded DuitNow QR was not found';
    END IF;
    INSERT INTO public.user_roles(user_id,role) VALUES(auth.uid(),'merchant') ON CONFLICT DO NOTHING;
    INSERT INTO public.merchants(owner_id,business_name,stall_description,contact_phone,duitnow_display_name,
        duitnow_qr_path,campus_id,campus_location,latitude,longitude,status,terms_accepted_at)
    VALUES(auth.uid(),trim(p_business_name),trim(p_stall_description),trim(p_contact_phone),trim(p_duitnow_display_name),
        p_duitnow_qr_path,p_campus_id,trim(p_campus_location),p_latitude,p_longitude,'approved',NOW())
    ON CONFLICT(owner_id) DO UPDATE SET business_name=EXCLUDED.business_name,stall_description=EXCLUDED.stall_description,
        contact_phone=EXCLUDED.contact_phone,duitnow_display_name=EXCLUDED.duitnow_display_name,
        duitnow_qr_path=EXCLUDED.duitnow_qr_path,campus_id=EXCLUDED.campus_id,campus_location=EXCLUDED.campus_location,
        latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,status='approved',rejection_reason=NULL,terms_accepted_at=NOW()
    RETURNING * INTO result;
    UPDATE public.profiles SET last_active_role='merchant',campus_id=p_campus_id,updated_at=NOW() WHERE id=auth.uid();
    RETURN result;
END; $$;
REVOKE ALL ON FUNCTION public.complete_merchant_registration(TEXT,TEXT,TEXT,TEXT,TEXT,UUID,TEXT,DOUBLE PRECISION,DOUBLE PRECISION) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.complete_merchant_registration(TEXT,TEXT,TEXT,TEXT,TEXT,UUID,TEXT,DOUBLE PRECISION,DOUBLE PRECISION) TO authenticated;

CREATE OR REPLACE FUNCTION public.submit_payment_receipt(p_order_id UUID, p_storage_path TEXT)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE result public.orders%ROWTYPE;
BEGIN
    IF p_storage_path IS NULL OR p_storage_path !~ ('^'||auth.uid()::text||'/'||p_order_id::text||'/') THEN
        RAISE EXCEPTION 'Invalid receipt path';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM storage.objects WHERE bucket_id='payment-receipts' AND name=p_storage_path) THEN
        RAISE EXCEPTION 'Uploaded receipt was not found';
    END IF;
    UPDATE public.orders SET status='pending_verification',receipt_storage_path=p_storage_path,
        payment_receipt_url=p_storage_path,receipt_submitted_at=NOW()
    WHERE id=p_order_id AND student_id=auth.uid() AND status='awaiting_payment'
      AND payment_expires_at>(extract(epoch FROM clock_timestamp())*1000)::BIGINT
    RETURNING * INTO result;
    IF NOT FOUND THEN RAISE EXCEPTION 'Order is not awaiting payment or has expired'; END IF;
    INSERT INTO public.notifications(recipient_id,recipient_role,campus_id,title,message,event_type,related_order_id)
    SELECT m.owner_id,'merchant',result.campus_id,'Payment receipt submitted',
      'Order #'||result.order_code||' is ready for review.','payment_submitted',result.id
      FROM public.merchants m WHERE m.id=result.merchant_id;
    RETURN result;
END; $$;
REVOKE ALL ON FUNCTION public.submit_payment_receipt(UUID,TEXT) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.submit_payment_receipt(UUID,TEXT) TO authenticated;
