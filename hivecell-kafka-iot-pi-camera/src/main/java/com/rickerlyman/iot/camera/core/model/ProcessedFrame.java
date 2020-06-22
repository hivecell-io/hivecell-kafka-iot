package com.rickerlyman.iot.camera.core.model;

import lombok.Getter;

import java.util.List;

@Getter
public class ProcessedFrame<T> {
    public static final ProcessedFrame<?> EMPTY = new ProcessedFrame<>(null, null);
    private final Long processingTime;
    private final List<T> detectionResults;

    private ProcessedFrame(Long timestamp, List<T> detectionResults) {
        this.processingTime = timestamp;
        this.detectionResults = detectionResults;
    }

    public static<T> ProcessedFrame<T> empty() {
        @SuppressWarnings("unchecked")
        ProcessedFrame<T> t = (ProcessedFrame<T>) EMPTY;
        return t;
    }

    public static<T> ProcessedFrame<T> frame(Long timestamp, List<T> detectionResults) {
        return new ProcessedFrame(timestamp, detectionResults);
    }

}
