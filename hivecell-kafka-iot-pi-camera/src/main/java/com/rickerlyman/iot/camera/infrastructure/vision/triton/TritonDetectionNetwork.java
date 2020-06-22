package com.rickerlyman.iot.camera.infrastructure.vision.triton;

import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
import com.rickerlyman.iot.camera.core.vision.Detection;
import com.rickerlyman.iot.camera.infrastructure.vision.Labels;
import com.rickerlyman.iot.common.util.TimeUtils;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class TritonDetectionNetwork implements Detection<ObjectDetectionResult> {

    private final ThreadLocal<TritonClient> tritonClient;
    protected final ThreadLocal<Map<Integer, String>> labels;

    public TritonDetectionNetwork(Supplier<TritonClient> tritonClientSupplier, String labelMapping) {
        this.tritonClient = getTritonClient(tritonClientSupplier);
        this.labels = getLabels(labelMapping);
    }

    private static ThreadLocal<TritonClient> getTritonClient(Supplier<TritonClient> supplier) {
        return ThreadLocal.withInitial(supplier);
    }

    private static ThreadLocal<Map<Integer, String>> getLabels(String nameMappingPath) {
        return ThreadLocal.withInitial(() -> Labels.getLabels(nameMappingPath));
    }

    @Override
    public ProcessedFrame<ObjectDetectionResult> detect(byte[] rawFrame) {
        ProcessedFrame<ObjectDetectionResult> processedFrame = null;
        try (PointerScope scope = new PointerScope()) {
            //TODO: hypothesis, detection could be optimized if replace opencv_imgcodecs.imdecode with smth else
            Mat frame = opencv_imgcodecs.imdecode(new Mat(rawFrame), opencv_imgcodecs.IMREAD_UNCHANGED);
            byte[] img = preProcess(frame);
            byte[] response = tritonClient.get().infer(img);
            byte[] rawTensors = truncateResponse(response);
            processedFrame = ProcessedFrame.frame(TimeUtils.now(), postProcess(rawTensors, frame));
        }
        return processedFrame;
    }

    protected abstract byte[] preProcess(Mat frame);

    protected abstract byte[] truncateResponse(byte[] response);

    protected abstract List<ObjectDetectionResult> postProcess(byte[] rawTensors, Mat frame);

}
