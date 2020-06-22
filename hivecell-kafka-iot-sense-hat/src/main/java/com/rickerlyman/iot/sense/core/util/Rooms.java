package com.rickerlyman.iot.sense.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickerlyman.iot.common.util.Resources;

import java.util.Map;
import java.util.Objects;

public class Rooms {

    protected static final Map<String, String> ROOM_MAPPING =
        Resources.asObject("/location-mapping.json",
            new TypeReference<Map<String, String>>() {
            });

    private Rooms() {
    }

    public static String findRoom(String sensorId) {
        if (Objects.isNull(sensorId)) {
            return null;
        }

        return ROOM_MAPPING.getOrDefault(sensorId, "N/A");
    }
}
