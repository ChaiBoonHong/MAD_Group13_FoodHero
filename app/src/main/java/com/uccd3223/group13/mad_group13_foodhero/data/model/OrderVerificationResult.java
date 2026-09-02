package com.uccd3223.group13.mad_group13_foodhero.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class OrderVerificationResult implements Serializable {
    @SerializedName("is_valid")
    private boolean isValid;

    @SerializedName("message")
    private String message;

    @SerializedName("order")
    private Order order;

    public OrderVerificationResult() {
    }

    public OrderVerificationResult(boolean isValid, String message, Order order) {
        this.isValid = isValid;
        this.message = message;
        this.order = order;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
