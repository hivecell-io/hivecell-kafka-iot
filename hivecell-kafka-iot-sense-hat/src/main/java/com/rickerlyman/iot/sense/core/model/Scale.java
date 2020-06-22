package com.rickerlyman.iot.sense.core.model;

import lombok.Getter;

public enum Scale {
    CELSIUS("C"), FAHRENHEIT("F");

    @Getter
    private final String metric;

    Scale(String metric) {
        this.metric = metric;
    }

    public static Scale of(String name) {
        switch (name) {
            case "c":
            case "C":
                return Scale.CELSIUS;
            case "f":
            case "F":
                return Scale.FAHRENHEIT;
            default:
                throw new IllegalArgumentException(String.format("Temperature metric %s is not supported", name));
        }
    }
}
