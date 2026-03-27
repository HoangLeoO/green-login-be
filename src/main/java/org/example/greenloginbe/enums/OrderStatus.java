package org.example.greenloginbe.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("pending"),
    PAID("paid"),
    CANCELLED("cancelled"),
    DELIVERING("delivering"),
    DEBT("debt"),
    PARTIAL_PAID("partial_paid");

    @com.fasterxml.jackson.annotation.JsonValue
    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
