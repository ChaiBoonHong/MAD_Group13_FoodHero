package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;

public class InstitutionMatch {
    @SerializedName("institution_code") private String code;
    @SerializedName("institution_name") private String name;
    @SerializedName("affiliation_type") private String affiliation;
    public String getCode(){return code;}
    public String getName(){return name;}
    public String getAffiliation(){return affiliation;}
}
