package com.uccd3223.group13.foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Profile implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private UserRole role;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("student_id")
    private String studentId;

    @SerializedName("faculty")
    private String faculty;

    @SerializedName("institution_code")
    private String institutionCode;

    @SerializedName("institution_name")
    private String institutionName;

    @SerializedName("institution_affiliation")
    private String institutionAffiliation;

    @SerializedName("email_verified_at")
    private String emailVerifiedAt;

    @SerializedName("campus_id")
    private String campusId;

    @SerializedName("last_active_role")
    private UserRole lastActiveRole;

    @SerializedName("eco_points")
    private int ecoPoints;

    @SerializedName("meals_rescued")
    private int mealsRescued;

    @SerializedName("money_saved")
    private double moneySaved;

    @SerializedName("co2_prevented")
    private double co2Prevented;

    public Profile() {
    }

    public Profile(String id, String email, UserRole role, String fullName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role != null ? role : UserRole.STUDENT;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getInstitutionCode() { return institutionCode; }
    public String getInstitutionName() { return institutionName; }
    public String getInstitutionAffiliation() { return institutionAffiliation; }
    public boolean isEmailVerified() { return emailVerifiedAt != null && !emailVerifiedAt.trim().isEmpty(); }
    public String getCampusId() { return campusId; }
    public void setCampusId(String campusId) { this.campusId = campusId; }
    public UserRole getLastActiveRole() { return lastActiveRole != null ? lastActiveRole : getRole(); }
    public void setLastActiveRole(UserRole lastActiveRole) { this.lastActiveRole = lastActiveRole; }

    public int getEcoPoints() {
        return ecoPoints;
    }

    public void setEcoPoints(int ecoPoints) {
        this.ecoPoints = ecoPoints;
    }

    public int getMealsRescued() {
        return mealsRescued;
    }

    public void setMealsRescued(int mealsRescued) {
        this.mealsRescued = mealsRescued;
    }

    public double getMoneySaved() {
        return moneySaved;
    }

    public void setMoneySaved(double moneySaved) {
        this.moneySaved = moneySaved;
    }

    public double getCo2Prevented() {
        return co2Prevented;
    }

    public void setCo2Prevented(double co2Prevented) {
        this.co2Prevented = co2Prevented;
    }
}
