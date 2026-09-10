begin;
select plan(12);

select has_table('public','institutions','institutions exists');
select has_table('public','campuses','campuses exists');
select has_table('public','user_roles','user_roles exists');
select has_table('public','student_affiliations','student affiliations exists');
select has_column('public','profiles','last_active_role','last active role is persisted');
select has_column('public','profiles','campus_id','profiles are campus scoped');
select has_column('public','merchants','campus_id','merchants are campus scoped');
select has_column('public','listings','campus_id','listings are campus scoped');
select has_column('public','orders','campus_id','orders are campus scoped');
select results_eq('select count(*)::bigint from public.institutions where is_active',array[35::bigint],
  'all 35 institutions are active');
select results_eq('select count(*)::bigint from public.campuses where is_main and is_active',array[35::bigint],
  'one active main campus exists per institution');
select results_eq($$select count(*)::bigint from public.campuses
  where is_main and is_active and jsonb_array_length(boundary_coordinates)>=3$$,array[35::bigint],
  'every main campus has a reviewable polygon boundary');

select * from finish();
rollback;
