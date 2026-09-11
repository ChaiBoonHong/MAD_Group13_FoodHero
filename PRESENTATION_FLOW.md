# FoodHero End-to-End Presentation Flow

Target duration: 8–10 minutes. Divide the demonstration into two role-based parts: **Merchant first**, followed by **Student**.

Use two physical phones where possible. Before presenting, configure a campus with a reviewed boundary, a verified Merchant account with a real test DuitNow QR, and a Student account with at least 100 Eco-Points.

## Opening

**Presenter line:** “FoodHero connects verified campus members with nearby merchants’ surplus food. Merchants recover value from unsold food, while students receive affordable meals and earn rewards for reducing food waste.”

Show the Login/Register page and briefly introduce the Student and Merchant role selector.

---

# Part 1 — Merchant Flow

## 1. Merchant registration

1. Select **Merchant**.
2. Select **Register**.
3. Enter the owner’s name, verified email, and password.
4. Confirm the email through Supabase Auth.
5. Return to FoodHero and log in.

**Presenter line:** “A merchant may register using any valid, verified email. FoodHero does not approve an unverified account.”

## 2. Complete Merchant Setup

Demonstrate the three setup steps.

### Step 1 — Business details

Enter:

- Business or stall name
- Stall description
- Contact phone number

Explain that incomplete or invalid fields show inline errors and do not create a Merchant role.

### Step 2 — DuitNow payment

1. Enter the DuitNow account or display name.
2. Select the merchant’s real DuitNow QR image.
3. Show the real preview.
4. Demonstrate **Replace QR** if time permits.

**Presenter line:** “The QR image is stored in private Supabase Storage. FoodHero never treats a placeholder as a real payment QR.”

### Step 3 — Campus and pickup location

1. Select the merchant’s campus.
2. Show the Google Map centered on that campus.
3. Tap **Locate me**.
4. Demonstrate map panning, pinch zoom, native zoom controls, and compass.
5. Tap the map to place the pickup pin.
6. Drag the marker to refine the position.
7. Review the campus, coordinates, and merchant details.
8. Accept the terms and select **Activate Merchant Account**.

**Presenter line:** “Supabase checks the selected campus, private QR object, and pickup coordinates against the reviewed campus boundary. The Android app cannot approve itself.”

## 3. Merchant dashboard

After activation, show:

- Merchant identity and campus
- Listing summary
- Pending payment-verification orders
- Ready-for-pickup orders
- Completed revenue
- Notifications

## 4. Create a surplus-food listing

1. Open **Listings**.
2. Select **Create Listing**.
3. Enter:
   - Food title and description
   - Category
   - Original price
   - Discounted price
   - Available quantity
   - Pickup window
   - Environmental value
4. Select a real listing image or valid HTTPS image.
5. Publish the listing.

**Presenter line:** “The listing inherits its campus and pickup coordinates from the Merchant profile. The Merchant cannot publish a listing at another campus by changing the request.”

## 5. Edit Merchant information

1. Open **Merchant Profile**.
2. Select **Edit Profile**.
3. Show that the Merchant can update:
   - Business name
   - Stall description
   - Contact phone
   - DuitNow display name
   - DuitNow QR image
   - Campus
   - Pickup location pin
4. Save the changes.

**Presenter line:** “Profile changes use the same guarded Supabase validation as initial setup. Updated coordinates and QR paths cannot bypass the backend checks.”

## 6. Merchant waits for an order

Leave the Merchant account on the Orders or Dashboard screen.

**Transition line:** “The Merchant’s listing is now available to verified students at the same campus. We will switch to the Student journey.”

---

# Part 2 — Student Flow

## 7. Student registration

1. Select **Student** on the Login/Register page.
2. Select **Register**.
3. Enter:
   - Full name
   - Supported institutional email
   - Password
   - Student ID
   - Faculty
4. Confirm the institutional email.
5. Log in.

**Presenter line:** “FoodHero uses exact institutional-domain matching. The institution and main campus are assigned server-side, so the Student cannot select or forge another campus.”

Mention these validation examples:

- A supported institutional domain is accepted.
- An unknown domain is rejected.
- A deceptive suffix such as `1utar.my.attacker.com` is rejected.
- An unconfirmed account cannot enter the Student workspace.

## 8. Discover campus listings

1. Show the Student home feed.
2. Show that the Merchant’s listing appears.
3. Open the campus Google Map.
4. Select walking, cycling, or shuttle mode.
5. Show the Google Routes polyline, distance, and estimated duration.
6. Open the listing details.

**Presenter line:** “A Student sees and reserves only listings belonging to the Student’s verified campus.”

## 9. Select quantity

1. Increase and decrease the quantity.
2. Show the changing subtotal.
3. Attempt to exceed the available stock.

Explain that Supabase locks the listing during reservation so simultaneous orders cannot reduce stock below zero.

## 10. Redeem Eco-Points

1. Show the Student’s current Eco-Points.
2. Enable **Redeem Eco-Points**.
3. Show the reduced payable amount.

Current reward rule:

- The Student needs at least **100 Eco-Points**.
- One eligible order redeems **100 points**.
- The discount is up to **RM5.00**.
- The discount never exceeds the order subtotal.
- Supabase deducts the points and creates a reward-redemption record atomically with the reservation.
- If the unpaid order is cancelled, expires, or its payment is rejected, the points are restored exactly once.

## 11. Reserve the listing

1. Select **Reserve**.
2. Show the generated order and payment reference.
3. Point out the server-controlled payment countdown.

**State:**

```text
AWAITING_PAYMENT
```

Supabase atomically:

- Validates the Student and campus
- Validates the requested quantity
- Locks and reduces stock
- Applies an eligible reward redemption
- Creates the order
- Creates an opaque pickup token
- Assigns the payment expiry

## 12. Pay with DuitNow

1. Show the associated Merchant’s real DuitNow QR.
2. Show the Merchant name, exact payable amount, and payment reference.
3. Demonstrate **Save QR** or the Android Sharesheet.
4. Open the bank/e-wallet application.
5. Complete the test payment.
6. Return to FoodHero.
7. Upload the real payment receipt.

**State transition:**

```text
AWAITING_PAYMENT
→ PENDING_VERIFICATION
```

The order changes state only after the receipt exists in private Supabase Storage and the receipt RPC succeeds.

## 13. Merchant verifies the payment

Return briefly to the Merchant phone.

1. Open the pending-verification notification or order.
2. Open the Student’s private receipt.
3. Explain both possible actions:
   - **Reject:** A reason is required. Stock and redeemed points are restored exactly once, and the Student is notified.
   - **Approve:** The order becomes ready for pickup, and the Student is notified.
4. Approve the demonstration order.

**State transition:**

```text
PENDING_VERIFICATION
→ READY_FOR_PICKUP
```

## 14. Student receives pickup confirmation

Return to the Student phone.

1. Open the notification.
2. Show the **Ready for Pickup** order.
3. Display the pickup QR.

**Presenter line:** “The pickup token is opaque, high-entropy, and single-use. It cannot be reused to complete an order twice.”

## 15. Merchant completes pickup

On the Merchant phone:

1. Open the pickup scanner.
2. Scan the Student’s pickup QR.
3. If camera access is unavailable, demonstrate the manual pickup-code fallback.
4. Wait for Supabase confirmation.

**State transition:**

```text
READY_FOR_PICKUP
→ COMPLETED
```

Only the associated Merchant can complete the pickup.

## 16. Student rewards and environmental impact

Return to the Student phone and show:

- Completed order
- Meals rescued
- Money saved
- CO₂ prevented
- Updated Eco-Points

Current completion reward:

- The Student earns **10 Eco-Points per completed item**.
- Impact totals update only after the first valid transition to `COMPLETED`.
- Replaying the pickup QR cannot award points or impact twice.

## 17. Merchant revenue

Return to the Merchant dashboard and show:

- Completed order
- Confirmed revenue
- Updated order statistics

Merchant revenue is updated only once after the order is successfully completed.

## 18. Student review

1. Open the completed order.
2. Submit a rating and review.
3. Return to the Merchant profile to show the updated review information.

Only the Student who owns the completed order can review it, and only one review is allowed per order.

## 19. Role signup and switching

If time permits, demonstrate the role-aware profile button:

- Student-only account: **Sign up as Merchant**
- Merchant-only account: **Sign up as Student**
- Dual-role account: **Switch to Merchant** or **Switch to Student**

Explain that the roles are attached to the same Supabase Auth user. Switching persists `last_active_role`, while signing up does not create a duplicate account.

---

# Final Flow Summary

## Merchant

```text
Merchant Register
→ Verify Email
→ Login
→ Enter Business Information
→ Upload Real DuitNow QR
→ Select Campus and Pin Pickup Location
→ Merchant Account Activated
→ Create Listing
→ Receive Payment Receipt
→ Approve or Reject Payment
→ Prepare Order
→ Scan Pickup QR or Enter Manual Code
→ Order Completed
→ Revenue Updated
```

## Student

```text
Student Register
→ Verify Institutional Email
→ Institution and Campus Assigned
→ Login
→ Discover Campus Listing
→ Select Quantity
→ Optionally Redeem 100 Eco-Points
→ Reserve Order
→ Pay with Merchant DuitNow QR
→ Upload Receipt
→ Wait for Merchant Verification
→ Receive Ready-for-Pickup Notification
→ Present Pickup QR or Manual Code
→ Order Completed
→ Earn Eco-Points and Update Impact
→ Submit Review
```

## Order lifecycle

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

A paid order that misses the pickup deadline becomes `NO_SHOW`. It is not refunded, and its stock is not restored.

# Evidence Checklist

- Merchant and Student email logins work on separate accounts.
- The selected demonstration campus has a reviewed boundary.
- Merchant registration/editing works with a real private DuitNow QR.
- Merchant can pan and zoom the Google Map after placing a pin.
- Listing has sufficient stock.
- Student has at least 100 Eco-Points before the demonstration.
- Student phone has a bank/e-wallet application.
- Merchant phone has camera permission.
- Google Maps and Google Routes work on the presentation APK.
- Receipt approval, pickup completion, rewards, impact, and revenue are visibly confirmed by Supabase-backed screens.
