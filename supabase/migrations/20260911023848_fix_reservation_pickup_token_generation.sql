-- pgcrypto is installed in Supabase's extensions schema. The reservation RPC
-- uses a restricted search_path, so the token generator must be qualified.
CREATE OR REPLACE FUNCTION public.reserve_listing(p_listing_id UUID,p_quantity INT,p_use_reward_points BOOLEAN DEFAULT FALSE)
RETURNS public.orders LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE l public.listings%ROWTYPE; p public.profiles%ROWTYPE; result public.orders%ROWTYPE;
        points_used INT:=0; reward_discount NUMERIC(10,2):=0;
        token TEXT:=encode(extensions.gen_random_bytes(24),'hex');
BEGIN
    IF auth.uid() IS NULL OR p_quantity IS NULL OR p_quantity<=0 THEN RAISE EXCEPTION 'Invalid reservation'; END IF;
    SELECT * INTO p FROM public.profiles WHERE id=auth.uid() FOR UPDATE;
    IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id=auth.uid() AND role='student')
       OR p.email_verified_at IS NULL OR p.campus_id IS NULL THEN RAISE EXCEPTION 'Verified student campus required'; END IF;
    SELECT * INTO l FROM public.listings WHERE id=p_listing_id FOR UPDATE;
    IF NOT FOUND OR l.campus_id IS DISTINCT FROM p.campus_id OR l.status<>'active' OR l.remaining_quantity<p_quantity THEN
        RAISE EXCEPTION 'Listing unavailable, outside your campus, or insufficient stock';
    END IF;
    IF p_use_reward_points AND p.eco_points>=100 THEN points_used:=100; reward_discount:=least(5.00,l.discounted_price*p_quantity); END IF;
    INSERT INTO public.orders(order_code,student_id,listing_id,merchant_id,campus_id,quantity,total_original_price,
        total_discounted_price,reward_points_used,reward_discount_amount,final_paid_price,pickup_start,pickup_end,
        pickup_token,status,payment_expires_at,payment_reference)
    VALUES('FH-'||upper(substr(replace(gen_random_uuid()::text,'-',''),1,8)),auth.uid(),l.id,l.merchant_id,l.campus_id,
        p_quantity,l.original_price*p_quantity,l.discounted_price*p_quantity,points_used,reward_discount,
        greatest(0,l.discounted_price*p_quantity-reward_discount),l.pickup_start,l.pickup_end,token,'awaiting_payment',
        (extract(epoch FROM(clock_timestamp()+interval '10 minutes'))*1000)::BIGINT,
        'FH-'||upper(substr(replace(gen_random_uuid()::text,'-',''),1,10))) RETURNING * INTO result;
    UPDATE public.listings SET remaining_quantity=remaining_quantity-p_quantity,
        status=CASE WHEN remaining_quantity-p_quantity=0 THEN 'sold_out' ELSE status END,updated_at=NOW() WHERE id=l.id;
    IF points_used>0 THEN
        UPDATE public.profiles SET eco_points=eco_points-points_used WHERE id=auth.uid();
        INSERT INTO public.reward_redemptions(student_id,order_id,points_deducted,discount_amount)
        VALUES(auth.uid(),result.id,points_used,reward_discount);
    END IF;
    RETURN result;
END; $$;

REVOKE ALL ON FUNCTION public.reserve_listing(UUID,INT,BOOLEAN) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.reserve_listing(UUID,INT,BOOLEAN) TO authenticated;

-- These verification functions use pgcrypto too. Preserve their explicit,
-- restricted search path while making the extensions schema reachable.
ALTER FUNCTION public.issue_institution_verification(UUID,TEXT,TEXT)
    SET search_path = public, extensions;
ALTER FUNCTION public.confirm_institution_verification(TEXT,TEXT,TEXT,TEXT)
    SET search_path = public, extensions;
