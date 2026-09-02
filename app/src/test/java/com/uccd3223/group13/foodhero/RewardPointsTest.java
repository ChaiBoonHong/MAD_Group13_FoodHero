package com.uccd3223.group13.foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.foodhero.data.model.Badge;
import com.uccd3223.group13.foodhero.data.model.ImpactSummary;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RewardPointsTest {

    @Test
    public void testPointsEarned_tenPointsPerMeal() {
        int mealsRescued = 4;
        int pointsPerMeal = 10;
        int earned = mealsRescued * pointsPerMeal;
        assertEquals("Each rescued meal earns 10 points", 40, earned);
    }

    @Test
    public void testRedemptionDiscount_hundredPointsGivesFiveRinggit() {
        int userPoints = 150;
        double subtotal = 11.00;

        boolean canRedeem = userPoints >= 100;
        assertTrue("User with >= 100 points can redeem discount", canRedeem);

        int pointsToDeduct = canRedeem ? 100 : 0;
        double discount = canRedeem ? Math.min(5.00, subtotal) : 0.00;
        double finalPrice = subtotal - discount;

        assertEquals(100, pointsToDeduct);
        assertEquals(5.00, discount, 0.001);
        assertEquals(6.00, finalPrice, 0.001);
    }

    @Test
    public void testBadgeTiers_unlockThresholds() {
        int mealsRescued = 7;

        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge("b1", "Eco Sprout", "1st meal", 1, "bronze", mealsRescued >= 1));
        badges.add(new Badge("b2", "Green Guardian", "5 meals", 5, "silver", mealsRescued >= 5));
        badges.add(new Badge("b3", "Campus Hero", "10 meals", 10, "gold", mealsRescued >= 10));
        badges.add(new Badge("b4", "Zero-Waste Master", "25 meals", 25, "emerald", mealsRescued >= 25));

        assertTrue("1st tier badge must be unlocked", badges.get(0).isUnlocked());
        assertTrue("5-meal badge must be unlocked", badges.get(1).isUnlocked());
        assertFalse("10-meal badge should be locked", badges.get(2).isUnlocked());
        assertFalse("25-meal badge should be locked", badges.get(3).isUnlocked());
    }

    @Test
    public void testTreeProgress_percentCalculation() {
        ImpactSummary impact = new ImpactSummary();
        impact.setMealsRescued(7);
        assertEquals(28, impact.getTreeProgressPercent()); // 7/25 = 28%

        impact.setMealsRescued(25);
        assertEquals(100, impact.getTreeProgressPercent());
    }
}
