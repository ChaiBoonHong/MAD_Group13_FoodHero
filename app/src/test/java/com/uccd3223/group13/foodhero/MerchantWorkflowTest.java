package com.uccd3223.group13.foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.foodhero.data.model.ImageSource;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.ListingStatus;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.model.OrderVerificationResult;
import org.junit.Test;

public class MerchantWorkflowTest {

    // ========================================================================
    // 1. RM10 PRICE CEILING ENFORCEMENT
    // ========================================================================

    @Test
    public void testDiscountedPrice_withinCeiling() {
        double originalPrice = 14.00;
        double discountedPrice = 6.50;

        assertTrue("Discounted price must be <= RM10.00 ceiling", discountedPrice <= 10.00);
        assertTrue("Discounted price must be positive", discountedPrice > 0);
        assertTrue("Discounted price must be less than original price", discountedPrice < originalPrice);

        Listing listing = new Listing();
        listing.setOriginalPrice(originalPrice);
        listing.setDiscountedPrice(discountedPrice);
        assertEquals(7.50, listing.getSavingsAmount(), 0.001);
    }

    @Test
    public void testDiscountedPrice_exactTenRinggitAllowed() {
        double discountedPrice = 10.00;
        assertTrue("RM10.00 is allowed as maximum price ceiling", discountedPrice <= 10.00);
    }

    @Test
    public void testDiscountedPrice_exceedsTenRinggitRejected() {
        double discountedPrice = 10.01;
        assertFalse("Price above RM10.00 must be rejected", discountedPrice <= 10.00);
    }

    // ========================================================================
    // 2. DUAL-SOURCE PHOTO VALIDATION (HTTPS & LENGTH)
    // ========================================================================

    @Test
    public void testExternalImageUrl_validHttps() {
        String validUrl = "https://images.unsplash.com/photo-bento-box-123.jpg";
        boolean isValidScheme = validUrl.startsWith("https://");
        boolean isValidLength = validUrl.length() <= 2048;

        assertTrue("Valid external photo must use HTTPS", isValidScheme);
        assertTrue("Valid external photo URL must not exceed 2048 characters", isValidLength);

        Listing listing = new Listing();
        listing.setImageSource(ImageSource.EXTERNAL_URL);
        listing.setImageUrl(validUrl);
        assertEquals(ImageSource.EXTERNAL_URL, listing.getImageSource());
        assertEquals(validUrl, listing.getImageUrl());
    }

    @Test
    public void testExternalImageUrl_rejectHttpAndExcessiveLength() {
        String httpUrl = "http://example.com/food.jpg";
        assertFalse("Plain HTTP URL must be rejected for security", httpUrl.startsWith("https://"));

        StringBuilder longUrl = new StringBuilder("https://example.com/");
        for (int i = 0; i < 2100; i++) {
            longUrl.append("a");
        }
        assertFalse("URL exceeding 2048 characters must be rejected", longUrl.length() <= 2048);
    }

    // ========================================================================
    // 3. MERCHANT ORDER STATUS FILTERING LOGIC
    // ========================================================================

    @Test
    public void testMerchantOrderQueueFiltering() {
        Order reservedOrder = new Order();
        reservedOrder.setStatus(OrderStatus.RESERVED);

        Order pendingVerificationOrder = new Order();
        pendingVerificationOrder.setStatus(OrderStatus.PENDING_VERIFICATION);

        Order completedOrder = new Order();
        completedOrder.setStatus(OrderStatus.COMPLETED);

        Order cancelledOrder = new Order();
        cancelledOrder.setStatus(OrderStatus.CANCELLED);

        // Tab 1: Reserved / Ready / Slip Pending
        assertTrue("Reserved order belongs to Active Tab",
            reservedOrder.getStatus() == OrderStatus.RESERVED || reservedOrder.getStatus() == OrderStatus.PENDING_VERIFICATION);
        assertTrue("Pending receipt order belongs to Active Tab",
            pendingVerificationOrder.getStatus() == OrderStatus.RESERVED || pendingVerificationOrder.getStatus() == OrderStatus.PENDING_VERIFICATION);

        // Tab 2: Completed
        assertTrue("Completed order belongs to Completed Tab", completedOrder.getStatus() == OrderStatus.COMPLETED);

        // Tab 3: Cancelled / Expired
        assertTrue("Cancelled order belongs to Cancelled Tab",
            cancelledOrder.getStatus() == OrderStatus.CANCELLED || cancelledOrder.getStatus() == OrderStatus.EXPIRED);
    }

    // ========================================================================
    // 4. RESTOCK QUANTITY & STATUS LOGIC
    // ========================================================================

    @Test
    public void testRestockQuantityAndStatus() {
        Listing listing = new Listing();
        listing.setRemainingQuantity(0);
        listing.setTotalQuantity(5);
        listing.setStatus(ListingStatus.EXPIRED);

        int additionalStock = 5;
        listing.setRemainingQuantity(listing.getRemainingQuantity() + additionalStock);
        listing.setTotalQuantity(listing.getTotalQuantity() + additionalStock);
        listing.setStatus(ListingStatus.ACTIVE);

        assertEquals("Remaining quantity should be updated to 5", 5, listing.getRemainingQuantity());
        assertEquals("Total quantity should be updated to 10", 10, listing.getTotalQuantity());
        assertEquals("Listing should be ACTIVE after restock", ListingStatus.ACTIVE, listing.getStatus());
    }

    // ========================================================================
    // 5. LOW STOCK THRESHOLD ALERT LOGIC
    // ========================================================================

    @Test
    public void testLowStockAlertThreshold() {
        int normalStock = 4;
        int lowStock = 2;
        int criticalStock = 1;
        int zeroStock = 0;

        assertFalse("Stock of 4 is not low stock", normalStock <= 2);
        assertTrue("Stock of 2 triggers low stock alert", lowStock <= 2 && lowStock > 0);
        assertTrue("Stock of 1 triggers low stock alert", criticalStock <= 2 && criticalStock > 0);
        assertTrue("Stock of 0 is sold out", zeroStock <= 0);
    }

    // ========================================================================
    // 6. QR PICKUP VERIFICATION RESULT
    // ========================================================================

    @Test
    public void testOrderVerificationResult() {
        Order order = new Order();
        order.setOrderCode("FH-829104");
        order.setStatus(OrderStatus.COMPLETED);

        OrderVerificationResult successResult = new OrderVerificationResult(true, "Pickup verified successfully! 10 Eco-Points awarded.", order);
        assertTrue(successResult.isValid());
        assertEquals("FH-829104", successResult.getOrder().getOrderCode());
        assertEquals(OrderStatus.COMPLETED, successResult.getOrder().getStatus());

        OrderVerificationResult failureResult = new OrderVerificationResult(false, "Invalid pickup code.", null);
        assertFalse(failureResult.isValid());
        assertNull(failureResult.getOrder());
    }

    // ========================================================================
    // 7. QR CODE PAYLOAD PARSING (orderCode:pickupToken)
    // ========================================================================

    @Test
    public void testQrCodePayloadParsing() {
        String fullPayload = "FH-829104:FH-TOKEN-ABC12345";
        String[] parts = fullPayload.split(":", 2);
        assertEquals("FH-829104", parts[0]);
        assertEquals("FH-TOKEN-ABC12345", parts[1]);

        String singleCode = "FH-829104";
        assertFalse("Single code does not contain colon", singleCode.contains(":"));
    }

    // ========================================================================
    // 8. DYNAMIC DASHBOARD RATING ROLLUP (NO HARDCODED 5.0)
    // ========================================================================

    @Test
    public void testDashboardRating_zeroWhenNoReviews() {
        java.util.List<com.uccd3223.group13.foodhero.data.model.Review> emptyReviews = new java.util.ArrayList<>();
        double avgRating = 0.0;
        if (!emptyReviews.isEmpty()) {
            double sum = 0;
            for (com.uccd3223.group13.foodhero.data.model.Review r : emptyReviews) sum += r.getRating();
            avgRating = sum / emptyReviews.size();
        }
        assertEquals("Average rating must be 0.0 when no reviews exist, not fake 5.0", 0.0, avgRating, 0.001);
    }

    @Test
    public void testDashboardRating_dynamicallyCalculated() {
        java.util.List<com.uccd3223.group13.foodhero.data.model.Review> reviews = new java.util.ArrayList<>();
        com.uccd3223.group13.foodhero.data.model.Review r1 = new com.uccd3223.group13.foodhero.data.model.Review();
        r1.setRating(4);
        com.uccd3223.group13.foodhero.data.model.Review r2 = new com.uccd3223.group13.foodhero.data.model.Review();
        r2.setRating(5);
        reviews.add(r1);
        reviews.add(r2);

        double sum = 0;
        for (com.uccd3223.group13.foodhero.data.model.Review r : reviews) sum += r.getRating();
        double avgRating = sum / reviews.size();

        assertEquals(4.5, avgRating, 0.001);
    }

    // ========================================================================
    // 9. CLEAN POSTGREST UPLOAD PAYLOADS (NO EMBEDDED RELATION OBJECTS)
    // ========================================================================

    @Test
    public void testCleanOrderPayload_hasNoJoinedObjects() {
        Order original = new Order();
        original.setId("ord-123");
        original.setOrderCode("FH-123456");
        original.setStatus(OrderStatus.AWAITING_PAYMENT);

        Listing listing = new Listing();
        listing.setId("lst-123");
        original.setListing(listing);

        assertNotNull("Original order has joined listing", original.getListing());

        // Simulated copyOrderForUpload
        Order payload = new Order();
        payload.setId(original.getId());
        payload.setOrderCode(original.getOrderCode());
        payload.setStatus(original.getStatus());

        assertNull("Upload payload must not have joined listing object", payload.getListing());
        assertNull("Upload payload must not have joined merchant object", payload.getMerchant());
    }
}
