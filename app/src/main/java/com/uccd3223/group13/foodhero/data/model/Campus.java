package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Campus implements Serializable {
    @SerializedName("id") private String id;
    @SerializedName("institution_code") private String institutionCode;
    @SerializedName("name") private String name;
    @SerializedName("address") private String address;
    @SerializedName("latitude") private double latitude;
    @SerializedName("longitude") private double longitude;
    @SerializedName("boundary_coordinates") private String boundaryCoordinates;
    @SerializedName("is_main") private boolean main;

    public String getId() { return id; }
    public String getInstitutionCode() { return institutionCode; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getBoundaryCoordinates() { return boundaryCoordinates; }
    public boolean isMain() { return main; }
}
