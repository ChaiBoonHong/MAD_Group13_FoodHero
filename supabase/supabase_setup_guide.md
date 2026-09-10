# FoodHero Supabase deployment and verification

This repository is linked to the **FoodHero development project**
`qouifvxsnevpqzafkdbf` in `ap-southeast-1`. Treat that project reference as a
safety check: do not run a push or reset if `supabase projects list` reports a
different linked project.

## Prerequisites

1. Install and sign in to the Supabase CLI.
2. Start Docker Desktop and select the Linux containers engine.
3. From the repository root, verify the link:

   ```powershell
   supabase projects list
   supabase link --project-ref qouifvxsnevpqzafkdbf
   ```

Never commit the database password, service-role key, Resend key, or generated
files inside `supabase/.temp/`.

## Validate locally before deployment

`supabase/schema.sql` is the declarative schema configured in
`supabase/config.toml`. Run:

```powershell
supabase start
supabase db reset
supabase test db
supabase db lint --local
```

The pgTAP files in `supabase/tests/` check reference data, privileged RPC
permissions, and Storage/RLS policy structure. A failing test must be fixed or
documented; do not bypass the suite to make a live push.

Generate a reviewable migration from the declarative schema:

```powershell
supabase db diff -f foodhero_multicampus_authoritative_backend
supabase db reset
supabase test db
supabase db lint --local
```

Read the generated file under `supabase/migrations/`. Check destructive
statements, grants, RLS policies, `SECURITY DEFINER` functions, and reference
data before continuing.

## Deploy the database

Preview first, then deploy:

```powershell
supabase db push --dry-run
supabase db push
```

After the push, run the SQL verification queries in the Dashboard and repeat
the actor tests against the development project. A local pgTAP result alone
does not prove that the hosted project has the same configuration.

## Deploy institutional email verification

Set secrets without putting their values in source control:

```powershell
supabase secrets set RESEND_API_KEY=YOUR_KEY VERIFICATION_FROM_EMAIL=YOUR_VERIFIED_SENDER
supabase functions deploy request-institution-verification
```

Confirm that the sender domain is verified in the selected email provider and
perform one real delivery test. The Android app must never receive the Resend
API key or a Supabase service-role key.

## Campus boundary evidence

Run the reproducible research collector:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/research-campus-boundaries.ps1
```

It writes `supabase/campus-boundary-research.json`. Each record separates:

- the official university page used to identify the intended campus; and
- the OpenStreetMap object used as candidate machine-readable geometry.

OpenStreetMap geometry is community-maintained ODbL data, not an official
university boundary. Import only candidates whose name, position, outline, and
official campus address have been visually reviewed. Records marked
`unresolved_no_polygon` must remain blocked; do not silently replace them with
the provisional 5 km radius.

The database boundary gate passes only when all 35 active campuses have a
reviewed polygon (at least three coordinate points) or the product owner has
documented approval for a specific service radius and its authoritative basis.

## RLS acceptance matrix

Before production, create disposable users and test at least these sessions:

| Actor | Required evidence |
|---|---|
| Anonymous | Cannot read private identity, receipt, QR, order, or notification data; cannot call commerce RPCs. |
| Student A / campus A | Can read and reserve campus A listings only. |
| Student B / campus B | Cannot read or reserve campus A listings or orders. |
| Merchant A / campus A | Can manage own listings, review own order receipts, and complete own pickups only. |
| Merchant B / campus B | Cannot access merchant A inventory, receipts, orders, or notifications. |
| Dual-role user | Can switch active role without gaining another account's or campus's privileges. |

Also test malformed JWT claims, direct table updates, wrong-campus RPC payloads,
pickup replay, duplicate cancellation/rejection, and private Storage URLs.

## Development reset record

On 11 September 2026, after confirming project `qouifvxsnevpqzafkdbf`, all
existing Auth users and user-generated rows were deleted. Storage contained no
objects. Reference service areas and landmarks were preserved. The reset does
not deploy the new schema and is never run by the app, APK installation, or a
normal schema deployment.
