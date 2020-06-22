package com.rickerlyman.iot.common.catalog.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public abstract class StoreMetaData {
    private String name;
    private Duration retention;

    public enum TimeIndicator {
        PT_S, PT_M, PT_H, P_D, P_W;
    }

    protected static Duration of(TimeIndicator period, long time) {
        switch (period) {
            case PT_S:
                return Duration.ofSeconds(time);
            case PT_M:
                return Duration.ofMinutes(time);
            case PT_H:
                return Duration.ofHours(time);
            case P_D:
                return Duration.ofDays(time);
            case P_W:
                return Duration.ofDays(time * 7);
            default:
                throw new IllegalArgumentException(String.format("Unsupported retention period: %s", period));
        }
    }
}
