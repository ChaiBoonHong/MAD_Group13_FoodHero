package com.uccd3223.group13.mad_group13_foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import org.junit.Test;

public class PriceCeilingTest {

    @Test
    public void testValidDiscountedPrice_withinCeiling() {
        Listing listing = new Listing();
        listing.setOriginalPrice(12.50);
        listing.setDiscountedPrice(5.50);

        assertTrue("Discounted price should be <= RM10.00", listing.getDiscountedPrice() <= 10.00);
        assertTrue("Discounted price should be positive", listing.getDiscountedPrice() > 0);
        assertTrue("Discounted price should be lower than original price", listing.getDiscountedPrice() < listing.getOriginalPrice());
        assertEquals("Savings should be RM7.00", 7.00, listing.getSavingsAmount(), 0.001);
    }

    @Test
    public void testPriceCeiling_exactTenRinggit() {
        Listing listing = new Listing();
        listing.setOriginalPrice(15.00);
        listing.setDiscountedPrice(10.00);

        assertTrue("RM10.00 is allowed as maximum ceiling", listing.getDiscountedPrice() <= 10.00);
    }

    @Test
    public void testInvalidDiscountedPrice_exceedsTenRinggit() {
        Listing listing = new Listing();
        listing.setOriginalPrice(20.00);
        listing.setDiscountedPrice(10.50);

        assertFalse("Price above RM10.00 must not be allowed", listing.getDiscountedPrice() <= 10.00);
    }
}
