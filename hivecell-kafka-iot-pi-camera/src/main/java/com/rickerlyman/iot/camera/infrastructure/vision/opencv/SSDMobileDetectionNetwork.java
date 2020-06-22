package com.rickerlyman.iot.camera.infrastructure.vision.opencv;

import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
import com.rickerlyman.iot.camera.core.vision.Detection;
import com.rickerlyman.iot.camera.infrastructure.vision.Labels;
import com.rickerlyman.iot.common.util.TimeUtils;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.Net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromTensorflow;

public class SSDMobileDetectionNetwork implements Detection<ObjectDetectionResult> {
    private static final double SCALE_FACTOR = 1;
    private static final Size SIZE = new Size(300, 300);
    private static final Scalar MEAN = Scalar.all(0);
    private static final float CONFIDENCE_THRESHOLD = 0.5f;

    private final ThreadLocal<Net> netThreadLocal;
    private final ThreadLocal<Map<Integer, String>> labels;

    public SSDMobileDetectionNetwork(String modelConfiguration, String modelWeights, String labelMapping) {
        this.netThreadLocal = getNet(modelConfiguration, modelWeights);
        this.labels = getLabels(labelMapping);
    }

    private static ThreadLocal<Net> getNet(String modelConfiguration, String modelWeights) {
        Objects.requireNonNull(modelWeights, "modelWeights");
        Objects.requireNonNull(modelConfiguration, "modelConfiguration");
        return ThreadLocal.withInitial(() -> readNetFromTensorflow(modelWeights, modelConfiguration));
    }

    private static ThreadLocal<Map<Integer, String>> getLabels(String nameMappingPath) {
        return ThreadLocal.withInitial(() -> Labels.getLabels(nameMappingPath));
    }

    @Override
    public ProcessedFrame<ObjectDetectionResult> detect(byte[] rawFrame) {
        Net net = netThreadLocal.get();
        Mat frame = opencv_imgcodecs.imdecode(new Mat(rawFrame), opencv_imgcodecs.IMREAD_UNCHANGED);
        Mat inputBlob = blobFromImage(frame, SCALE_FACTOR, SIZE, MEAN, false, false, CV_32F);
        // set input
        net.setInput(inputBlob);
        // create output layers
        StringVector outNames = net.getUnconnectedOutLayersNames();
        MatVector outs = new MatVector(outNames.size());
        // run detection
        net.forward(outs, outNames);
        return ProcessedFrame.frame(TimeUtils.now(), postProcess(frame, outs));
    }

    public List<ObjectDetectionResult> postProcess(Mat frame, MatVector outs) {
        Mat detection = outs.get(0);
        Mat detectionMat = new Mat(detection.size(2), detection.size(3), CV_32F, detection.ptr());

        // extract detections
        Map<Integer, String> labelMapping = labels.get();
        List<ObjectDetectionResult> detections = new ArrayList<>();
        for (int i = 0; i < detectionMat.rows(); i++) {
            FloatPointer dataPtr = new FloatPointer(detectionMat.row(i).data());

            float confidence = dataPtr.get(2);
            if (confidence < CONFIDENCE_THRESHOLD) continue;

            int label = Math.round(dataPtr.get(1)) - 1;
            float xLeftBottom = dataPtr.get(3) * frame.cols();
            float yLeftBottom = dataPtr.get(4) * frame.rows();
            float xRightTop = dataPtr.get(5) * frame.cols();
            float yRightTop = dataPtr.get(6) * frame.rows();

            int x = Math.round(xLeftBottom);
            int y = Math.round(yLeftBottom);
            int width = Math.round(xRightTop - xLeftBottom);
            int height = Math.round(yRightTop - yLeftBottom);

            detections.add(ObjectDetectionResult.detectionResult(label, labelMapping.get(label),
                x, y, width, height, confidence));
        }

        return detections;
    }

}
