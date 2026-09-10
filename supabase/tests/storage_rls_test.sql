begin;
select plan(8);

select results_eq($$select count(*)::bigint from storage.buckets where id='listing-images' and public$$,
  array[1::bigint],'listing images bucket is public');
select results_eq($$select count(*)::bigint from storage.buckets where id='payment-receipts' and not public$$,
  array[1::bigint],'payment receipts bucket is private');
select results_eq($$select count(*)::bigint from storage.buckets where id='merchant-payment-qrs' and not public$$,
  array[1::bigint],'merchant QR bucket is private');
select results_eq($$select count(*)::bigint from pg_policies where schemaname='storage' and tablename='objects'
  and policyname='Order participants read payment receipts'$$,array[1::bigint],
  'receipt participant policy exists');
select results_eq($$select count(*)::bigint from pg_policies where schemaname='storage' and tablename='objects'
  and policyname='Order participants read DuitNow QR'$$,array[1::bigint],
  'DuitNow participant policy exists');
select results_eq($$select count(*)::bigint from pg_policies where schemaname='storage' and tablename='objects'
  and cmd='SELECT' and roles @> array['anon'::name] and qual like '%payment-receipts%'$$,array[0::bigint],
  'anonymous cannot read receipts');
select results_eq($$select count(*)::bigint from pg_policies where schemaname='storage' and tablename='objects'
  and cmd='SELECT' and roles @> array['anon'::name] and qual like '%merchant-payment-qrs%'$$,array[0::bigint],
  'anonymous cannot read merchant QR files');
select results_eq($$select count(*)::bigint from storage.objects$$,array[0::bigint],
  'post-reset Storage is empty');

select * from finish();
rollback;
