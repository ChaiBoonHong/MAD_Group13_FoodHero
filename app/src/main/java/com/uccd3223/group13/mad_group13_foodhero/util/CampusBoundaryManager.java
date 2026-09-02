package com.uccd3223.group13.mad_group13_foodhero.util;

import com.uccd3223.group13.mad_group13_foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.mad_group13_foodhero.data.model.GeoPoint;
import com.uccd3223.group13.mad_group13_foodhero.data.model.RouteResult;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ServiceArea;
import com.uccd3223.group13.mad_group13_foodhero.data.model.TravelMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CampusBoundaryManager {
    public static final double CAMPUS_CENTER_LAT = 4.336214;
    public static final double CAMPUS_CENTER_LNG = 101.142111;

    // Authoritative UTAR Kampar Campus Polygon Perimeter
    private static final List<GeoPoint> CAMPUS_POLYGON = Arrays.asList(
        new GeoPoint(4.344500, 101.135000),
        new GeoPoint(4.344500, 101.150000),
        new GeoPoint(4.330000, 101.150000),
        new GeoPoint(4.327000, 101.143000),
        new GeoPoint(4.330000, 101.135000)
    );

    // Approved UTAR Kampar Entrances
    private static final List<CampusLandmark> ENTRANCES = Arrays.asList(
        new CampusLandmark("ent_east", "East Gate (Main Entrance)", "entrance", 4.338500, 101.146500),
        new CampusLandmark("ent_west", "West Gate (Sports Complex)", "entrance", 4.332800, 101.137200),
        new CampusLandmark("ent_north", "North Gate", "entrance", 4.343200, 101.141500)
    );

    /**
     * Ray-casting algorithm to determine if point is within UTAR Kampar polygon.
     */
    public static boolean isInsideCampus(double lat, double lng) {
        int i, j;
        boolean inside = false;
        int nvert = CAMPUS_POLYGON.size();
        for (i = 0, j = nvert - 1; i < nvert; j = i++) {
            double vertXi = CAMPUS_POLYGON.get(i).getLongitude();
            double vertYi = CAMPUS_POLYGON.get(i).getLatitude();
            double vertXj = CAMPUS_POLYGON.get(j).getLongitude();
            double vertYj = CAMPUS_POLYGON.get(j).getLatitude();

            if (((vertYi > lat) != (vertYj > lat)) &&
                (lng < (vertXj - vertXi) * (lat - vertYi) / (vertYj - vertYi) + vertXi)) {
                inside = !inside;
            }
        }
        return inside;
    }

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

    /**
     * Find nearest approved entrance when student is outside campus.
     */
    public static CampusLandmark findNearestEntrance(double userLat, double userLng) {
        CampusLandmark nearest = ENTRANCES.get(0);
        double minDistance = Double.MAX_VALUE;

        for (CampusLandmark entrance : ENTRANCES) {
            double dist = calculateDistanceMeters(userLat, userLng, entrance.getLatitude(), entrance.getLongitude());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = entrance;
            }
        }
        return nearest;
    }

    /**
     * Calculate campus route, enforcing on-campus restrictions and fallback entrance snapping.
     */
    public static RouteResult calculateCampusRoute(double userLat, double userLng, double destLat, double destLng, TravelMode mode) {
        RouteResult result = new RouteResult();
        result.setTravelMode(mode);

        boolean userInside = isInsideCampus(userLat, userLng);
        double startLat = userLat;
        double startLng = userLng;

        if (!userInside) {
            CampusLandmark entrance = findNearestEntrance(userLat, userLng);
            startLat = entrance.getLatitude();
            startLng = entrance.getLongitude();
            result.setFallbackEntrance(true);
            result.setEntranceName(entrance.getName());
        }

        double distanceMeters = calculateDistanceMeters(startLat, startLng, destLat, destLng);
        // Add 15% path tortuosity factor for real campus walkway turns
        distanceMeters = distanceMeters * 1.15;
        result.setDistanceMeters(distanceMeters);

        // Calculate duration based on travel mode speed
        double speedMps = (mode.getAvgSpeedKmh() * 1000.0) / 3600.0;
        int durationSeconds = (int) Math.round(distanceMeters / speedMps);
        result.setDurationSeconds(Math.max(60, durationSeconds));

        // Generate route waypoints (intermediate campus paths)
        List<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(new GeoPoint(startLat, startLng));
        waypoints.add(new GeoPoint((startLat * 2 + destLat) / 3, (startLng * 2 + destLng) / 3));
        waypoints.add(new GeoPoint((startLat + destLat * 2) / 3, (startLng + destLng * 2) / 3));
        waypoints.add(new GeoPoint(destLat, destLng));
        result.setPoints(waypoints);

        return result;
    }

    public static ServiceArea getUtarKamparServiceArea() {
        ServiceArea area = new ServiceArea();
        area.setName("UTAR Kampar Campus");
        area.setCenterLatitude(CAMPUS_CENTER_LAT);
        area.setCenterLongitude(CAMPUS_CENTER_LNG);
        area.setPolygonCoordinates(CAMPUS_POLYGON);
        return area;
    }

    public static List<CampusLandmark> getSeededLandmarks() {
        List<CampusLandmark> list = new ArrayList<>(ENTRANCES);
        list.add(new CampusLandmark("l1", "Student Pavilion I (Cafeteria)", "student_pavilion", 4.335800, 101.141200));
        list.add(new CampusLandmark("l2", "Student Pavilion II (Cafeteria)", "student_pavilion", 4.337500, 101.143800));
        list.add(new CampusLandmark("l3", "Block A - Heritage Hall", "landmark", 4.339200, 101.144500));
        list.add(new CampusLandmark("l4", "Dewan Tun Dr Ling Liong Sik", "landmark", 4.338800, 101.143500));
        list.add(new CampusLandmark("l5", "Block N - FICT", "academic_block", 4.336500, 101.140200));
        list.add(new CampusLandmark("l6", "Block K - FEGT", "academic_block", 4.335200, 101.139500));
        list.add(new CampusLandmark("l7", "Block D - FBF", "academic_block", 4.337800, 101.142000));
        list.add(new CampusLandmark("l8", "UTAR Kampar Library", "academic_block", 4.338200, 101.144000));
        return list;
    }
}
