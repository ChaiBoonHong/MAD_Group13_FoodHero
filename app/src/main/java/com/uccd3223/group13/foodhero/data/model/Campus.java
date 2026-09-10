package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Campus implements Serializable {
    @SerializedName("id") private String id;
    @SerializedName("institution_code") private String institutionCode;
    @SerializedName("name") private String name;
    @SerializedName("address") private String address;
    @SerializedName("latitude") private double latitude;
    @SerializedName("longitude") private double longitude;
    @SerializedName("boundary_coordinates") private List<GeoPoint> boundaryCoordinates;
    @SerializedName("is_main") private boolean main;

    public String getId() { return id; }
    public String getInstitutionCode() { return institutionCode; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<GeoPoint> getBoundaryCoordinates() {
        return boundaryCoordinates == null ? Collections.emptyList() : boundaryCoordinates;
    }
    public boolean isMain() { return main; }
}
