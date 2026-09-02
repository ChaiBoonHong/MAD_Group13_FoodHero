package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RouteResult implements Serializable {
    @SerializedName("distance_meters")
    private double distanceMeters;

    @SerializedName("duration_seconds")
    private int durationSeconds;

    @SerializedName("encoded_polyline")
    private String encodedPolyline;

    @SerializedName("travel_mode")
    private TravelMode travelMode;

    @SerializedName("points")
    private List<GeoPoint> points;

    @SerializedName("is_fallback_entrance")
    private boolean isFallbackEntrance;

    @SerializedName("entrance_name")
    private String entranceName;

    public RouteResult() {
        this.points = new ArrayList<>();
        this.travelMode = TravelMode.WALKING;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getDistanceKm() {
        return distanceMeters / 1000.0;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getDurationMinutes() {
        return Math.max(1, (int) Math.round(durationSeconds / 60.0));
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    public TravelMode getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(TravelMode travelMode) {
        this.travelMode = travelMode;
    }

    public List<GeoPoint> getPoints() {
        return points;
    }

    public void setPoints(List<GeoPoint> points) {
        this.points = points;
    }

    public boolean isFallbackEntrance() {
        return isFallbackEntrance;
    }

    public void setFallbackEntrance(boolean fallbackEntrance) {
        isFallbackEntrance = fallbackEntrance;
    }

    public String getEntranceName() {
        return entranceName;
    }

    public void setEntranceName(String entranceName) {
        this.entranceName = entranceName;
    }
}
