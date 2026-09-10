begin;
select plan(13);

select has_function('public','reserve_listing',array['uuid','integer','boolean'],'reservation RPC exists');
select has_function('public','submit_payment_receipt',array['uuid','text'],'receipt RPC exists');
select has_function('public','decide_payment_receipt',array['uuid','boolean','text'],'review RPC exists');
select has_function('public','cancel_unpaid_order',array['uuid'],'cancellation RPC exists');
select has_function('public','expire_unpaid_order',array['uuid'],'expiry RPC exists');
select has_function('public','complete_pickup',array['text'],'pickup RPC exists');
select has_function('public','mark_no_show',array['uuid'],'no-show RPC exists');
select function_privs_are('public','reserve_listing',array['uuid','integer','boolean'],'authenticated',array['EXECUTE'],
  'authenticated may reserve through RPC');
select function_privs_are('public','complete_pickup',array['text'],'authenticated',array['EXECUTE'],
  'authenticated merchant may call pickup RPC');
select function_privs_are('public','reserve_listing',array['uuid','integer','boolean'],'anon',array[]::text[],
  'anonymous role cannot reserve');
select function_privs_are('public','complete_pickup',array['text'],'anon',array[]::text[],
  'anonymous role cannot complete pickup');
select results_eq($$select count(*)::bigint from information_schema.role_table_grants
  where table_schema='public' and table_name='orders' and grantee='authenticated'
    and privilege_type in ('INSERT','UPDATE','DELETE')$$,array[0::bigint],
  'authenticated clients cannot mutate orders directly');
select results_eq($$select count(*)::bigint from pg_proc p join pg_namespace n on n.oid=p.pronamespace
  where n.nspname='public' and p.prosecdef and has_function_privilege('anon',p.oid,'execute')
    and p.proname not in ('lookup_institution_domain')$$,array[0::bigint],
  'anonymous cannot execute privileged business functions');

select * from finish();
rollback;
