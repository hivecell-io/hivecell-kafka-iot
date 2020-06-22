package com.rickerlyman.iot.camera.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class ObjectDetectionResult {
    private final int classId;
    private final String className;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final float confidence;

    public static ObjectDetectionResult detectionResult(int classId, String className, double x, double y, double width, double height, float confidence) {
        return new ObjectDetectionResult(classId, className, x, y, width, height, confidence);
    }
}
