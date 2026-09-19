insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values
 ('listing-images','listing-images',true,5242880,array['image/jpeg','image/png','image/webp']),
 ('payment-receipts','payment-receipts',false,5242880,array['image/jpeg','image/png','image/webp']),
 ('merchant-payment-qrs','merchant-payment-qrs',false,5242880,array['image/jpeg','image/png','image/webp'])
on conflict(id) do update set
 public=excluded.public,file_size_limit=excluded.file_size_limit,allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists "Public read listing images" on storage.objects;
create policy "Public read listing images" on storage.objects for select
to anon,authenticated using(bucket_id='listing-images');

drop policy if exists "Users upload own listing images" on storage.objects;
create policy "Users upload own listing images" on storage.objects for insert
to authenticated with check(bucket_id='listing-images' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists "Users update own listing images" on storage.objects;
create policy "Users update own listing images" on storage.objects for update
to authenticated using(bucket_id='listing-images' and owner_id=(select auth.uid())::text)
with check(bucket_id='listing-images' and owner_id=(select auth.uid())::text);
drop policy if exists "Users delete own listing images" on storage.objects;
create policy "Users delete own listing images" on storage.objects for delete
to authenticated using(bucket_id='listing-images' and owner_id=(select auth.uid())::text);

drop policy if exists "Students upload own payment receipts" on storage.objects;
create policy "Students upload own payment receipts" on storage.objects for insert
to authenticated with check(bucket_id='payment-receipts' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists "Owners read own payment receipts" on storage.objects;
create policy "Owners read own payment receipts" on storage.objects for select
to authenticated using(bucket_id='payment-receipts' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists "Owners update own payment receipts" on storage.objects;
create policy "Owners update own payment receipts" on storage.objects for update
to authenticated using(bucket_id='payment-receipts' and owner_id=(select auth.uid())::text)
with check(bucket_id='payment-receipts' and owner_id=(select auth.uid())::text);
drop policy if exists "Owners delete own payment receipts" on storage.objects;
create policy "Owners delete own payment receipts" on storage.objects for delete
to authenticated using(bucket_id='payment-receipts' and owner_id=(select auth.uid())::text);

drop policy if exists "Merchants upload own DuitNow QR" on storage.objects;
create policy "Merchants upload own DuitNow QR" on storage.objects for insert
to authenticated with check(bucket_id='merchant-payment-qrs' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists "Owners read own DuitNow QR" on storage.objects;
create policy "Owners read own DuitNow QR" on storage.objects for select
to authenticated using(bucket_id='merchant-payment-qrs' and (storage.foldername(name))[1]=(select auth.uid())::text);
drop policy if exists "Owners update own DuitNow QR" on storage.objects;
create policy "Owners update own DuitNow QR" on storage.objects for update
to authenticated using(bucket_id='merchant-payment-qrs' and owner_id=(select auth.uid())::text)
with check(bucket_id='merchant-payment-qrs' and owner_id=(select auth.uid())::text);
drop policy if exists "Owners delete own DuitNow QR" on storage.objects;
create policy "Owners delete own DuitNow QR" on storage.objects for delete
to authenticated using(bucket_id='merchant-payment-qrs' and owner_id=(select auth.uid())::text);;
