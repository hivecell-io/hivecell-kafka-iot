package com.rickerlyman.iot.sense.core.model;

import com.rickerlyman.iot.schema.SenseReadingInternal;

public class SenseReadingInternalCompanion {
    private SenseReadingInternalCompanion() {
    }

    public static SenseReadingInternal from(String sensorId, Long timestamp, String locationId, Float temperature, Float relativeHumidity, Float pressure, int count) {
        SenseReadingInternal senseReading = new SenseReadingInternal();
        senseReading.setSensorId(sensorId);
        senseReading.setCount(count);
        senseReading.setTimestamp(timestamp);
        senseReading.setLocationId(locationId);
        senseReading.setTemperature(temperature);
        senseReading.setRelativeHumidity(relativeHumidity);
        senseReading.setPressure(pressure);
        return senseReading;
    }

    public static SenseReadingInternal empty() {
        SenseReadingInternal senseReading = new SenseReadingInternal();
        senseReading.setSensorId(null);
        senseReading.setCount(0);
        senseReading.setTimestamp(0L);
        senseReading.setLocationId(null);
        senseReading.setTemperature(0.0f);
        senseReading.setRelativeHumidity(0.0f);
        senseReading.setPressure(0.0f);
        return senseReading;
    }

}
