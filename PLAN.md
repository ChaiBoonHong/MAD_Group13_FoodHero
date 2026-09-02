# FoodHero Java/Supabase Implementation Plan

## Summary

Build the complete FoodHero application described in the assignment guideline :codex-file-citation{path="D:\OneDrive\UCCD3223 Mobile Applications Development\UCCD3223 Assignment\Group Assignment\Group Assignment Guideline Part 1 & Part 2.pdf" purpose="source"} and proposal :codex-file-citation{path="D:\OneDrive\UCCD3223 Mobile Applications Development\UCCD3223 Assignment\Group Assignment\Proposal Report v3.pdf" purpose="source"}.

- Target project: `D:\UTAR\Group13-FoodHero\FoodHero`.
- Android code: Java 11 with XML layouts; no Kotlin and no Jetpack Compose.
- Backend: the existing active Supabase project named `FoodHero`.
- Cloud integration: Supabase Auth, PostgreSQL, REST Data API, Storage, Realtime, Edge Functions, RLS, and database triggers.
- Mapping: Google Maps SDK and Google Routes API, restricted to UTAR Kampar Campus only.
- Contribution split: Chai Boon Hong approximately 80%; Fong Chee Hou approximately 20%.
- Delivery model: sequential rather than parallel. Chai completes and merges the shared platform, backend contracts, student workflow, and merchant-facing service layer first. Fong branches only from that stable handoff and then implements the smaller merchant UI workflow using the completed contracts. Both contributors submit their own commits and pull requests; commit authorship must not be fabricated.
- Current baseline: only the Empty Views Activity exists. The Supabase project has no tables or migrations. The command-line Gradle baseline currently encounters an environment-level loopback connection error, so the first implementation checkpoint must also verify the project through Android Studio.

## Application Pages and Functions

| Page | User | Functions and implementation |
|---|---|---|
| Splash and session routing | Both roles | Read the encrypted saved session, refresh it through Supabase Auth, load the role from the protected `profiles` table, and route to the correct home page. |
| Role selection | New user | Choose Student or Merchant before registration; the chosen role is checked against the corresponding demo allowlist and is never trusted from editable JWT user metadata. |
| Register | Both roles | Email/password registration, name and role fields, student ID/faculty for students, business details for merchants, validation, loading, duplicate-account, and failure states. |
| Login and password reset | Both roles | Supabase password login, session persistence, logout, password-reset email, offline and invalid-credential handling. |
| Student Feed | Student | Active surplus bags, category chips, “Under RM5” filter, favourites, distance, discounted price, remaining quantity, merchant rating, pickup window, and live countdown. |
| Listing Details | Student | Food image loaded from either Supabase Storage or an external HTTPS photo URL, description, original/discount price, savings, CO2 value, merchant details, route preview, quantity selector, reward redemption, and Reserve button. Show the FoodHero placeholder when an image is missing, invalid, or temporarily unavailable. |
| UTAR Kampar Campus Map | Student | Open at the official UTAR Kampar centre (`4.336214, 101.142111`), restrict the camera and results to the stored campus boundary, and display only on-campus merchants, listings, pickup points, and landmarks. Show current location, route polyline, distance, ETA, and walking/cycling/shuttle mode. Activity Recognition suggests a mode; manual choice remains available. If the student is outside campus, begin the displayed route at the nearest campus entrance. |
| Reservation confirmation | Student | Atomically reserve stock, calculate the final price, apply points if selected, and show a success summary. Failed or concurrent reservations must never create negative inventory. |
| My Orders | Student | Tabs for reserved, completed, cancelled, and expired orders with pickup window and merchant information. |
| QR Pickup Token | Student | Display a signed, single-use QR token containing the order reference. Do not expose a reusable database credential or raw secret in the QR code. |
| Review | Student | One 1-5 star review and optional comment after a completed pickup only. |
| Eco-Hero Profile | Student | Meals rescued, money saved, CO2 prevented, available points, earned badges, carbon-progress tree, faculty leaderboard, profile editing, notification radius, and logout. |
| Merchant Dashboard | Merchant | Revenue recovered, food diverted, orders completed, average rating, active listings, low-stock alerts, recent orders, and unread merchant-notification count. |
| Merchant Listings | Merchant | List active, sold-out, expired, and draft listings; create, edit, deactivate, or restock only the merchant’s own items. |
| Add/Edit Listing | Merchant | Let the merchant choose either `Upload Photo` or `Use Photo URL`. Upload Photo selects/compresses an image and stores it in Supabase Storage; Use Photo URL accepts an external HTTPS image link and previews it without consuming Supabase Storage. Also collect title, description, category, original price, discounted price, quantity, CO2 estimate, pickup window, closing time, and location. Enforce the RM10 ceiling in both UI and database. |
| Merchant Orders | Merchant | Reserved-order queue, pickup-window validation, order details, and manual token fallback. |
| QR Scanner | Merchant | Camera scan, signed-token verification through Supabase, merchant-ownership check, single-use completion, and clear success/already-used/invalid states. |
| Merchant Reviews/Profile | Merchant | Read reviews for owned listings, show rating summary, update permitted business details, and logout. |
| Notifications | Both roles | Students receive new nearby listing, reservation confirmation, pickup reminder, cancellation, completion, and review-prompt alerts. Merchants receive new reservation, student cancellation, approaching pickup, low-stock/sold-out, and new-review alerts. Supabase Realtime delivers foreground alerts; WorkManager checks unread role-specific notifications in the background and creates Android local notifications. |

All screens use the proposal's Eco-Vibrant design and the locked UI/UX specification below. The interface must feel trustworthy, affordable, sustainable, and easy to operate with one hand; functional completion alone is not sufficient.

The custom adaptive launcher icon will use the proposed FoodHero shield/recycling identity.

## UI/UX Design Specification

### Design goals and hierarchy

- Student goal: find an affordable nearby surplus meal and reserve it with as few decisions as possible.
- Merchant goal: publish surplus quickly, notice reservations immediately, and complete pickup confidently.
- Emotional tone: fresh, optimistic, community-focused, and trustworthy rather than luxurious or overly playful.
- Primary student emphasis: discounted price, remaining pickup time, distance, and stock.
- Primary merchant emphasis: active stock, new reservations, pickup status, and revenue recovered.
- Use high-quality food photography, prominent calls to action, visible ratings, walking distance, pickup estimates, and clear confirmation states—the expected conventions for a food marketplace.

### Locked visual system

Implement all tokens as named Android color, dimension, typography, shape, and theme resources so no screen introduces arbitrary values.

| Token | Value | Usage |
|---|---|---|
| `colorPrimary` | `#216E39` | Main actions, selected navigation, progress, positive sustainability states |
| `colorPrimaryDark` | `#123C25` | App bars, strong headings, launcher-icon base |
| `colorPrimaryContainer` | `#DDF3E3` | Impact banners, selected chips, subtle success cards |
| `colorAccent` | `#FF9800` | Reserve action, flash-sale urgency, pickup emphasis |
| `colorAccentContainer` | `#FFF1D6` | Countdown and pickup reminder backgrounds |
| `colorBackground` | `#F7F8F3` | Main page background |
| `colorSurface` | `#FFFFFF` | Cards, sheets, dialogs, form surfaces |
| `colorTextPrimary` | `#17211A` | Titles, important values, primary body text |
| `colorTextSecondary` | `#627067` | Metadata, helper text, timestamps |
| `colorError` | `#B3261E` | Destructive actions and validation errors only |
| `colorDivider` | `#E1E7E2` | Subtle separators and outlines |

- Follow the 60/30/10 balance: 60% neutral background/surfaces, 30% dark green/text structure, and 10% orange emphasis.
- Use the Android system Roboto family. Limit the hierarchy to four principal sizes: 28sp display metrics, 20sp page/card titles, 16sp actions/body emphasis, and 14sp body/metadata. Use only regular and bold weights.
- Use tabular or monospace numerals only for prominent prices, countdowns, order codes, and dashboard metrics.
- Use an 8-point grid with only 4dp, 8dp, 12dp, 16dp, 24dp, and 32dp spacing increments.
- Default horizontal screen padding is 16dp; card padding is 16dp; gaps between major sections are 24dp.
- Cards use 16dp rounded corners, buttons 14dp, bottom sheets 24dp top corners, and filter chips pill-shaped corners.
- Use light green-tinted elevation/shadows. Avoid heavy black shadows, excessive gradients, glass effects, or decorative animation that reduces clarity.
- Use Material icons from one consistent icon family. Never use emoji as functional icons.

### Navigation structure

- Student bottom navigation contains exactly four destinations: `Feed`, `Campus Map`, `Orders`, and `Impact`.
- Merchant bottom navigation contains exactly four destinations: `Dashboard`, `Listings`, `Orders`, and `Profile`.
- A notification bell with an unread badge appears in the top app bar for both roles and opens the role-filtered Notifications page.
- Preserve each bottom-tab scroll position and selected filters when switching tabs.
- Bottom navigation remains visible on root pages and is hidden on focused tasks such as registration, listing details, add/edit listing, QR display, scanner, and review submission.
- Android system Back returns to the preceding screen, closes an open sheet/dialog first, and never unexpectedly logs the user out or discards a form.
- Destructive actions use confirmation dialogs; ordinary navigation and safe edits do not add unnecessary confirmations.

### Student experience

#### Authentication

- Use one calm, scroll-safe card with FoodHero branding, concise field labels, password visibility control, inline validation, and one strong primary action.
- Role choice uses two large selectable cards with Student and Merchant descriptions rather than a small spinner.
- Keep entered non-password values after validation or network errors. Move focus to the first invalid field and show the error beside it.
- Display progress inside the submit button and disable duplicate submissions while a request is running.

#### Student Feed

- Top area: campus label, notification bell, search field, and a compact impact message such as `You rescued 3 meals this month`.
- Follow with horizontally scrollable category/filter chips; never open a blank search page. When the query is empty, show active listings and recent filters.
- Each listing card shows a consistent 16:9 image, category badge, merchant/listing title, distance, rating, original price with strikethrough, large discounted price, stock, and countdown.
- Use orange only for the effective price, urgent countdown, and Reserve action. Sold-out and expired cards are disabled and visually distinct without relying on color alone.
- Pull-to-refresh updates Supabase data. Initial load uses skeleton cards; subsequent refresh keeps existing content visible.
- Empty state includes a FoodHero illustration/icon, `No surplus food is currently available within UTAR Kampar Campus`, and a `Refresh` action.

#### Listing Details and reservation

- Use a large hero image followed by price/savings, pickup window, distance/ETA, merchant trust information, description, rating, and impact estimate in that reading order.
- Place the quantity selector and a full-width `Reserve for RM X.XX` button in a sticky bottom action area within thumb reach.
- Show reward redemption as a clear optional switch with the exact points and discount; never apply points silently.
- Reservation confirmation uses a short success animation, check mark, order summary, pickup time, and two actions: `View QR` and `Back to Feed`.
- If stock changes during checkout, keep the student on the page, explain the available quantity, update the total, and let the student retry.

#### UTAR Kampar Campus Map

- Use a full-screen map with a top search field/filter button, current-location control, campus-bound camera, and colour-consistent merchant markers.
- Selecting a marker opens a draggable bottom sheet with image, merchant, price, stock, walking distance, ETA, and `View Deal` action; do not navigate immediately on marker tap.
- Walking, cycling, and shuttle choices use a three-option segmented control. Show the selected mode, distance, and ETA together.
- If location is denied, keep the map usable and show an entrance selector with a short explanation instead of repeatedly requesting permission.
- Cluster overlapping markers when required and ensure map controls remain clear of system bars and bottom navigation.

#### Orders, QR, reviews, and impact

- Orders use `Reserved`, `Completed`, and `Cancelled` tabs plus a compact visual status timeline inside each order detail.
- The active order opens with a confident status message and pickup window before secondary details.
- QR display maximises contrast and size, prevents clipping, shows the order code below it, and provides the manual code as an accessible fallback.
- Review submission uses five large star targets, optional comment, remaining-character counter, and a single submit action.
- Impact page leads with meals rescued, money saved, and CO2 prevented, followed by the tree progress, points/reward card, earned badges, and faculty leaderboard.
- Celebrate a completed pickup or newly unlocked badge with a brief scale/fade animation and encouraging copy; do not use a large animation library solely for this effect.

### Merchant experience

- Dashboard opens with `Create Listing` as the dominant thumb-zone action, then shows revenue, rescued quantity, active listings, new reservations, and rating.
- Listing rows/cards show image, title, price, stock, pickup window, status, and overflow actions. `Edit`, `Restock`, and `Deactivate` remain reachable without opening multiple nested menus.
- Add/Edit Listing groups fields into `Photo`, `Food details`, `Price and stock`, `Pickup`, and `Location` sections in a scrollable form.
- Photo section uses a two-option `Upload Photo` / `Use Photo URL` selector. Show the chosen image preview, source label, Replace/Retry/Remove controls, upload progress, and a FoodHero placeholder on failure.
- Use selection controls for category and approved UTAR pickup landmark; use text entry only for values that genuinely require precise input.
- Keep valid form values after an error. Warn before leaving only when unsaved changes exist.
- Orders page prioritises new reservations and approaching pickups. Each row shows student first name, order code, items, pickup time, and status.
- QR scanner uses a clear framing guide, torch control, permission fallback, manual-code action, and distinct valid/invalid/already-used feedback.
- Merchant notifications are grouped as `New` and `Earlier`, use event icons plus readable labels, and deep-link to the relevant order, listing, or review.

### Shared component and state rules

- Reuse named components/styles for top app bars, primary/secondary buttons, listing cards, metric cards, chips, empty states, error states, notification rows, form fields, and bottom sheets.
- Every network screen implements four explicit states: loading, content, empty, and error. Offline cached content adds a non-blocking `Showing saved data` banner.
- Use skeleton placeholders for feed/dashboard loading, inline retry for section failures, and full-page retry only when no usable content exists.
- Snackbars confirm lightweight actions such as favourite changes; dialogs are reserved for destructive or irreversible choices.
- Primary actions remain visible and enabled only when required input is valid. Do not use disabled low-contrast text as the only explanation—show nearby helper text.
- Images use consistent aspect ratios and centre-crop. Glide error/fallback drawables prevent layout jumps when URL or Storage images fail.
- Motion duration stays between 150ms and 250ms, respects the device animation setting, and never blocks interaction.

### Accessibility and device support

- Minimum interactive target is 48dp by 48dp with at least 8dp separation between adjacent actions.
- Normal text and icons meet at least 4.5:1 contrast; large text meets at least 3:1.
- Status is always represented by icon/text as well as colour. Countdown urgency cannot be communicated by red/orange alone.
- Every meaningful image/icon has a TalkBack description; decorative imagery is excluded from accessibility focus.
- Logical focus order follows the visible reading order. Error announcements, notification badges, QR fallback codes, and scanner results are accessible.
- Support font scaling to 200% without clipped prices, buttons, navigation labels, form errors, or dashboard metrics.
- All task pages scroll on 360x640dp-class screens and remain usable in portrait; map and scanner handle landscape without overlapping controls.
- Respect edge-to-edge insets, display cut-outs, keyboard visibility, light/dark system bars, and Android 13+ notification permission.
- Use plain English labels and concise Malaysian currency formatting (`RM 4.50`); avoid unexplained technical terms.

### UI ownership and review gate

- Chai defines and implements the theme resources, components, student screens, navigation behaviour, state patterns, and reference merchant components before the Fong handoff.
- The `handoff-fong-ready` tag must include a small UI reference screen or sample XML usages for buttons, cards, fields, empty states, and notification rows.
- Fong implements merchant screens only with the locked tokens and components. New colours, spacing values, type sizes, or interaction patterns require Chai's review before merge.
- Before release, compare every implemented screen with this specification and the proposal mock-ups; preserve the Eco-Vibrant identity while adapting layouts to native Android conventions.

## Architecture and Backend Contracts

### Android structure

Use a compact native structure without unnecessary framework layers:

- Activities: `SplashActivity`, `AuthActivity`, `StudentHomeActivity`, and `MerchantHomeActivity`.
- Fragments for the bottom-navigation pages.
- RecyclerView adapters for listings, orders, reviews, and analytics.
- Plain Java model classes and three shared repositories: `AuthRepository`, `FoodHeroRepository`, and `LocalCacheRepository`.
- Retrofit/OkHttp/Gson for Supabase REST, Auth, Storage, and Edge Function calls.
- Glide for consistent image preview/display, placeholders, errors, memory caching, and disk caching across Supabase Storage and external HTTPS URLs.
- OkHttp WebSocket for the narrowly scoped Supabase Realtime notification subscription.
- Room SQLite for cached listings, favourites, and last successful dashboard data.
- EncryptedSharedPreferences for refresh-session data.
- WorkManager for background notification checks.
- ZXing Embedded for QR generation/scanning.
- Google Maps, Fused Location, Activity Recognition, and Polyline utilities.

No dependency injection framework, custom navigation framework, generic base repository, or speculative abstraction will be added.

### Shared Java interfaces and types

Lock these contracts in the initial scaffold commit so both contributors can work without redefining shared behavior:

- Enums: `UserRole`, `ListingStatus`, `OrderStatus`, `TravelMode`, and `NotificationType`.
- Core models: `Profile`, `Merchant`, `Listing`, `Order`, `Review`, `ImpactSummary`, `RouteResult`, and `FoodHeroNotification`.
- `AuthRepository`: register, login, restore session, refresh session, reset password, and logout.
- `FoodHeroRepository`: listing CRUD, active-feed retrieval, reservation, order retrieval, pickup verification, review submission, analytics, location update, notification retrieval, Supabase image upload/delete, and external photo URL validation/persistence.
- `LocalCacheRepository`: replace/read cached listings, add/remove/read favourites, and cache/read dashboard summaries.
- All asynchronous operations return one consistent success/error callback type with user-safe messages and preserved HTTP/database diagnostics for debugging.

### Supabase schema

Create versioned SQL migrations for:

- `profiles`: authenticated identity, protected role, name, student ID, faculty, points, and impact counters.
- `student_allowlist` and `merchant_allowlist`: private demo identities and assigned roles.
- `merchants`: owner, business name, verification state, address, coordinates, closing time, and aggregate rating.
- `listings`: merchant, food details, prices, quantity, category, `image_source` (`storage`, `external_url`, or `none`), resolved `image_url`, optional Supabase `storage_path`, pickup times, location, CO2 estimate, and status.
- `orders`: student, listing, quantity, price/savings, reward usage, pickup window, state, and completion timestamp.
- `reviews`: one review per completed order.
- `user_locations`: opt-in last known student location, alert radius, and timestamp.
- `service_areas`: the authoritative UTAR Kampar campus boundary polygon, official centre point, active status, and display name.
- `campus_landmarks`: approved UTAR Kampar entrances, academic blocks, student pavilions, merchant locations, and other searchable map points.
- `notifications`: per-user notification queue for both students and merchants, including recipient, role, event type, related listing/order/review, message, creation time, and read timestamp.
- `reward_redemptions`: auditable point deductions and discounts.

Business rules:

- Discounted price must be positive, below the original price, and no more than RM10.
- A reservation trigger locks the listing row, checks time/status/stock, decrements quantity atomically, and rejects overselling.
- Completing an order is idempotent and awards 10 points per rescued meal.
- Redeeming 100 points deducts RM5 from a reservation, without allowing a negative total.
- Badges are derived at 1, 5, 10, and 25 completed rescued meals.
- Money saved is `original price - final paid price`.
- CO2 prevented is the sum of each completed listing’s `co2_kg_per_item × quantity`.
- Faculty leaderboard totals completed rescues by faculty.
- Merchant analytics are derived only from the merchant’s completed orders.
- Database triggers create notifications for both roles: students are notified about relevant listing/order events, while the owning merchant is notified about reservations, cancellations, approaching pickups, low stock, sold-out listings, and reviews.
- A merchant or listing coordinate must fall inside the active UTAR Kampar `service_areas` polygon. The database rejects outside-campus coordinates even if validation is bypassed in the Android app.
- Student coordinates outside the campus polygon are not stored as precise locations; the application keeps only an `outside_campus` state and determines the nearest approved campus entrance for routing.

### UTAR Kampar Campus Map boundary

- Seed the official campus centre as latitude `4.336214`, longitude `101.142111`.
- Trace and store the UTAR Kampar perimeter as a polygon rather than using a broad circular radius. This prevents nearby Kampar-town businesses from appearing accidentally.
- Constrain the Google Maps camera target to the campus bounds and set minimum/maximum zoom levels appropriate for the campus.
- Limit map search to FoodHero merchants and seeded `campus_landmarks`; do not enable unrestricted Google Places search, a Kampar town view, multiple campuses, or a campus selector.
- Return only listings whose pickup coordinates are inside the UTAR Kampar polygon.
- Accept merchant location selection only from an on-campus map pin or an approved campus landmark.
- Require every Google Routes destination to be inside the campus boundary. When the student is outside campus, calculate the FoodHero campus route from the nearest approved entrance instead of drawing an off-campus route.
- Generate geofenced FoodHero alerts only for on-campus listings and students whose latest consented location is inside the campus.
- When no listing is available, show: `No surplus food is currently available within UTAR Kampar Campus.`

### Security and external services

- Enable RLS on every exposed table and define separate ownership policies for students and merchants.
- The Android app contains only the Supabase publishable key. Service-role, QR-signing, and Google Routes secrets stay in Supabase Edge Function secrets.
- Role authorization comes from protected database rows, not user-editable metadata.
- Storage bucket `listing-images` is publicly readable for marketplace images; only the owning merchant may insert, replace, or delete files under their UID path.
- Supabase Storage remains fully supported. Device uploads accept JPEG, PNG, or WebP, are compressed before upload, and are limited to approximately 5 MB per source file.
- To conserve the Supabase free storage allowance, merchants may instead save an external photo URL. Accept only absolute `https://` URLs up to 2,048 characters and require the image preview to load successfully before enabling Save. Do not accept `http://`, `file://`, `content://`, JavaScript, data-URI, or local-network addresses as external listing URLs.
- External image links are loaded directly by the Android image loader; Supabase and its Edge Functions do not download or proxy those images. A failed or expired external link displays the FoodHero placeholder and does not break the listing/feed.
- When a merchant changes from an existing Supabase Storage image to an external URL, update the listing first and delete the old owned Storage object only after the database update succeeds. When changing from a URL to an upload, save the new Storage object and listing reference before replacing the previous URL.
- Use a protected `route-proxy` Edge Function for Google Routes so the unrestricted server key is never shipped in the APK. Return only distance, duration, and encoded polyline fields, matching Google’s efficient field-mask guidance: [Google Compute Routes API](https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes).
- The `route-proxy` must validate the destination against the stored UTAR Kampar polygon before calling Google Routes. Outside-campus destinations return a validation error and do not consume a Routes API request.
- Use `issue-pickup-token` and `verify-pickup` Edge Functions for signed, expiring, one-use QR verification.
- Follow Supabase’s frontend guidance: publishable client key, least-privilege grants, and RLS on exposed data: [Supabase data security](https://supabase.com/docs/guides/database/secure-data).
- Ask only for foreground location. Upload a coarse/precise last-known location after consent; do not request continuous background location.
- Android notification permission, location permission, activity-recognition permission, and camera permission must each have denial fallbacks.

### Notification compromise

Because FCM was declined:

- While the app is active, an authenticated Supabase Realtime subscription receives only notification rows addressed to the signed-in student or merchant.
- In the background, WorkManager checks unread role-specific notifications using network constraints and posts them through separate Student and Merchant Android notification channels.
- Student notifications cover nearby on-campus listings, reservation changes, pickup reminders, completion, and review prompts.
- Merchant notifications cover new reservations, cancellations, approaching pickups, low stock/sold out, and new reviews.
- Android limits periodic WorkManager jobs to a minimum 15-minute interval, so closed-app alerts are not guaranteed to be immediate: [Android periodic work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
- The Part 2 report must describe this honestly as foreground real-time plus delayed background delivery, not claim FCM-style instant push.
- FCM remains the future upgrade if immediate closed-app delivery becomes mandatory. Supabase’s official push example also relies on an external platform push service: [Supabase push-notification example](https://supabase.com/docs/guides/functions/examples/push-notifications).

## Two-Contributor Coding Plan

### Part 1 — Chai Boon Hong, approximately 80%

Chai owns the shared platform, all backend contracts, the complete student journey, and every prerequisite required before Fong can start:

1. Establish the Android/Git foundation, dependencies, package structure, locked Eco-Vibrant design tokens, reusable UI components/styles, navigation shell, state patterns, launcher icon, shared models, callbacks, and repository interfaces.
2. Initialize Git in the `FoodHero` folder, add the Android `.gitignore`, protect local/API credential files, and create the first scaffold commit.
3. Build all Supabase migrations, constraints, triggers, RLS policies, Storage policies, seed allowlists, demo data, analytics queries, and security checks.
4. Implement REST/Auth/Storage clients, secure session handling, Room cache, offline favourites, error handling, and role routing.
5. Implement registration, login, password reset, and profile completion.
6. Implement Student Feed, filters, countdowns, listing details, reservation, reward redemption, order history, and QR display.
7. Implement the UTAR Kampar-only Google Map, stored campus polygon and landmarks, camera bounds, inside-campus validation, nearest-entrance fallback, location handling, Activity Recognition, Google Routes Edge Function, travel-mode fallback, ETA, and route polyline.
8. Implement Eco-Hero dashboard, badges, loyalty, faculty leaderboard, student review flow, settings, the shared Realtime client, and role-aware WorkManager notification checks.
9. Implement and verify the merchant-facing repository operations, Supabase RLS policies, notification triggers, dual-source listing-image contract (Supabase upload or external HTTPS URL), safe source switching, analytics responses, QR verification contract, and stable Java models/callbacks that Fong's screens will consume.
10. Merge Chai's completed core into `main`, tag the stable handoff as `handoff-fong-ready`, provide seeded merchant data and reference UI component usages, and verify that every merchant endpoint and shared UI state can be exercised before Fong begins coding.
11. After Fong's pull request, perform only cross-role integration, schema verification, security-advisor checks, release build, demo-data reset procedure, and report integration.

Suggested Chai commits:

- `chore: establish Java project structure and shared contracts`
- `feat: add secured Supabase schema and storage policies`
- `feat: implement authentication session and offline cache`
- `feat: build student feed reservation and order flow`
- `feat: add campus routing and smart pickup estimates`
- `feat: add impact rewards reviews and nearby alerts`
- `feat: add role-aware student and merchant notifications`
- `feat: finalize merchant service contracts for handoff`
- `test: verify integrated FoodHero workflows`

### Part 2 — Fong Chee Hou, approximately 20%

Fong starts only after Chai's `handoff-fong-ready` tag exists. Fong owns the merchant presentation layer using the completed, tested service interfaces; Fong does not redesign the schema or shared contracts:

1. Implement Merchant Dashboard, analytics cards, recent orders, unread-notification badge, and merchant notification list using Chai's finished repository methods.
2. Implement the merchant listing screens and add/edit form using the completed dual-source image contract. Provide an `Upload Photo` / `Use Photo URL` selector, device picker, URL field, preview, retry/remove controls, RM10 UI validation, and lifecycle-state display.
3. Implement the merchant order queue, order details, QR scanner, manual token fallback, and completion feedback using the completed pickup-verification contract.
4. Add merchant empty/loading/error states, run focused merchant UI tests, capture labelled screenshots, and document Fong's implementation for the Part 2 report and Q&A.

Suggested Fong commits:

- `feat: build merchant dashboard and analytics`
- `feat: add merchant notifications and dual-source listing photos`
- `feat: implement merchant order and pickup screens`
- `test: cover merchant UI workflows`

### GitHub contribution workflow

1. Create a private GitHub repository named `Group13-FoodHero` from Chai’s account and invite Fong as a collaborator.
2. Chai creates `feature/chai-core`, completes the 80% core implementation, opens a pull request, and merges it into `main` after verification.
3. Chai tags the verified `main` commit as `handoff-fong-ready`. Fong must not begin implementation before this handoff because Fong's screens depend on Chai's final Java models, repositories, Supabase schema, seeded data, and API behavior.
4. Fong creates `feature/fong-merchant-ui` from `handoff-fong-ready` and implements only the assigned merchant presentation workflow.
5. Each member configures their own GitHub-connected name/email and commits from their own machine/account.
6. Fong pushes and opens a pull request for the merchant UI track. Chai reviews it without replacing or re-authoring Fong’s commits.
7. Merge using a normal merge commit, not squash, so both authors’ commits remain visible.
8. Chai makes any cross-role integration fixes on `feature/integration`, merges them, and tags the demonstrated build `v1.0-assignment`.
9. The report lists each commit range, branch, pages, backend functions, test evidence, and screenshots by contributor.

The purpose is genuine independent contribution. Artificial empty commits, forged authors, or having one person commit all code under two identities are excluded.

## Implementation Order and Schedule

### Day 1 — Chai foundation and backend

- Chai: repository setup, Gradle dependencies, complete Eco-Vibrant design system, navigation shell, reusable components/states, schema, RLS, Storage, external-photo URL contract, seed data, shared interfaces, Auth, session/cache, reservation logic, QR contracts, merchant service operations, and role-aware notification generation.
- Fong does not implement code on Day 1 because the required contracts are not yet stable.
- Checkpoint: both demo accounts can register/login; backend tests confirm listings, orders, QR verification, analytics, and notifications for both roles.

### Day 2 — Chai completes the core and creates the handoff

- Chai: student feed, listing details, reservation, orders, UTAR Kampar boundary enforcement, campus-only Google Map/Routes, QR display, impact, loyalty, reviews, student notifications, merchant notifications, and all reusable merchant-facing APIs.
- Chai merges `feature/chai-core` only after the student journey and merchant backend contracts pass their checks, then creates `handoff-fong-ready`.
- Fong may begin only after this checkpoint is complete; otherwise Fong waits until Day 3.
- Checkpoint: Chai can exercise the entire student flow and every merchant endpoint with seeded data or focused tests.

### Day 3 — Fong's dependent 20% and final integration

- Fong branches from `handoff-fong-ready` and implements the assigned merchant dashboard, notification/listing screens, order queue, QR scanner screen, and focused UI tests.
- Fong opens the merchant UI pull request with screenshots and test evidence; Chai reviews and merges it without rewriting Fong's commits.
- Run RLS/security, unit, instrumentation, physical-device, notification, map, QR, and offline tests.
- Fix only verified integration defects.
- Produce the signed/debug APK, clean-clone Android Studio instructions, screenshots, GitHub contribution table, demo script, and limitations section.
- Freeze demo data and tag the final commit before the deadline.

## Test and Acceptance Plan

### Automated checks

- Java unit tests: RM10 price ceiling, countdown/expiry state, points redemption, badges, savings/CO2 calculations, route-mode mapping, QR response parsing, and cache updates.
- Campus-boundary unit tests: points inside the UTAR Kampar polygon, points on its edge, points outside it, invalid coordinates, nearest approved entrance selection, and map-result filtering.
- Supabase tests: allowlists, table constraints, atomic stock decrement, zero-stock rejection, duplicate review rejection, pickup idempotency, and reward balance protection.
- Notification tests: confirm recipient ownership and event payloads for student listing/order alerts and merchant reservation, cancellation, pickup, stock, sold-out, and review alerts.
- RLS matrix: anonymous, student A, student B, merchant A, and merchant B access to every table/action.
- Storage policies: public image read, owner upload/update/delete, and cross-merchant denial.
- Image-source tests: successful Supabase upload, successful HTTPS URL preview, URL persistence without Storage usage, invalid scheme, malformed URL, broken/expired image fallback, oversized upload rejection, and safe switching between Storage and URL sources.
- Edge Functions: unauthenticated denial, malformed inputs, route provider failure, expired QR, wrong merchant, already-completed order, and successful completion.
- Route protection: reject outside-campus destinations before calling Google Routes and confirm that no unrestricted API key is returned to the Android client.
- Android checks: `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`, and `assembleDebug`.
- Supabase security and performance advisors must be clean or each remaining advisory documented.

### UI/UX verification

- Verify Student Feed, Listing Details, Campus Map, Orders/QR, Impact, Merchant Dashboard, Add/Edit Listing, Merchant Orders/Scanner, Notifications, and Auth on at least a compact 360x640dp device and a typical 1080x2400 phone.
- Capture screenshots for every root page and every important loading, empty, offline, error, permission-denied, and success state.
- Confirm all pages use only the locked colours, type hierarchy, spacing increments, shapes, components, icon family, and currency format.
- Run TalkBack through registration, reservation, QR fallback, merchant pickup verification, and notifications; correct missing labels and illogical focus order.
- Test font scales at 100%, 150%, and 200%; reject clipped, overlapping, or inaccessible controls.
- Check contrast, 48dp targets, keyboard behaviour, system insets, scroll reachability, image placeholders, and one-handed access to primary actions.
- Confirm tab switching preserves state, Back navigation is predictable, destructive actions require confirmation, and safe actions do not create unnecessary dialogs.
- UI acceptance requires visual inspection on a physical Android phone; a successful build or Espresso run alone is not visual proof.

### Physical-device scenarios

1. Register and log in with the seeded student and merchant allowlist accounts.
2. Deny each permission once and verify the app remains usable with a clear fallback.
3. Confirm the map opens at UTAR Kampar, remains camera-bound to the campus, and never displays unrelated Kampar-town results.
4. Merchant creates one listing with a Supabase Storage upload and another with a valid external HTTPS photo URL; both images appear in the merchant preview, Student Feed, and Listing Details pages.
5. Confirm the external URL listing does not create a Supabase Storage object, while the upload listing remains stored and accessible through the `listing-images` bucket.
6. Confirm invalid, non-HTTPS, malformed, or broken URLs cannot be saved; confirm an image that becomes unavailable later displays the FoodHero placeholder without crashing.
7. Confirm switching from an upload to a URL updates the listing before deleting the old owned Storage object; switching back to an upload preserves a valid image throughout the change.
8. Confirm an outside-campus merchant or listing location is rejected in both the Android form and Supabase database.
9. Confirm a price above RM10 is rejected in both the app and database.
10. Student receives the listing, filters it, favourites it, restarts the app, and retrieves the favourite from local storage.
11. Student inside campus views the map, selects walking/cycling/shuttle, and receives an on-campus route, distance, and ETA.
12. Student outside campus is shown the nearest approved UTAR entrance, and the displayed FoodHero route begins from that entrance.
13. Deny location permission and confirm the campus map still works with manual entrance selection.
14. Student reserves the final available item; a simultaneous second attempt fails without negative stock.
15. Student displays the QR; the owning merchant scans it and completes the order exactly once.
16. Student submits one review; a second review attempt is rejected.
17. Student and merchant dashboards update savings, CO2, points, revenue, diversion, and rating.
18. An on-campus nearby listing produces an immediate foreground Realtime alert; an outside-campus test record produces none.
19. A new reservation produces a merchant notification containing the correct order and listing reference.
20. Student cancellation, approaching pickup, low-stock/sold-out, and new-review events each produce the correct merchant alert without exposing another merchant's data.
21. A background WorkManager run retrieves unread student and merchant notifications through their respective signed-in accounts and creates the correct local Android notification.
22. Disable the network and verify cached browsing/favourites and previously cached images work while reservation clearly requires connectivity.
23. Install the final APK and confirm the custom launcher icon, cold start, session restore, logout, and clean login.

### Definition of done

FoodHero is complete when a clean Android Studio checkout can build and demonstrate this uninterrupted story:

`Merchant login → create a surplus bag at an approved UTAR Kampar pickup point → student login → discover the on-campus bag → view an on-campus route/ETA → reserve with optional reward → display signed QR → merchant scans and completes pickup → student reviews → both dashboards update → cached content survives restart.`

The Part 2 report must contain labelled screenshots, GitHub contribution evidence, Supabase/Google service architecture, tests performed, UI/UX evidence, and the notification limitation. No feature may be reported as working or visually complete unless it was observed on the final build and checked against the locked design specification.

## Assumptions and Fixed Defaults

- Chai Boon Hong is the primary contributor with approximately 80% of the implementation. Fong Chee Hou contributes approximately 20% through the dependent merchant presentation workflow after Chai's stable handoff.
- Repository visibility defaults to private until submission requirements say otherwise.
- The existing Supabase `FoodHero` project is reused.
- Demo allowlists contain the two presentation accounts; no claim of production university identity verification is made.
- Student discounted-price ceiling is RM10.
- Merchants may use either Supabase Storage or an external HTTPS URL for each listing photo. Supabase Storage is retained; external URLs are the quota-saving option, not a replacement.
- Merchants are responsible for using image URLs they are permitted to display. Broken external links fall back to the FoodHero placeholder and can be replaced from the Edit Listing page.
- Payment is “reserve in app, pay at pickup”; no payment gateway is introduced because the proposal requires QR pickup verification, not payment processing.
- Google Maps/Routes uses the existing FoodHero Google Cloud project. The Maps key is Android-restricted by package name and signing SHA; the Routes server key stays in Supabase secrets.
- The only supported service area is UTAR Kampar Campus, centred at `4.336214, 101.142111`. Listings, merchant pickup points, map search, route destinations, and location-triggered notifications outside the stored campus polygon are excluded.
- No Kampar town marketplace, UTAR Sungai Long support, multi-campus selector, or unrestricted Google Places search is included.
- Walking and cycling use their matching Google route modes. Shuttle uses available transit data; if campus shuttle data is unavailable, it is visibly labelled as a driving-time estimate.
- Foreground location is sufficient; background-location permission is not requested.
- Immediate closed-app push is excluded by the selected no-FCM decision and replaced by the Supabase Realtime/WorkManager hybrid.
- Android application sources remain Java. XML, SQL migrations, and small Supabase Edge Functions in TypeScript are infrastructure artifacts required by their respective platforms, not a Kotlin application rewrite.
- The application ships with the single locked Eco-Vibrant visual system in this plan. Dark mode and additional visual themes are excluded from the assignment build unless all required screens and states are already complete and verified.
