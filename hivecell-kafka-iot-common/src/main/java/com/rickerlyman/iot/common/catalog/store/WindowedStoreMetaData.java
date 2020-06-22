package com.rickerlyman.iot.common.catalog.store;

import lombok.Getter;

import java.time.Duration;

@Getter
public class WindowedStoreMetaData extends StoreMetaData {
    private Duration size;
    private Duration afterWindowEnd;

    private WindowedStoreMetaData(String name, Duration retention, Duration size, Duration afterWindowEnd) {
        super(name, retention);
        this.size = size;
        this.afterWindowEnd = afterWindowEnd;
    }

    public static WindowedStoreMetaData with(String name, String timeIndicatorType, long retentionPeriod, long windowSize, long afterWindowEnd) {
        return with(name, TimeIndicator.valueOf(timeIndicatorType), retentionPeriod, windowSize, afterWindowEnd);
    }

    public static WindowedStoreMetaData with(String name, TimeIndicator timeIndicatorType, long retentionPeriod, long windowSize, long afterWindowEnd) {
        return new WindowedStoreMetaData(name,
            of(timeIndicatorType, retentionPeriod),
            of(timeIndicatorType, windowSize),
            of(timeIndicatorType, afterWindowEnd));
    }
}
