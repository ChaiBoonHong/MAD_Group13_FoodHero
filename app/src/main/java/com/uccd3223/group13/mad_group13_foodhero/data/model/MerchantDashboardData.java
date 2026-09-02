package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MerchantDashboardData implements Serializable {
    @SerializedName("revenue_recovered")
    private double revenueRecovered;

    @SerializedName("food_diverted_kg")
    private double foodDivertedKg;

    @SerializedName("orders_completed")
    private int ordersCompleted;

    @SerializedName("average_rating")
    private double averageRating;

    @SerializedName("active_listings_count")
    private int activeListingsCount;

    @SerializedName("low_stock_alerts_count")
    private int lowStockAlertsCount;

    @SerializedName("unread_notifications_count")
    private int unreadNotificationsCount;

    @SerializedName("recent_orders")
    private List<Order> recentOrders;

    public MerchantDashboardData() {
        this.recentOrders = new ArrayList<>();
    }

    public double getRevenueRecovered() {
        return revenueRecovered;
    }

    public void setRevenueRecovered(double revenueRecovered) {
        this.revenueRecovered = revenueRecovered;
    }

    public double getFoodDivertedKg() {
        return foodDivertedKg;
    }

    public void setFoodDivertedKg(double foodDivertedKg) {
        this.foodDivertedKg = foodDivertedKg;
    }

    public int getOrdersCompleted() {
        return ordersCompleted;
    }

    public void setOrdersCompleted(int ordersCompleted) {
        this.ordersCompleted = ordersCompleted;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getActiveListingsCount() {
        return activeListingsCount;
    }

    public void setActiveListingsCount(int activeListingsCount) {
        this.activeListingsCount = activeListingsCount;
    }

    public int getLowStockAlertsCount() {
        return lowStockAlertsCount;
    }

    public void setLowStockAlertsCount(int lowStockAlertsCount) {
        this.lowStockAlertsCount = lowStockAlertsCount;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public List<Order> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<Order> recentOrders) {
        this.recentOrders = recentOrders;
    }
}
