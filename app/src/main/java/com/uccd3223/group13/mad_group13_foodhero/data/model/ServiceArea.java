package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ServiceArea implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("center_latitude")
    private double centerLatitude;

    @SerializedName("center_longitude")
    private double centerLongitude;

    @SerializedName("polygon_coordinates")
    private List<GeoPoint> polygonCoordinates;

    public ServiceArea() {
        this.centerLatitude = 4.336214;
        this.centerLongitude = 101.142111;
        this.polygonCoordinates = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public List<GeoPoint> getPolygonCoordinates() {
        return polygonCoordinates;
    }

    public void setPolygonCoordinates(List<GeoPoint> polygonCoordinates) {
        this.polygonCoordinates = polygonCoordinates;
    }
}
