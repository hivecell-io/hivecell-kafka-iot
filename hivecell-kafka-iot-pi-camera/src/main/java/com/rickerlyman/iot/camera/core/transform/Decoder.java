package com.rickerlyman.iot.camera.core.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickerlyman.iot.camera.core.model.RawFrame;
import com.rickerlyman.iot.common.util.Jackson;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

public class Decoder {

    public static Iterable<RawFrame> decode(String value) {
        Map<String, String> frameProperties = Jackson.fromJsonString(value, new TypeReference<>() {
        });

        if (!frameProperties.containsKey("time") || !frameProperties.containsKey("data"))
            return Collections.emptyList();

        return Collections.singletonList(new RawFrame(
            Long.valueOf(frameProperties.get("time")),
            Base64.getDecoder().decode(frameProperties.get("data"))));
    }

}
