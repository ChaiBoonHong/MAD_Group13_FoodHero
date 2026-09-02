package com.uccd3223.group13.foodhero;

import static org.junit.Assert.*;

import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import com.uccd3223.group13.foodhero.util.CampusBoundaryManager;
import org.junit.Test;

public class CampusBoundaryTest {

    @Test
    public void testCampusCenter_isInsideBoundary() {
        boolean inside = CampusBoundaryManager.isInsideCampus(
            CampusBoundaryManager.CAMPUS_CENTER_LAT,
            CampusBoundaryManager.CAMPUS_CENTER_LNG
        );
        assertTrue("UTAR Kampar official center must be inside campus polygon", inside);
    }

    @Test
    public void testStudentPavilionI_isInsideBoundary() {
        // Pavilion I: (4.335800, 101.141200)
        boolean inside = CampusBoundaryManager.isInsideCampus(4.335800, 101.141200);
        assertTrue("Student Pavilion I must be inside campus polygon", inside);
    }

    @Test
    public void testKamparTown_isOutsideBoundary() {
        // Kampar Old Town / Grand Kampar Hotel area: ~4.312000, 101.152000
        boolean inside = CampusBoundaryManager.isInsideCampus(4.312000, 101.152000);
        assertFalse("Kampar town point must be outside UTAR Kampar campus polygon", inside);
    }

    @Test
    public void testNearestEntrance_fallbackCalculation() {
        // A point in Westlake / Bandar Barat (outside campus): 4.325000, 101.136000
        CampusLandmark entrance = CampusBoundaryManager.findNearestEntrance(4.325000, 101.136000);
        assertNotNull("Nearest entrance should be found", entrance);
        assertEquals("West Gate (Hostel / Sport Complex Entrance)", "West Gate (Sports Complex)", entrance.getName());
    }

    @Test
    public void testRouteCalculation_withOutsideOriginSnapsToEntrance() {
        // Point outside campus routing to Pavilion I
        RouteResult route = CampusBoundaryManager.calculateCampusRoute(
            4.312000, 101.152000,
            4.335800, 101.141200,
            TravelMode.WALKING
        );

        assertTrue("Fallback entrance should be flagged", route.isFallbackEntrance());
        assertNotNull("Entrance name must be populated", route.getEntranceName());
        assertTrue("Distance should be positive", route.getDistanceMeters() > 0);
        assertTrue("Duration should be positive", route.getDurationMinutes() > 0);
    }
}
