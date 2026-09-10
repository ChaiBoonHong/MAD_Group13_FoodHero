DROP POLICY IF EXISTS "Public insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Students can insert reviews" ON public.reviews;
DROP POLICY IF EXISTS "Students can insert review for own order" ON public.reviews;
DROP POLICY IF EXISTS "Students insert own reviews" ON public.reviews;
CREATE POLICY "Students insert own reviews" ON public.reviews FOR INSERT TO authenticated WITH CHECK (
    student_id = (SELECT auth.uid()) AND EXISTS (
      SELECT 1 FROM public.orders o WHERE o.id=order_id AND o.student_id=(SELECT auth.uid())
        AND o.status='completed' AND o.merchant_id=reviews.merchant_id AND o.listing_id=reviews.listing_id));

CREATE INDEX IF NOT EXISTS idx_notifications_related_listing ON public.notifications(related_listing_id);
CREATE INDEX IF NOT EXISTS idx_notifications_related_order ON public.notifications(related_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_listing ON public.orders(listing_id);
CREATE INDEX IF NOT EXISTS idx_reviews_listing ON public.reviews(listing_id);
CREATE INDEX IF NOT EXISTS idx_reviews_student ON public.reviews(student_id);
CREATE INDEX IF NOT EXISTS idx_reward_redemptions_order ON public.reward_redemptions(order_id);
CREATE INDEX IF NOT EXISTS idx_reward_redemptions_student ON public.reward_redemptions(student_id);
