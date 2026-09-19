drop policy if exists "Block direct student allowlist access" on public.student_allowlist;
create policy "Block direct student allowlist access"
on public.student_allowlist for all to anon, authenticated
using (false) with check (false);

drop policy if exists "Block direct merchant allowlist access" on public.merchant_allowlist;
create policy "Block direct merchant allowlist access"
on public.merchant_allowlist for all to anon, authenticated
using (false) with check (false);

drop policy if exists "Students read own reward redemptions" on public.reward_redemptions;
create policy "Students read own reward redemptions"
on public.reward_redemptions for select to authenticated
using (student_id = (select auth.uid()));

drop policy if exists "Users read own location" on public.user_locations;
create policy "Users read own location"
on public.user_locations for select to authenticated
using (student_id = (select auth.uid()));

drop policy if exists "Users insert own location" on public.user_locations;
create policy "Users insert own location"
on public.user_locations for insert to authenticated
with check (student_id = (select auth.uid()));

drop policy if exists "Users update own location" on public.user_locations;
create policy "Users update own location"
on public.user_locations for update to authenticated
using (student_id = (select auth.uid()))
with check (student_id = (select auth.uid()));

drop policy if exists "Users delete own location" on public.user_locations;
create policy "Users delete own location"
on public.user_locations for delete to authenticated
using (student_id = (select auth.uid()));

alter function public.process_order_completion() set search_path = public, pg_temp;
alter function public.process_order_reservation() set search_path = public, pg_temp;
alter function public.process_order_status_change() set search_path = public, pg_temp;
alter function public.process_review_submission() set search_path = public, pg_temp;

revoke all on function public.handle_new_user() from public, anon, authenticated;
revoke all on function public.rls_auto_enable() from public, anon, authenticated;;
