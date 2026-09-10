package com.uccd3223.group13.foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import com.uccd3223.group13.foodhero.util.CampusBoundaryManager;
import org.junit.Test;

public class CampusBoundaryTest {

    @Test
    public void distanceBetweenSamePoint_isZero() {
        assertEquals(0.0, CampusBoundaryManager.calculateDistanceMeters(3.0, 101.0, 3.0, 101.0), 0.001);
    }

    @Test
    public void routeCalculation_usesProvidedCoordinatesWithoutInventedCampusFallback() {
        RouteResult route = CampusBoundaryManager.calculateCampusRoute(
            4.312000, 101.152000,
            4.335800, 101.141200,
            TravelMode.WALKING
        );

        assertFalse("No institution-specific entrance may be invented", route.isFallbackEntrance());
        assertNull("No invented entrance name may be returned", route.getEntranceName());
        assertTrue("Distance should be positive", route.getDistanceMeters() > 0);
        assertTrue("Duration should be positive", route.getDurationMinutes() > 0);
        assertEquals(4, route.getPoints().size());
    }
}
