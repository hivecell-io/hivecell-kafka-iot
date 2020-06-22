package com.rickerlyman.iot.camera.infrastructure.vision;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickerlyman.iot.common.util.Resources;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Labels {

    @SneakyThrows
    public static Map<Integer, String> getLabels(String labelPath) {
        Objects.requireNonNull(labelPath, "labelPath");

        Map<String, Integer> nameMapping = Resources.byAbsolutePath(labelPath,
            new TypeReference<Map<String, Integer>>() {
            });

        return nameMapping.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }


}
