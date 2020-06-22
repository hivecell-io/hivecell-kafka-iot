package com.rickerlyman.iot.sense.core.transform;

import com.rickerlyman.iot.sense.core.model.SenseReadingInternalCompanion;
import com.rickerlyman.iot.sense.core.util.Rooms;
import com.rickerlyman.iot.schema.SenseReadingInternal;
import com.rickerlyman.iot.schema.SenseReadingOutput;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.stream.Stream;

import static com.rickerlyman.iot.sense.core.math.Formulas.*;
import static com.rickerlyman.iot.common.util.TimeUtils.PT1S;

public class AggregatorOps {

    private static Logger logger = LoggerFactory.getLogger(AggregatorOps.class);

    private static final int SENSOR_ID = 0, TIMESTAMP = 1, TEMP = 2, HUMIDITY = 3, PRESSURE = 4;

    public static Iterable<KeyValue<String, SenseReadingInternal>> parseTemperature(String key, String value) {
        String[] columns = value.split(",");

        if (columns.length < 3) {
            logger.error("Invalid temperature reading: {}", value);
            return Collections.emptyList();
        }

        boolean isEmptyColumn = Stream
            .of(columns[SENSOR_ID], columns[TIMESTAMP], columns[TEMP], columns[HUMIDITY], columns[PRESSURE])
            .anyMatch(String::isEmpty);

        if (isEmptyColumn) {
            logger.error("Invalid temperature reading: {}", value);
            return Collections.emptyList();
        }

        String sensorId = columns[SENSOR_ID];
        Long timestamp = Long.valueOf(columns[TIMESTAMP]) * PT1S;
        String location = Rooms.findRoom(sensorId);

        int tLen = columns[TEMP].length() - 1;
        float temp = truncatePrecision(Float.valueOf(columns[TEMP].substring(0, tLen)), 2);

        int hLen = columns[HUMIDITY].length() - 1;
        float humidity = truncatePrecision(Float.valueOf(columns[HUMIDITY].substring(0, hLen)), 2);

        int pLen = columns[PRESSURE].length() - 3;
        float pressure = truncatePrecision(Float.valueOf(columns[PRESSURE].substring(0, pLen)), 2);

        return Collections
            .singletonList(KeyValue
                .pair(location, SenseReadingInternalCompanion.from(sensorId, timestamp, location, temp, humidity, pressure, 0)));
    }

    public static SenseReadingInternal initTempCollection() {
        return SenseReadingInternalCompanion.empty();
    }

    public static SenseReadingInternal collectTemp(String location, SenseReadingInternal reading, SenseReadingInternal acc) {
        acc.setSensorId(reading.getSensorId());
        acc.setLocationId(reading.getLocationId());
        acc.setTemperature(acc.getTemperature() + reading.getTemperature());
        acc.setRelativeHumidity(acc.getRelativeHumidity() + reading.getRelativeHumidity());
        acc.setPressure(acc.getPressure() + reading.getPressure());
        acc.setTimestamp(Long.max(reading.getTimestamp(), acc.getTimestamp()));
        acc.setCount(acc.getCount() + 1);
        return acc;
    }

    public static SenseReadingOutput calculateAvgTemp(SenseReadingInternal acc) {
        float avgTemperature = acc.getTemperature() / acc.getCount();
        float avgRelativeHumidity = acc.getRelativeHumidity() / acc.getCount();
        float avgPressure = acc.getPressure() / acc.getCount();
        return new SenseReadingOutput(acc.getSensorId(), acc.getTimestamp(), acc.getLocationId(),
            truncatePrecision(avgTemperature, 2),
            truncatePrecision(avgRelativeHumidity, 2),
            truncatePrecision(avgPressure, 2),
            truncatePrecision(summerSimmerIndex(avgTemperature, avgRelativeHumidity), 2),
            truncatePrecision(heatIndex(avgTemperature, avgRelativeHumidity), 2),
            truncatePrecision(dewPoint(avgTemperature, avgRelativeHumidity), 2),
            truncatePrecision(humIndexByTempDP(avgTemperature, avgRelativeHumidity), 2));
    }

}
