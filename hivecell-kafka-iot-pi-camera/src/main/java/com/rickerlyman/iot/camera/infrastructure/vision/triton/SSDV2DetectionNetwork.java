package com.rickerlyman.iot.camera.infrastructure.vision.triton;

import com.rickerlyman.iot.camera.core.model.ObjectDetectionResult;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class SSDV2DetectionNetwork extends TritonDetectionNetwork {

    private static final Size size = new Size(300, 300);
    private static final float confTh = 0.3f;
    private static final int outputLayout = 7;
    private static final int[] outputShapes = {100, 1, 7};

    public SSDV2DetectionNetwork(Supplier<TritonClient> tritonClientSupplier, String labelMapping) {
        super(tritonClientSupplier, labelMapping);
    }

    @Override
    protected byte[] preProcess(Mat frame) {
        Mat dst = new Mat();
        resize(frame, dst, size);

        dst = transpose(dst);
        byte[] buf = new byte[(int) dst.total() * dst.channels() * 4];
        dst.data().get(buf);

        return buf;
    }

    private Mat transpose(Mat input) {
        Mat dst = input.clone();
        int orgC = dst.channels();
        int orgH = dst.rows();
        int orgW = dst.cols();

        ByteBuffer byteBuffer = dst.createBuffer();
        FloatBuffer floatBuffer = FloatBuffer.allocate(byteBuffer.capacity());

        for (int c = 0; c < orgC; c++) {
            for (int wh = 0; wh < orgW * orgH; wh++) {
                int idxOutput = c * orgW * orgH + wh;
                int idxInput = wh * orgC + c;
                byte b = byteBuffer.get(idxInput);
                float f = (float) (b & 0xFF) * (2.f / 255) - 1;
                floatBuffer.put(idxOutput, f);
            }
        }
        return new Mat(new FloatPointer(floatBuffer));
    }

    @Override
    protected byte[] truncateResponse(byte[] response) {
        return Arrays.copyOf(response, 2800);
    }

    @Override
    protected List<ObjectDetectionResult> postProcess(byte[] rawTensors, Mat frame) {
        Map<Integer, String> labelMapping = labels.get();
        List<ObjectDetectionResult> detections = new ArrayList<>();

        float[] floats = byteToFloat(rawTensors);
        INDArray reshape = Nd4j.create(outputShapes, floats).reshape(outputShapes);
        DataBuffer data = reshape.data();

        for (int prefix = 0; prefix < data.length(); prefix = prefix + outputLayout) {

            float confidence = data.getFloat(prefix + 2);
            if (confidence < confTh) continue;

            float xLeftBottom = data.getFloat(prefix + 3) * frame.cols();
            float yLeftBottom = data.getFloat(prefix + 4) * frame.rows();
            float xRightTop = data.getFloat(prefix + 5) * frame.cols();
            float yRightTop = data.getFloat(prefix + 6) * frame.rows();
            int cls = data.getInt(prefix + 1);

            int x = Math.round(xLeftBottom);
            int y = Math.round(yLeftBottom);
            int width = Math.round(xRightTop - xLeftBottom);
            int height = Math.round(yRightTop - yLeftBottom);

            detections.add(ObjectDetectionResult.detectionResult(cls, labelMapping.get(cls),
                x, y, width, height, confidence));
        }
        return detections;
    }

    private float[] byteToFloat(byte[] byteArray) {
        int times = Float.SIZE / Byte.SIZE;
        float[] doubles = new float[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        return doubles;
    }
}
