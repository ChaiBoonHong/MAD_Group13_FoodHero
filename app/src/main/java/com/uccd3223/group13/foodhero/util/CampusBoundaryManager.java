package com.uccd3223.group13.foodhero.util;

import com.uccd3223.group13.foodhero.data.model.GeoPoint;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import java.util.ArrayList;
import java.util.List;

public class CampusBoundaryManager {

    /**
     * Haversine formula to compute great-circle distance in meters between two coordinates.
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** Estimates a route from real caller and listing coordinates without a campus-specific fallback. */
    public static RouteResult calculateCampusRoute(double userLat, double userLng, double destLat, double destLng, TravelMode mode) {
        RouteResult result = new RouteResult();
        result.setTravelMode(mode);
        double distanceMeters = calculateDistanceMeters(userLat, userLng, destLat, destLng);
        // Add 15% path tortuosity factor for real campus walkway turns
        distanceMeters = distanceMeters * 1.15;
        result.setDistanceMeters(distanceMeters);

        // Calculate duration based on travel mode speed
        double speedMps = (mode.getAvgSpeedKmh() * 1000.0) / 3600.0;
        int durationSeconds = (int) Math.round(distanceMeters / speedMps);
        result.setDurationSeconds(Math.max(60, durationSeconds));

        // Generate route waypoints (intermediate campus paths)
        List<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(new GeoPoint(userLat, userLng));
        waypoints.add(new GeoPoint((userLat * 2 + destLat) / 3, (userLng * 2 + destLng) / 3));
        waypoints.add(new GeoPoint((userLat + destLat * 2) / 3, (userLng + destLng * 2) / 3));
        waypoints.add(new GeoPoint(destLat, destLng));
        result.setPoints(waypoints);

        return result;
    }
}
