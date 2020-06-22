package com.rickerlyman.iot.camera.infrastructure.vision.opencv;

import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
import com.rickerlyman.iot.camera.core.vision.Detection;
import com.rickerlyman.iot.camera.infrastructure.vision.Labels;
import com.rickerlyman.iot.common.util.TimeUtils;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_text.FloatVector;
import org.bytedeco.opencv.opencv_text.IntVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.minMaxLoc;
import static org.bytedeco.opencv.global.opencv_dnn.*;

public class YOLODetectionNetwork implements Detection<ObjectDetectionResult> {
    private static final double SCALE_FACTOR = 1 / 255.0;
    private static final float CONF_THRESHOLD = 0.5f;
    private static final float NMS_THRESHOLD = 0.4f;
    private static final Size SIZE = new Size(608, 608);
    private static final Scalar MEAN = new Scalar(0.0);
    private static final boolean SKIP_NMS = false;

    private final ThreadLocal<Net> netThreadLocal;
    private final ThreadLocal<Map<Integer, String>> labels;

    public YOLODetectionNetwork(String modelConfiguration, String modelWeights, String labelMapping) {
        this.netThreadLocal = getNet(modelConfiguration, modelWeights);
        this.labels = getLabels(labelMapping);
    }

    @Override
    public ProcessedFrame<ObjectDetectionResult> detect(byte[] rawFrame) {
        Net net = netThreadLocal.get();
        Mat frame = opencv_imgcodecs.imdecode(new Mat(rawFrame), opencv_imgcodecs.IMREAD_UNCHANGED);
        // convert image into batch of images
        Mat blob = blobFromImage(frame, SCALE_FACTOR, SIZE, MEAN, true, false, CV_32F);
        // set input
        net.setInput(blob);
        // create output layers
        StringVector outNames = net.getUnconnectedOutLayersNames();
        MatVector outs = new MatVector(outNames.size());
        // run detection
        net.forward(outs, outNames);

        return ProcessedFrame.frame(TimeUtils.now(), postProcess(frame, outs));
    }

    /**
     * Remove the bounding boxes with low confidence using non-maxima suppression
     *
     * @param frame Input frame
     * @param outs  Network outputs
     * @return List of objects
     */
    private List<ObjectDetectionResult> postProcess(Mat frame, MatVector outs) {
        IntVector classIds = new IntVector();
        FloatVector confidences = new FloatVector();
        RectVector boxes = new RectVector();

        for (int i = 0; i < outs.size(); ++i) {
            // Scan through all the bounding boxes output from the network and keep only the
            // ones with high confidence scores. Assign the box's class label as the class
            // with the highest score for the box.
            Mat result = outs.get(i);

            for (int j = 0; j < result.rows(); j++) {
                FloatPointer data = new FloatPointer(result.row(j).data());
                Mat scores = result.row(j).colRange(5, result.cols());

                Point classIdPoint = new Point(1);
                DoublePointer confidence = new DoublePointer(1);

                // Get the value and location of the maximum score
                minMaxLoc(scores, null, confidence, null, classIdPoint, null);
                if (confidence.get() > CONF_THRESHOLD) {
                    // todo: maybe round instead of floor
                    int centerX = (int) (data.get(0) * frame.cols());
                    int centerY = (int) (data.get(1) * frame.rows());
                    int width = (int) (data.get(2) * frame.cols());
                    int height = (int) (data.get(3) * frame.rows());
                    int left = centerX - width / 2;
                    int top = centerY - height / 2;

                    classIds.push_back(classIdPoint.x());
                    confidences.push_back((float) confidence.get());
                    boxes.push_back(new Rect(left, top, width, height));
                }
            }
        }

        Map<Integer, String> labelMapping = labels.get();
        // skip nms
        if (SKIP_NMS) {
            List<ObjectDetectionResult> detections = new ArrayList<>();
            for (int i = 0; i < confidences.size(); ++i) {
                Rect box = boxes.get(i);

                int classId = classIds.get(i);
                detections.add(ObjectDetectionResult.detectionResult(classId, labelMapping.get(classId),
                    box.x(), box.y(), box.width(), box.height(), confidences.get(i)));
            }
            return detections;
        }

        // Perform non maximum suppression to eliminate redundant overlapping boxes with
        // lower confidences
        IntPointer indices = new IntPointer(confidences.size());
        FloatPointer confidencesPointer = new FloatPointer(confidences.size());
        confidencesPointer.put(confidences.get());

        NMSBoxes(boxes, confidencesPointer, CONF_THRESHOLD, NMS_THRESHOLD, indices, 1.f, 0);

        List<ObjectDetectionResult> detections = new ArrayList<>();
        for (int i = 0; i < indices.limit(); ++i) {
            int idx = indices.get(i);
            Rect box = boxes.get(idx);

            int classId = classIds.get(idx);
            detections.add(ObjectDetectionResult.detectionResult(classId, labelMapping.get(classId),
                box.x(), box.y(), box.width(), box.height(), confidences.get(idx)));
        }

        return detections;
    }

    private static ThreadLocal<Net> getNet(String modelConfiguration, String modelWeights) {
        Objects.requireNonNull(modelWeights, "modelWeights");
        Objects.requireNonNull(modelConfiguration, "modelConfiguration");
        return ThreadLocal.withInitial(() -> readNetFromDarknet(modelConfiguration, modelWeights));
    }

    private static ThreadLocal<Map<Integer, String>> getLabels(String nameMappingPath) {
        return ThreadLocal.withInitial(() -> Labels.getLabels(nameMappingPath));
    }

}
