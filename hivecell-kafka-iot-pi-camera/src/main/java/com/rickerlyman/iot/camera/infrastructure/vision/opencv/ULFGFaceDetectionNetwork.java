package com.rickerlyman.iot.camera.infrastructure.vision.opencv;

import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
import com.rickerlyman.iot.camera.core.vision.Detection;
import com.rickerlyman.iot.camera.infrastructure.util.MathUtils;
import com.rickerlyman.iot.camera.infrastructure.vision.Labels;
import com.rickerlyman.iot.common.util.TimeUtils;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.Net;
import org.bytedeco.opencv.opencv_text.FloatVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.*;

public class ULFGFaceDetectionNetwork implements Detection<ObjectDetectionResult> {
    private static float IOU_THRESHOLD = 0.3f;
    private static int TOP_K = -1;
    private static float IMAGE_STD = 128.0f;
    private static float CENTER_VARIANCE = 0.1f;
    private static float SIZE_VARIANCE = 0.2f;
    private static float[][] MIN_BOXES = {{10.0f, 16.0f, 24.0f}, {32.0f, 48.0f}, {64.0f, 96.0f}, {128.0f, 192.0f, 256.0f}};
    private static float[] STRIDES = {8.0f, 16.0f, 32.0f, 64.0f};
    private static final Scalar MEAN = Scalar.all(127);
    private static final double SCALE_FACTOR = 1 / IMAGE_STD;
    private static final Size SIZE = new Size(640, 480);
    private static final float CONFIDENCE_THRESHOLD = 0.7f;

    private List<float[]> priors;
    private final ThreadLocal<Net> netThreadLocal;

    public ULFGFaceDetectionNetwork(String modelWeights) {
        this.netThreadLocal = getNet(modelWeights);
        this.priors = new ArrayList<>();
        defineImageSize(SIZE);
    }

    private static ThreadLocal<Net> getNet(String modelWeights) {
        Objects.requireNonNull(modelWeights, "modelWeights");
        return ThreadLocal.withInitial(() -> readNetFromONNX(modelWeights));
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
        // extract boxes and scores
        Mat boxesOut = outs.get(0);
        Mat confidencesOut = outs.get(1);
        // boxes
        Mat boxes = boxesOut.reshape(0, boxesOut.size(1));
        // class confidences (BACKGROUND, face)
        Mat confidences = confidencesOut.reshape(0, confidencesOut.size(1));
        return predict(frame.size().width(), frame.size().height(), confidences, boxes);
    }

    private List<ObjectDetectionResult> predict(int frameWidth, int frameHeight, Mat confidences, Mat boxes) {
        FloatVector relevantConfidences = new FloatVector();
        RectVector relevantBoxes = new RectVector();

        // extract only relevant prob
        for (int i = 0; i < boxes.rows(); i++) {
            FloatPointer confidencesPtr = new FloatPointer(confidences.row(i).data());
            float probability = confidencesPtr.get(1); // read second column (face)

            if (probability < CONFIDENCE_THRESHOLD) continue;

            // add probability
            relevantConfidences.push_back(probability);

            // add box data and convert locations to positions
            float[] prior = priors.get(i);
            FloatPointer boxesPtr = new FloatPointer(boxes.row(i).data());
            float centerX = ((boxesPtr.get(0) * CENTER_VARIANCE * prior[2] + prior[0]) * frameWidth);
            float centerY = ((boxesPtr.get(1) * CENTER_VARIANCE * prior[3] + prior[1]) * frameHeight);
            float width = (float) ((Math.exp(boxesPtr.get(2) * SIZE_VARIANCE) * prior[2]) * frameHeight);
            float height = (float) ((Math.exp(boxesPtr.get(3) * SIZE_VARIANCE) * prior[3]) * frameHeight);

            int left = Math.round(centerX - width / 2.0f);
            int top = Math.round(centerY - height / 2.0f);

            relevantBoxes.push_back(new Rect(left, top, Math.round(width), Math.round(height)));
        }

        // run nms
        IntPointer indices = new IntPointer(confidences.size());
        FloatPointer confidencesPointer = new FloatPointer(relevantConfidences.size());
        confidencesPointer.put(relevantConfidences.get());

        NMSBoxes(relevantBoxes, confidencesPointer, CONFIDENCE_THRESHOLD, IOU_THRESHOLD, indices, 1.0f, TOP_K);

        // extract nms result
        List<ObjectDetectionResult> detections = new ArrayList<>();
        for (int i = 0; i < indices.limit(); ++i) {
            int idx = indices.get(i);
            Rect box = relevantBoxes.get(idx);

            detections.add(ObjectDetectionResult.detectionResult(1, "face", relevantConfidences.get(idx),
                box.x(), box.y(), box.width(), box.height()));
        }

        return detections;
    }

    private void defineImageSize(Size imageSize) {
        // shrinkageList is always the same
        int[][] featureMapList = new int[2][STRIDES.length];

        // create feature maps
        for (int d = 0; d < featureMapList.length; d++) {
            int size = imageSize.get(d);

            for (int i = 0; i < STRIDES.length; i++) {
                featureMapList[d][i] = (int) (Math.ceil(size / STRIDES[i]));
            }
        }

        generatePriors(featureMapList, imageSize);
    }

    private void generatePriors(int[][] featureMapList, Size imageSize) {
        priors.clear();

        for (int index = 0; index < featureMapList[0].length; index++) {
            float scaleW = imageSize.get(0) / STRIDES[index];
            float scaleH = imageSize.get(1) / STRIDES[index];

            for (int j = 0; j < featureMapList[1][index]; j++) {
                for (int i = 0; i < featureMapList[0][index]; i++) {
                    float xCenter = (i + 0.5f) / scaleW;
                    float yCenter = (j + 0.5f) / scaleH;

                    for (float minBox : MIN_BOXES[index]) {
                        float w = minBox / imageSize.get(0);
                        float h = minBox / imageSize.get(1);

                        priors.add(new float[]{
                                MathUtils.clamp(xCenter, 0.0f, 1.0f),
                                MathUtils.clamp(yCenter, 0.0f, 1.0f),
                                MathUtils.clamp(w, 0.0f, 1.0f),
                                MathUtils.clamp(h, 0.0f, 1.0f)
                            }
                        );
                    }
                }
            }
        }
    }

}
