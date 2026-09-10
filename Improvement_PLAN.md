# FoodHero End-to-End Implementation Plan

## Summary

Complete the student and merchant flows using Supabase as the authoritative backend:

- Support multiple institutions, with one main campus configured for each institution.
- Isolate profiles, merchants, listings, orders, and notifications by campus.
- Require verified institutional identity for Student access.
- Allow Merchant registration with any verified email.
- Automatically approve a merchant after email verification and completion of all required merchant fields.
- Allow one account to hold Student and Merchant roles.
- Remember the account’s last-used role and provide “Switch Role” in both profile screens.
- Finish the order lifecycle from reservation through DuitNow verification and QR pickup.
- After implementation and acceptance testing, clear all user-generated Supabase data, Storage objects, and Auth users while preserving schema and reference configuration.

## Implementation changes

### 1. Institution, campus, and multi-role identity

Introduce these authoritative records:

- `institutions`: code and name.
- `institution_email_domains`: exact domain, institution, and `student`, `staff`, or `member` affiliation.
- `campuses`: institution, main-campus name, address, latitude, longitude, service boundary, and active status.
- `user_roles`: user ID plus `student` or `merchant`; replace the single-role assumption.
- `student_affiliations`: user ID, verified institutional email, institution, campus, affiliation, verification status, and verification timestamp.
- `profiles.last_active_role`: controls post-login routing.

Seed every supplied email domain and one verified main campus for each institution. Campus coordinates and boundaries must come from official institution addresses and checked map coordinates, not invented values.

Use exact normalized domain matching after `@`. Shared domains such as UTP and Nottingham become `member`.

### 2. Registration and login

Student registration:

1. Collect name, institutional email, password, student ID where applicable, and faculty.
2. Check the exact email domain against the active registry.
3. Create the Supabase Auth account.
4. Require email confirmation before workspace access.
5. Assign institution, main campus, and affiliation server-side.
6. Add the Student role and open the Student workspace.
7. Show campus-filtered listings or the no-listings state.

Merchant registration:

1. Collect owner name, verified login email, business name, description, phone, DuitNow display name, stall details, terms acceptance, and selected campus.
2. Upload the actual DuitNow QR to Supabase Storage.
3. Let the merchant position a map marker and confirm the address.
4. Validate the marker against the selected campus boundary.
5. Create the Merchant role and merchant record only after all fields and email verification succeed.
6. Set the merchant to `approved` automatically.
7. Open the Merchant workspace.

Dual-role behavior:

- An existing Student may add Merchant from Profile without creating another Auth user.
- A Merchant with an institutional primary email may add Student after institutional verification.
- A Merchant using a personal/business login email may add Student by verifying a secondary institutional email through a protected verification function.
- Store only a hashed, expiring verification code; enforce retry and resend limits.
- Remember the last-used role and expose “Switch Role” in both workspaces.
- If the user selected the wrong registration role, offer “Add/Switch to Student” or “Add/Switch to Merchant”; do not create a duplicate Auth account.

### 3. Campus-isolated marketplace

Add `campus_id` to merchants, listings, orders, service areas, landmarks, and relevant notifications.

Enforce:

- Students only see listings from their verified campus.
- Merchants only publish at their registered campus and pinned location.
- Listings inherit campus and coordinates from the merchant; the listing form cannot forge another campus.
- Orders require the student, merchant, and listing to share the same campus.
- RLS prevents cross-campus and cross-account data access.
- Existing UTAR-only constants and coordinate fallbacks are removed from domain logic.

### 4. DuitNow and order lifecycle

Use these states:

```text
AWAITING_PAYMENT
→ PENDING_VERIFICATION
→ READY_FOR_PICKUP
→ COMPLETED
```

Terminal alternatives:

```text
CANCELLED
EXPIRED
PAYMENT_REJECTED
NO_SHOW
```

Rules:

- `reserve_listing` atomically locks the listing, validates campus and quantity, reduces stock, creates the order, and assigns a server expiry.
- The payment screen loads the merchant’s actual DuitNow QR and shows merchant, amount, payment reference, and countdown.
- Provide Save to Gallery and Android Sharesheet actions.
- Upload a real receipt to private Storage before changing the order to `PENDING_VERIFICATION`.
- Only the associated merchant can view the receipt.
- Approval changes the order to `READY_FOR_PICKUP`.
- Rejection requires a reason, changes it to `PAYMENT_REJECTED`, restores stock once, and notifies the student.
- Expired or student-cancelled unpaid orders restore stock once.
- A ready order not collected before the pickup deadline becomes `NO_SHOW`; no refund is issued and stock is not restored.
- Pickup tokens are opaque, high-entropy, single-use values.
- Only the associated merchant can complete pickup.
- Repeated completion, rejection, cancellation, expiry, or stock restoration requests are idempotent.
- Impact totals and merchant revenue update only after the first valid transition to `COMPLETED`.

### 5. UX and failure handling

Every remote screen supports:

```text
Idle → Loading → Content / Empty / Error → Retry
```

Apply these behaviors:

- Disable primary actions during requests.
- Preserve entered information after recoverable failures.
- Never show success until Supabase confirms the mutation.
- Clearly distinguish awaiting payment, under review, rejected, ready, completed, expired, cancelled, and no-show orders.
- Display merchant rejection reasons and the next allowed action.
- Provide a manual pickup-code fallback when camera scanning is unavailable.
- Keep the approved Eco-Vibrant theme, 48dp targets, safe insets, keyboard-safe forms, and accessible text.
- Verify compact screens, 200% font scale, TalkBack, broken images, denied permissions, lost connectivity, and process recreation.

### 6. Existing implementation reconciliation

Preserve current user-owned changes and consolidate the partially implemented features rather than creating parallel flows:

- Replace the single `profiles.role` routing assumption with `user_roles` and `last_active_role`.
- Update current confirmed-email checks to support primary and secondary institutional verification.
- Change existing merchant approval gating to automatic approval only after the complete registration transaction succeeds.
- Replace UTAR coordinate defaults with campus records.
- Rename existing `RESERVED` behavior to `READY_FOR_PICKUP` through a compatible migration.
- Change unknown order-status parsing from a successful default to a fail-closed unknown/error path.
- Keep Supabase Storage for listing photos, receipts, and merchant DuitNow QR images.
- Preserve external HTTPS listing images as the existing optional alternative.

## Public interfaces and data contracts

Add or revise repository operations:

- `registerStudent(...)`
- `registerMerchant(...)`
- `addStudentRole(...)`
- `addMerchantRole(...)`
- `requestInstitutionalEmailVerification(...)`
- `confirmInstitutionalEmailVerification(...)`
- `getAvailableRoles(userId)`
- `switchActiveRole(role)`
- `getInstitutionsAndCampuses()`
- `reserveListing(listingId, quantity, useRewards)`
- `uploadPaymentReceipt(orderId, image)`
- `reviewPayment(orderId, approved, rejectionReason)`
- `completePickup(token)`
- `markNoShow(orderId)`

All commerce mutations call guarded Supabase functions. Android never directly sets trusted role, institution, campus, merchant approval, stock, reward totals, revenue, or terminal order status.

## Test plan

### Authentication and roles

- Accept every configured domain using exact case-insensitive matching.
- Reject unknown, malformed, deceptive suffix, and inactive domains.
- Block unconfirmed primary and secondary emails.
- Confirm Student, Merchant, and dual-role routing.
- Verify last-used role persistence and both Switch Role paths.
- Confirm personal-email merchants cannot add Student without secondary institutional verification.
- Confirm duplicate role addition is idempotent.

### Campus isolation

- Verify one main campus exists for every institution.
- Confirm student A cannot see or reserve campus B listings.
- Confirm a merchant cannot publish outside its selected campus.
- Reject cross-campus order payloads at the database level.
- Test boundary edges and invalid coordinates.

### Ordering and payment

- Test valid, zero, negative, excessive, simultaneous, and sold-out quantities.
- Test receipt upload success, failure, retry, wrong file type, and expired payment window.
- Test merchant approval and rejection with mandatory reason.
- Confirm stock is restored exactly once for rejection, cancellation, and expiry.
- Confirm no-show does not restore stock or trigger a refund.
- Confirm receipt and DuitNow QR access is limited to appropriate participants.

### Pickup

- Test valid scan, manual code, malformed token, wrong merchant, expired order, cancelled order, no-show order, and replay.
- Confirm only one transition to `COMPLETED`.
- Confirm impact and revenue update once.

### Quality gates

- Unit and repository tests pass.
- Supabase migration and RLS tests pass for anonymous, two students, two merchants, dual-role users, and cross-campus access.
- `assembleDebug`, Android lint, and `git diff --check` pass.
- Two physical phones complete registration, payment-app handoff, receipt review, notification, and pickup.
- Final screens are compared with the approved prototype.

## Post-implementation Supabase reset

Run the reset only after screenshots, videos, logs, and acceptance results have been captured.

The reset must:

- Confirm the exact development/demo Supabase project ID.
- Delete orders, reviews, reward records, notifications, listings, merchants, profiles, role memberships, affiliations, and other user-generated rows in foreign-key-safe order.
- Delete all listing images, payment receipts, and merchant DuitNow QR objects.
- Delete every Supabase Auth test user through an authorized server-side or Dashboard operation.
- Preserve institutions, institutional domains, main campuses, service areas, landmarks, Storage buckets, RLS policies, functions, triggers, and migrations.
- Re-run empty-state and fresh Student/Merchant registration checks after reset.
- Never execute automatically during app startup, APK installation, or normal schema deployment.

## Assumptions and locked defaults

- Multi-campus isolation is required.
- Every listed institution receives one main campus in the initial dataset.
- Merchant registration accepts any verified email and is automatically approved after complete registration.
- Student access always requires verified institutional identity.
- Personal-email merchants use secondary institutional verification to add Student.
- Dual-role accounts remember their last-used role and include Switch Role.
- Paid pickup expiry becomes `NO_SHOW` with no refund.
- Supabase Dashboard/service-side tooling performs the final destructive reset.
- Existing dirty worktree changes are preserved and reviewed before implementation.
