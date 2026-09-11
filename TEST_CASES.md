# FoodHero Complete Test Catalogue

This catalogue tests the Android app and its authoritative Supabase backend. Use only the development/demo project. Do not use production accounts, real payments, or personal receipts.

## Status and evidence

Record each case as `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`. Attach the relevant screenshot/video, Logcat excerpt, Supabase Function log, or SQL result. A screen looking correct is not proof that its database mutation succeeded.

Current automated baseline (11 September 2026):

- `testDebugUnitTest --rerun-tasks`: PASS.
- `assembleDebug`: PASS.
- `lintDebug`: PASS.
- Edge Functions `request-institution-verification` and `google-routes`: deployed with JWT verification.
- Reference data: 35 active institutions, 75 domains, and 35 active main campuses.
- Campus boundaries: BLOCKED — 0 of 35 campuses have reviewed polygon coordinates. Merchant activation must fail closed until this is corrected.
- Physical-device, real email delivery, payment-app handoff, camera, and accessibility cases: NOT RUN.

## Test environment

Prepare two institutions/campuses and these users without sharing credentials in source control:

1. Student A at campus A.
2. Student B at campus B.
3. Merchant A at campus A with a real test DuitNow QR.
4. Merchant B at campus B with a real test DuitNow QR.
5. A dual-role user at campus A.
6. An unconfirmed user and a personal-email merchant.

Use two physical Android phones where a case involves notifications, bank/e-wallet handoff, camera pickup, permissions, or process recreation. First configure reviewed campus polygons for the test campuses.

## Authentication and institutional identity

| ID | Test | Steps | Expected result |
|---|---|---|---|
| AUTH-01 | Valid Student registration | Register with each supported Student domain, mixed-case email, confirm email, sign in | Exact normalized domain is accepted; institution and main campus are server assigned; Student workspace opens |
| AUTH-02 | Staff/member domain | Register using staff and shared-member domains | Registry affiliation matches the configured domain; no client-selected institution is trusted |
| AUTH-03 | Unknown domain | Register with an unlisted domain | Registration is blocked with a clear supported-email error |
| AUTH-04 | Deceptive suffix | Try `user@1utar.my.attacker.com` and `user@fakeutar.edu.my` | Both are rejected; suffix/substring matching is never used |
| AUTH-05 | Malformed fields | Try blank name, invalid email, short password, blank Student ID/faculty | Inline error appears; values remain; no role/profile is created |
| AUTH-06 | Unconfirmed email | Create account but do not confirm it | Workspace access is blocked and resend guidance is shown |
| AUTH-07 | Login failures | Try wrong password, unknown email, offline mode, expired session | No success navigation; actionable error/retry is shown |
| AUTH-08 | Login routing | Sign in as Student-only, Merchant-only, and dual-role users | Single role routes correctly; dual role routes to persisted `last_active_role` |
| AUTH-09 | Logout | Log out, relaunch, press Back | Tokens and local session are cleared; protected screens cannot reappear |
| AUTH-10 | Duplicate registration | Register an existing email under the other role | App offers role signup on the same Auth user; it does not create a duplicate user |

## Roles and Student verification

| ID | Test | Steps | Expected result |
|---|---|---|---|
| ROLE-01 | Student-only profile | Open Student profile | Action says `Sign up as Merchant` only after role lookup succeeds |
| ROLE-02 | Merchant-only profile | Open Merchant profile | Action says `Sign up as Student` only after role lookup succeeds |
| ROLE-03 | Dual-role profiles | Open both profiles | Actions say `Switch to Merchant` / `Switch to Student` |
| ROLE-04 | Role lookup loading/error | Slow or block the role request, then retry | Disabled loading label appears; failure says `Retry account access`; missing role is never inferred from failure |
| ROLE-05 | Switch role | Switch both directions | Server confirms `last_active_role`; old task is cleared; correct workspace opens |
| ROLE-06 | Switch failure | Disconnect during switch | Current workspace remains and button re-enables; no false success |
| ROLE-07 | Relaunch persistence | Switch role, kill app, relaunch | Last confirmed role opens |
| ROLE-08 | Send verification code | Personal-email merchant enters valid institutional email | Code is sent through the protected function; no code is returned to Android/logs |
| ROLE-09 | Invalid verification email | Use malformed, unsupported, or deceptive-suffix email | No challenge/email is issued |
| ROLE-10 | Code validation | Enter wrong, expired, malformed, and correct codes | Attempts/expiry are enforced; only the correct active code adds Student |
| ROLE-11 | Resend limits | Resend immediately and after countdown | Early request is rate-limited; allowed resend invalidates/replaces the earlier challenge |
| ROLE-12 | Delivery-provider failure | Make Resend reject delivery, then retry | Error is shown; undelivered challenge is deleted and does not consume the resend window |
| ROLE-13 | Student identity fields | Verify with blank/invalid Student ID or faculty | Server rejects incomplete identity; no Student role is added |
| ROLE-14 | Idempotent role addition | Submit the successful confirmation twice | One role/affiliation exists; no duplicate rows or duplicate navigation |

## Merchant setup, Google Map, and campus boundary

| ID | Test | Steps | Expected result |
|---|---|---|---|
| MER-01 | Three-step state | Fill each step, go Back/forward, rotate, recreate process | Step, fields, QR URI, campus, camera, and pin are preserved |
| MER-02 | Business validation | Test blank/long business name, description, malformed phone | Inline errors block Continue; entered values remain |
| MER-03 | Real QR required | Continue without QR or with placeholder graphic | Activation is blocked; placeholder is never presented as an uploaded QR |
| MER-04 | QR validation | Select valid PNG/JPEG, wrong type, corrupt, and oversized file | Only valid file is previewed/uploaded; error is actionable |
| MER-05 | QR replace/remove | Upload, replace, remove, and retry failed upload | Preview/status matches the actual selection; no duplicate orphan upload is created on retry |
| MER-06 | Campus dropdown | Load campuses, disconnect, retry, select another campus | Supabase values are used; error/retry works; camera moves to selected campus |
| MER-07 | Map interaction | Pinch, native zoom buttons, compass, tap, drag marker | Camera and draggable pin respond smoothly; coordinates update; pin gives short feedback |
| MER-07A | Pan after pinning | Place a pin, then drag on empty map and pinch with two fingers | Parent form does not steal the gesture; map pans/zooms while the pin stays at its geographic coordinate |
| MER-08 | Locate me allowed | Grant location and tap Locate me | Map centers on current fix at a useful zoom without silently changing the merchant pin |
| MER-09 | Location denied/unavailable | Deny permission, disable GPS, return null last location | Manual map placement remains available with clear guidance |
| MER-10 | Boundary inside/edge/outside | Submit points clearly inside, on edge, and outside reviewed polygon | Server authoritatively accepts inside/edge per policy and rejects outside |
| MER-11 | Missing boundary | Submit against a campus with an empty boundary | Server fails closed with reviewed-boundary message; no Merchant role/status approval occurs |
| MER-12 | Forged campus/coordinates | Intercept request and substitute another campus or invalid latitude/longitude | RPC rejects it; client values cannot bypass server validation |
| MER-13 | Double activation | Double-tap Activate and retry after timeout | One transaction/merchant role is created; button prevents concurrent submission |
| MER-14 | Completion | Complete all fields with verified email, real QR, valid pin, terms | Supabase confirms approved merchant, success transition plays once, Merchant workspace opens |
| MER-15 | Edit complete profile | From Merchant profile edit business name, description, phone, DuitNow name, QR, campus, and pickup pin | Current values and secure QR preview load; replacement is validated/uploaded; server confirms all changes before success |
| MER-16 | Edit without replacing QR | Change text/location while retaining current QR | Existing private QR path is retained and no duplicate Storage object is uploaded |
| MER-17 | Edit failure/invalid boundary | Disconnect or select an outside/missing-boundary pin while saving | Existing merchant data remains unchanged; form values remain for correction/retry |

## Listings and campus marketplace

| ID | Test | Steps | Expected result |
|---|---|---|---|
| LIST-01 | Empty/loading/error | Open campus feed with no listings, slow network, and failure | Correct loading, empty, error, and Retry states appear |
| LIST-02 | Campus isolation | Student A/B open feed/map | Each sees only their campus listings |
| LIST-03 | Create listing | Merchant enters valid listing and real image | Listing inherits merchant campus/coordinates; Supabase confirmation precedes success |
| LIST-04 | Invalid listing | Test blank title, invalid prices, zero/negative stock, bad times | Inline validation blocks submission |
| LIST-05 | Price ceiling | Original and discounted prices around allowed limits | Invalid pricing is rejected consistently by app/server |
| LIST-06 | Image sources | Test owned Storage image and valid external HTTPS URL | Both supported real sources display; HTTP/invalid URL is rejected or fails visibly |
| LIST-07 | Broken image | Remove/deny an image | Stable fallback appears; list does not crash or show fake image |
| LIST-08 | Edit/delete ownership | Merchant A edits/deletes own and Merchant B’s listing | Own mutation succeeds; cross-account mutation is denied by RLS |
| LIST-09 | Merchant campus forgery | Change `campus_id` or coordinates in payload | Database ignores/rejects forged scope and uses merchant location |
| LIST-10 | Expired/sold-out visibility | Exhaust stock and pass collection/listing expiry | Listing is no longer reservable; UI status matches backend |

## Reservation and payment

| ID | Test | Steps | Expected result |
|---|---|---|---|
| ORD-01 | Valid reservation | Student selects available quantity | Atomic RPC reduces stock once and creates `AWAITING_PAYMENT` with server expiry |
| ORD-02 | Invalid quantities | Submit zero, negative, non-number, and above-stock values | All fail without changing stock/order count |
| ORD-03 | Concurrent last stock | Two students reserve the last unit simultaneously | Exactly one succeeds; stock never becomes negative |
| ORD-04 | Cross-campus reservation | Student A calls reserve for campus B listing | Database rejects request |
| ORD-05 | Reward option | Reserve with insufficient/sufficient rewards | Server validates use; totals remain consistent and cannot be client-forged |
| PAY-01 | Payment display | Open awaiting-payment order | Real associated merchant QR, name, exact amount/reference, and server countdown appear |
| PAY-02 | QR privacy | Unrelated authenticated user and anonymous user request QR | Access is denied; participant access works |
| PAY-03 | Save/share | Save QR and use Android Sharesheet with allowed/denied permission | Action succeeds or gives honest recovery; no false saved message |
| PAY-04 | Receipt validation | Choose real image, wrong type, corrupt, and oversized file | Only allowed image uploads; invalid file leaves order unchanged |
| PAY-05 | Receipt upload retry | Interrupt upload and retry/recreate Activity | Same completed object/path is reused safely; no duplicate order transition |
| PAY-06 | Receipt path forgery | Submit another user/order’s path or nonexistent object | RPC rejects it |
| PAY-07 | Valid submission | Upload receipt before expiry | Order becomes `PENDING_VERIFICATION` only after Storage and RPC both succeed |
| PAY-08 | Expired submission | Upload after server expiry | Submission fails; order expires and stock is restored once |
| PAY-09 | Student cancellation | Cancel unpaid order repeatedly/concurrently | Order becomes `CANCELLED`; stock restores exactly once |
| PAY-10 | Merchant approval | Associated merchant approves pending receipt | Order becomes `READY_FOR_PICKUP`; Student is notified |
| PAY-11 | Merchant rejection | Reject with blank reason, then valid reason | Blank reason is blocked; valid rejection becomes `PAYMENT_REJECTED`, restores stock once, and notifies reason |
| PAY-12 | Wrong merchant review | Merchant B attempts to view/approve/reject Merchant A receipt | Storage/RPC access is denied |
| PAY-13 | Review idempotency | Repeat/concurrently approve or reject | Only first valid transition applies; no duplicate restoration/notification |
| PAY-14 | Unknown status | Return an unrecognized backend status to parser | UI fails closed to unknown/error; it never treats it as success |

## Pickup, no-show, impact, and reviews

| ID | Test | Steps | Expected result |
|---|---|---|---|
| PICK-01 | Valid QR pickup | Associated merchant scans ready-order token | Order becomes `COMPLETED` once; success shown after server response |
| PICK-02 | Manual fallback | Enter the visible pickup/order code without camera | Server resolves it to the opaque token and completes only the matching ready order |
| PICK-03 | Bad token/code | Scan malformed/random token and invalid manual code | Clear error; no order changes |
| PICK-04 | Wrong merchant | Merchant B scans Merchant A token | RPC rejects it |
| PICK-05 | Invalid state | Scan awaiting, pending, rejected, cancelled, expired, no-show token | Every attempt is rejected |
| PICK-06 | Replay/concurrency | Scan completed token again and simultaneously on two devices | One completion only; later calls are idempotent/rejected safely |
| PICK-07 | Token quality | Inspect generated tokens across many reservations | Values are opaque, high entropy, non-sequential, and not logged |
| PICK-08 | No-show | Pass pickup deadline and reconcile/mark no-show | Ready order becomes `NO_SHOW`; no refund and no stock restoration |
| IMP-01 | Impact/revenue once | Complete one order and replay completion | Student impact and merchant revenue increase exactly once |
| IMP-02 | Non-completed totals | Cancel, expire, reject, and no-show orders | None increases completed impact/revenue |
| REV-01 | Valid review | Student reviews own completed order once | Review is saved and merchant aggregate updates |
| REV-02 | Review authorization | Review another user’s/uncompleted/cross-campus order; submit twice | All invalid cases are denied; duplicate review is prevented |

## Notifications, Google Routes, lifecycle, and accessibility

| ID | Test | Steps | Expected result |
|---|---|---|---|
| NTF-01 | Transition notifications | Reserve, submit, approve/reject, ready, complete/no-show | Correct participant receives accurate status/reason without duplicates |
| NTF-02 | Read mutation failure | Tap unread notification offline, then retry | Optimistic read state rolls back on failure; Snackbar offers Retry |
| ROUTE-01 | Providers | Search runtime traffic/code and exercise map route | Only Google Maps and Google Routes are used; no OSM/OSRM requests |
| ROUTE-02 | Mode mapping | Request walking, cycling, shuttle | Edge payload uses `WALK`, `BICYCLE`, `DRIVE`; route polyline/distance/duration render |
| ROUTE-03 | Route auth/failure | Call without JWT, invalid coordinates, quota/network failure | Unauthorized/invalid calls fail; UI shows route error and never invents geometry/ETA |
| LIFE-01 | Process recreation | Kill/recreate during forms, reservation, upload, review, pickup | Confirmed state reloads; draft-safe state persists; no duplicate mutation |
| UX-01 | Remote-state contract | Exercise every remote screen under slow, empty, failed, recovered response | `Loading → Content/Empty/Error → Retry` is coherent; action disabled in flight |
| UX-02 | Font/compact/landscape | Test compact width, 200% font, keyboard, supported landscape | No clipped text/controls; scrolling and insets work |
| UX-03 | TalkBack | Traverse all screens | Focus order is logical; controls/images/statuses have useful descriptions |
| UX-04 | Touch/contrast | Measure interactive targets and inspect light/dark theme | Targets are at least 48dp; text/status contrast remains readable |
| UX-05 | Reduced motion | Disable system animations | Decorative entrances/bounces are skipped; functionality remains intact |
| UX-06 | Offline recovery | Lose/recover connectivity on each mutation | No fabricated success; input remains; retry is safe and idempotent |

## Supabase security and contract tests

Run the SQL tests in `supabase/tests` against a disposable local Supabase database:

```powershell
supabase start
supabase db reset
supabase test db
```

| ID | Test | Expected result |
|---|---|---|
| DB-01 | `identity_and_campus_rls_test.sql` | Schema/scoping assertions pass; boundary assertion intentionally fails until 35 reviewed polygons exist |
| DB-02 | `commerce_rpc_security_test.sql` | Required RPCs exist; anonymous commerce and direct order mutations are denied |
| DB-03 | `storage_rls_test.sql` | Listing bucket is public; receipts/QRs private; participant policies exist; post-reset objects empty |
| DB-04 | Two students/two merchants | Each actor reads only own/campus-authorized profiles, listings, orders, reviews, notifications, and files |
| DB-05 | Direct trusted-field writes | Clients try to set roles, campus, approval, stock, rewards, revenue, terminal status | Direct mutations are denied; guarded RPCs remain the only route |
| DB-06 | Function privileges | Inspect grants for `anon`, `authenticated`, and service roles | Only public domain lookup is anonymous; guarded business functions require authentication |
| DB-07 | Advisor scan | Run Supabase Security and Performance advisors | No unexpected public functions/RLS gaps; intentional warnings documented; leaked-password protection enabled before release |

## Release gate

Run from the repository root:

```powershell
.\gradlew.bat testDebugUnitTest --rerun-tasks
.\gradlew.bat assembleDebug lintDebug
git diff --check
```

Release/acceptance is blocked if any high-risk case fails, if campus boundaries are missing, if leaked-password protection remains disabled for production, or if real-device email/payment/camera/permission flows lack evidence. Capture evidence before any post-acceptance data reset.
