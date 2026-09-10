# FoodHero Login-Led Whole-App UI/UX Redesign

## Summary

Use the existing Login/Register page as the authoritative visual standard for all 35 application layouts. Preserve its polished Eco‑Vibrant identity while extending the same typography, spacing, cards, inputs, buttons, role selector, flat surfaces, and restrained motion throughout Student, Merchant, payment, ordering, profile, and setup flows.

Merchant Setup and role signup remain the largest structural improvements. Existing business flows, Google Maps, Google Routes, Supabase Storage, campus isolation, and navigation remain authoritative.

## Login Page Design Standard

- FoodHero logo in a white bordered 64dp card with deep green as the primary identity.
- Roboto Slab for page titles and important headings; Karla for body text and controls.
- Soft background canvas, flat white bento cards, 20–24dp corners, and subtle 1dp borders.
- Outlined 12dp Material fields with floating labels, inline errors, and visible focus.
- Full-pill segmented selection matching the Student/Merchant selector.
- Green primary, green outlined secondary, and red outlined destructive actions, all at least 48dp high.
- An 8dp spacing grid, 20dp page padding, and 16–20dp card padding.
- Compact branded header, clear title, short supporting copy, grouped card, and one obvious primary action.
- Safe insets, keyboard-safe scrolling, compact-screen support, and no fixed-height text containers that clip at 200% font scale.

## Implementation Changes

### 1. Shared visual and motion system

- Standardize typography, 8dp spacing, card corners, input appearance, button hierarchy, icons, empty/error states, app bars, dialogs, and bottom navigation across every screen and list item.
- Replace remaining plain `EditText`, `Spinner`, and platform `Button` controls with FoodHero-styled Material components.
- Move hardcoded UI text, dimensions, and colors into shared resources; preserve the current green Eco‑Vibrant identity and Karla typography.
- Replace success/error Toasts with anchored Material Snackbars where the user may need context or retry. Keep Toasts only for short non-actionable notices.
- Add reusable native motion:
  - 150ms button press/selection feedback.
  - 200ms loading/content/error crossfades.
  - 250ms screen and card entrances using fade plus small vertical translation.
  - Progress morphing inside primary buttons during mutations.
  - Short map-pin scale/bounce after placement.
  - Success check animation before role activation or completed setup.
- Disable decorative movement when `ValueAnimator.areAnimatorsEnabled()` is false and avoid repeated list-item animation after initial load.
- Preserve screen state through rotation/process recreation and prevent double submission while animations or requests are active.

### 2. Merchant Setup redesign

Replace the long unstructured form with a three-step full-screen flow and sticky bottom action:

1. **Business**
   - Business name, stall description, and contact phone in outlined labeled fields.
   - Inline validation with preserved input after recoverable failures.

2. **DuitNow**
   - DuitNow display name and private QR upload.
   - Compact upload card with real preview, Replace/Remove actions, file validation, and upload status.
   - Never show a decorative QR as though a real QR was selected.

3. **Pickup location and review**
   - Labeled campus dropdown loaded from Supabase.
   - Google Map centered on the selected campus.
   - “Locate me,” native zoom controls, pinch zoom, compass, tap-to-pin, and draggable pin.
   - Coordinates and selected campus shown in a confirmation card.
   - Validate the pin against the stored campus boundary/service rule; no client-only 5km assumption or invented coordinate fallback.
   - Final summary, terms confirmation, and “Activate Merchant Account.”

Animate step changes with a short horizontal fade/slide, animate map pin placement, and show a completion check before entering Merchant workspace. Preserve form, QR URI, selected campus, and pin through recreation.

### 3. Role-aware profile actions

Both profile screens must call `getAvailableRoles()` before displaying the cross-role action:

| Current account roles | Student profile action | Merchant profile action |
|---|---|---|
| Student only | **Sign up as Merchant** | Not applicable |
| Merchant only | Not applicable | **Sign up as Student** |
| Student + Merchant | **Switch to Merchant** | **Switch to Student** |
| Role lookup loading | Disabled “Checking account access…” | Disabled “Checking account access…” |
| Role lookup failed | “Retry account access” | “Retry account access” |

- Never infer that a role is missing from a failed switch request.
- “Switch” directly calls `switchActiveRole()` and routes only after Supabase confirms success.
- “Sign up” adds the role to the same Auth user; it never creates another account.
- **Sign up as Merchant** opens the redesigned Merchant Setup flow.
- **Sign up as Student** opens a dedicated full-screen institutional verification flow:
  1. Institutional email, Student ID, and faculty.
  2. Send-code loading/confirmation state.
  3. Six-digit verification with resend countdown, attempt feedback, and change-email action.
  4. Verified institution/campus summary and automatic activation of Student role.
- Replace the current raw email/code dialogs with this full-screen flow.
- After successful role addition, refresh available roles, persist `last_active_role`, show the success transition, clear the activity stack, and enter the new workspace.

### 4. Repository, database, and application-wide reconciliation

- Extend `addStudentRole(...)` and the protected confirmation RPC to accept and validate Student ID and faculty while assigning institution/campus server-side.
- Keep `addMerchantRole(...)` backed by the guarded merchant registration transaction and private QR Storage path.
- Introduce a reusable role-action state model: `LOADING`, `SWITCH_AVAILABLE`, `SIGNUP_REQUIRED`, `ERROR`.
- Apply shared loading/content/empty/error/retry transitions to authentication, feeds, maps, listings, orders, payment, pickup, reviews, notifications, impact, merchant dashboard, and both profiles.
- Remove remaining fake success behavior, including “saved locally” after failed remote mutations.
- Ensure all navigation transitions use the same subtle motion and that back navigation returns to the correct step/screen without losing entered data.
- Deploy the consolidated multi-role Supabase schema and seed institution/campus reference configuration before runtime acceptance; the current empty legacy backend cannot support role discovery or campus selection.

## Public Interfaces

Revise the Student role operation to carry complete identity information:

```java
addStudentRole(
    String institutionalEmail,
    String verificationCode,
    String studentId,
    String faculty,
    ResultCallback<Profile> callback
)
```

The protected Supabase confirmation function will accept equivalent parameters, validate the code/domain/campus, add the Student role idempotently, update the affiliation/profile, and set `last_active_role = student`.

Add UI-only contracts:

```text
RoleActionState:
LOADING | SWITCH_AVAILABLE | SIGNUP_REQUIRED | ERROR

RoleSignupResult:
role, profile, destination
```

Existing `getAvailableRoles()`, `switchActiveRole()`, `addMerchantRole()`, Google Maps, Google Routes, and Storage bucket contracts remain in use.

## Test Plan

- Verify Student-only, Merchant-only, dual-role, role-fetch failure, expired session, and duplicate role-addition states.
- Confirm button text is correct before interaction and never changes based merely on a failed switch.
- Confirm role addition reuses the current Auth user and role switching persists after relaunch.
- Test Student verification with valid, malformed, unsupported, deceptive-suffix, expired, wrong, rate-limited, and resent codes.
- Test Merchant Setup with missing fields, invalid phone, unreadable/oversized QR, denied location, unavailable GPS, manual/dragged pin, campus changes, invalid boundary, upload failure, retry, rotation, and double tap.
- Audit every screen at compact width, landscape where supported, 200% font scale, light/dark theme, keyboard open, offline state, broken images, TalkBack, and system animations disabled.
- Verify 48dp targets, contrast, focus order, content descriptions, no clipped text, and stable RecyclerView performance.
- Run unit/repository/UI tests, Supabase RLS/RPC tests, `assembleDebug`, Android lint, and `git diff --check`.
- Complete physical-device acceptance for role signup/switching, map/location permission, QR selection, process recreation, and reduced-motion behavior.

## Assumptions and Defaults

- “Whole application” means a consistency redesign of all existing screens, not a change to FoodHero’s product identity or navigation architecture.
- Motion is subtle and functional; no Lottie dependency or decorative looping animation will be introduced.
- Missing-role signup always adds access to the signed-in account and never creates a duplicate Auth user.
- Both-role accounts retain “Switch to Student/Merchant”; single-role accounts show “Sign up as Student/Merchant.”
- Merchant Setup and Student role signup are dedicated full-screen flows.
- Supabase remains authoritative for roles, verification, campus assignment, registration completion, and mutation success.
