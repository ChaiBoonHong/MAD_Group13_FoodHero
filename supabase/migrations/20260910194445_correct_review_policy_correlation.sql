DROP POLICY IF EXISTS "Students insert own reviews" ON public.reviews;
CREATE POLICY "Students insert own reviews" ON public.reviews FOR INSERT TO authenticated WITH CHECK (
 student_id=(SELECT auth.uid()) AND EXISTS (
  SELECT 1 FROM public.orders o WHERE o.id=reviews.order_id
   AND o.student_id=(SELECT auth.uid()) AND o.status='completed'
   AND o.merchant_id=reviews.merchant_id AND o.listing_id=reviews.listing_id));;
