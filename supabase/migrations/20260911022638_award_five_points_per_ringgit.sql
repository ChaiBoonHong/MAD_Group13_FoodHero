-- Award five Eco-Points for every RM1 actually paid on future completed orders.
-- Existing balances and completed orders are intentionally left unchanged.
CREATE OR REPLACE FUNCTION public.process_order_status_change()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=public,pg_temp AS $$
DECLARE listing_rec public.listings%ROWTYPE; earned_pts INT; saved_amt NUMERIC(10,2); co2_amt NUMERIC(10,2);
BEGIN
    IF OLD.status<>'completed' AND NEW.status='completed' THEN
        SELECT * INTO listing_rec FROM public.listings WHERE id=NEW.listing_id;
        earned_pts:=FLOOR(GREATEST(NEW.final_paid_price,0)*5)::INT;
        saved_amt:=NEW.total_original_price-NEW.final_paid_price;
        co2_amt:=COALESCE(listing_rec.co2_kg_per_item,1.20)*NEW.quantity;
        UPDATE public.profiles SET eco_points=eco_points+earned_pts,meals_rescued=meals_rescued+NEW.quantity,
            money_saved=money_saved+saved_amt,co2_prevented=co2_prevented+co2_amt,updated_at=NOW() WHERE id=NEW.student_id;
        INSERT INTO public.notifications(recipient_id,recipient_role,campus_id,title,message,event_type,related_order_id)
        VALUES(NEW.student_id,'student',NEW.campus_id,'Pickup completed',
            'You earned ' || earned_pts || ' Eco-Points at RM1 = 5 points.','order_completed',NEW.id);
    END IF;
    IF OLD.status IN('awaiting_payment','pending_verification','reserved','ready_for_pickup')
       AND NEW.status IN('cancelled','expired','rejected','payment_rejected') THEN
        UPDATE public.listings SET remaining_quantity=remaining_quantity+NEW.quantity,status='active',updated_at=NOW() WHERE id=NEW.listing_id;
        IF NEW.reward_points_used>0 THEN
            UPDATE public.profiles SET eco_points=eco_points+NEW.reward_points_used WHERE id=NEW.student_id;
            DELETE FROM public.reward_redemptions WHERE order_id=NEW.id;
        END IF;
        INSERT INTO public.notifications(recipient_id,recipient_role,campus_id,title,message,event_type,related_order_id)
        VALUES(NEW.student_id,'student',NEW.campus_id,
            CASE WHEN NEW.status='expired' THEN 'Order expired' WHEN NEW.status IN('rejected','payment_rejected') THEN 'Payment rejected' ELSE 'Order cancelled' END,
            CASE WHEN NEW.status IN('rejected','payment_rejected') THEN COALESCE(NEW.rejection_reason,'The merchant rejected the receipt.') ELSE 'Reserved stock has been released.' END,
            NEW.status::text,NEW.id);
    END IF;
    RETURN NEW;
END; $$;

REVOKE ALL ON FUNCTION public.process_order_status_change() FROM PUBLIC,anon,authenticated;
