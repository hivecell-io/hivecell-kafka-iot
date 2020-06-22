package com.rickerlyman.iot.common.catalog.store;

import lombok.Getter;

import java.time.Duration;

@Getter
public class SimpleStoreMetaData extends StoreMetaData {

    private SimpleStoreMetaData(String name, Duration retention) {
        super(name, retention);
    }

    public static SimpleStoreMetaData with(String name, String retentionType, long retentionPeriod) {
        return with(name, TimeIndicator.valueOf(retentionType), retentionPeriod);
    }

    public static SimpleStoreMetaData with(String name, TimeIndicator timeIndicatorType, long retentionPeriod) {
        return new SimpleStoreMetaData(name, of(timeIndicatorType, retentionPeriod));
    }


}
