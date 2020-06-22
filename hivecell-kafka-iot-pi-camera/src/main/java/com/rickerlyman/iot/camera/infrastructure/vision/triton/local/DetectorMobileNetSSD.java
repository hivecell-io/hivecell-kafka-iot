//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local;
//
//import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
//import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
//import com.rickerlyman.iot.camera.core.model.RawFrame;
//import com.rickerlyman.iot.camera.core.vision.Detection;
//import com.rickerlyman.iot.camera.infrastructure.vision.Labels;
//import com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine.TensorRTEngine;
//import org.bytedeco.javacpp.BytePointer;
//import org.bytedeco.javacpp.FloatPointer;
//import org.bytedeco.javacpp.Loader;
//import org.bytedeco.javacpp.Pointer;
//import org.bytedeco.opencv.global.opencv_imgcodecs;
//import org.bytedeco.opencv.opencv_core.Mat;
//import org.bytedeco.opencv.opencv_core.MatVector;
//import org.bytedeco.opencv.opencv_core.Scalar;
//import org.bytedeco.opencv.opencv_core.Size;
//import org.bytedeco.tensorrt.global.nvinfer;
//
//import java.nio.FloatBuffer;
//import java.util.*;
//
//import static org.bytedeco.opencv.global.opencv_core.CV_32F;
//import static org.bytedeco.opencv.global.opencv_imgproc.resize;
//
//
//public class DetectorMobileNetSSD implements Detection<ObjectDetectionResult> {
//
//    private static String CONV_0 = "conv0";
//
//    private static final float CONFIDENCE_THRESHOLD = 0.5f;
//
//    private static final Size SIZE = new Size(300, 300);
//    private static final float SCALE_FACTOR = 0.007843f;
//    private static final Scalar MEAN = Scalar.all(127.5);
//
//    private final ThreadLocal<TensorRTEngine> engineThreadLocal;
//    private final ThreadLocal<Map<Integer, String>> labels;
//
//    public DetectorMobileNetSSD(String protoTxt, String model, String labelMapping) {
//        Loader.load(nvinfer.class);
//        this.engineThreadLocal = getEngine(protoTxt, model);
//        this.labels = getLabels(labelMapping);
//    }
//
//    private static ThreadLocal<Map<Integer, String>> getLabels(String nameMappingPath) {
//        return ThreadLocal.withInitial(() -> Labels.getLabels(nameMappingPath));
//    }
//
//    private static ThreadLocal<TensorRTEngine> getEngine(String protoTxt, String model) {
//        Objects.requireNonNull(protoTxt, "protoTxt");
//        Objects.requireNonNull(model, "model");
//        return ThreadLocal.withInitial(() -> new TensorRTEngine(protoTxt, model));
//    }
//
//    @Override
//    public ProcessedFrame<ObjectDetectionResult> detect(RawFrame rawFrame) {
//        TensorRTEngine engine = engineThreadLocal.get();
//        Mat frame = opencv_imgcodecs.imdecode(new Mat(rawFrame.getFrame()), opencv_imgcodecs.IMREAD_UNCHANGED);
//        Map<String, Pointer> image = loadImage(frame, SIZE, MEAN, SCALE_FACTOR);
//        Map<String, Pointer> infer = engine.infer(image, 1);
//        return ProcessedFrame.frame(rawFrame.getTimestamp(), processOutput(frame, infer));
//    }
//
//    private Map<String, Pointer> loadImage(Mat image, Size size, Scalar mean, float scaleFactor) {
//        Mat dst = image.clone();
//        resize(image, image, size);
//
//        int offsetG = size.width() * size.height();
//        int offsetR = size.width() * size.height() * 2;
//
//        FloatBuffer floatBuffer = FloatBuffer.allocate(image.createBuffer().capacity());
//        float meanValue = (float) mean.get();
//
//        for (int i = 0; i < size.height(); ++i) {
//            BytePointer line = dst.ptr(i);
//
//            int lineOffset = i * size.width();
//            for (int j = 0; j < size.width(); ++j) {
//                // b
//                floatBuffer.put(lineOffset + j, (line.getFloat(j * 3) - meanValue) * scaleFactor);
//                // g
//                floatBuffer.put(offsetG + lineOffset + j, (line.getFloat(j * 3 + 1) - meanValue) * scaleFactor);
//                // r
//                floatBuffer.put(offsetR + lineOffset + j, (line.getFloat(j * 3 + 2) - meanValue) * scaleFactor);
//            }
//        }
//        FloatPointer pointer = new FloatPointer(floatBuffer);
//        Map<String, Pointer> map = new HashMap<>();
//        map.put(CONV_0, pointer);
//        return map;
//    }
//
//    protected List<ObjectDetectionResult> processOutput(Mat image, Map<String, Pointer> output) {
//        Mat[] mats = output.values().stream().map(Mat::new).toArray(Mat[]::new);
//        MatVector outs = new MatVector(mats);
//        return postProcess(image, outs);
//    }
//
//    public List<ObjectDetectionResult> postProcess(Mat frame, MatVector outs) {
//        Mat detection = outs.get(0);
//        Mat detectionMat = new Mat(detection.size(2), detection.size(3), CV_32F, detection.ptr());
//
//        // extract detections
//        Map<Integer, String> labelMapping = labels.get();
//        List<ObjectDetectionResult> detections = new ArrayList<>();
//        for (int i = 0; i < detectionMat.rows(); i++) {
//            FloatPointer dataPtr = new FloatPointer(detectionMat.row(i).data());
//
//            int label = Math.round(dataPtr.get(1)) - 1;
//            float confidence = dataPtr.get(2);
//            if (confidence < CONFIDENCE_THRESHOLD) continue;
//
//            float xLeftBottom = dataPtr.get(3) * frame.cols();
//            float yLeftBottom = dataPtr.get(4) * frame.rows();
//            float xRightTop = dataPtr.get(5) * frame.cols();
//            float yRightTop = dataPtr.get(6) * frame.rows();
//
//            int x = Math.round(xLeftBottom);
//            int y = Math.round(yLeftBottom);
//            int width = Math.round(xRightTop - xLeftBottom);
//            int height = Math.round(yRightTop - yLeftBottom);
//
//            detections.add(ObjectDetectionResult.detectionResult(label, labelMapping.get(label),
//                x, y, width, height, confidence));
//        }
//
//        return detections;
//    }
//
//}
