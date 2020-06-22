//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local;
//
//import com.rickerlyman.iot.camera.core.model.ProcessedFrame;
//import com.rickerlyman.iot.camera.core.model.RawFrame;
//import com.rickerlyman.iot.camera.core.vision.Detection;
//import org.bytedeco.cuda.cudart.float3;
//import org.bytedeco.javacpp.FloatPointer;
//import org.bytedeco.javacpp.Loader;
//import org.bytedeco.javacpp.PointerPointer;
//import org.bytedeco.opencv.global.opencv_imgcodecs;
//import org.bytedeco.opencv.opencv_core.Mat;
//import org.bytedeco.opencv.opencv_core.GpuMat;
//
//import org.bytedeco.opencv.opencv_core.MatVector;
//import org.bytedeco.opencv.opencv_core.Size;
//import org.bytedeco.opencv.opencv_core.StringVector;
//import org.bytedeco.opencv.opencv_dnn.Net;
//import org.bytedeco.tensorrt.nvinfer.DimsCHW;
//
//import static org.bytedeco.cuda.global.cudart.cudaMalloc;
//import static org.bytedeco.javacpp.Pointer.malloc;
//import static org.bytedeco.opencv.global.opencv_core.CV_32F;
//import static org.bytedeco.opencv.global.opencv_cudawarping.resize;
//import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
//
//public class MobileNetSSDTensorRT implements Detection<String> {
//
//    private static final int BATCH_SIZE = 1;
//    private static final Size SIZE = new Size(300, 300);
//    private static final int CONFIDENCE_THRESHOLD = -1;
//
//    private static final String INPUT_BLOB_NAME = "data";
//    private static final String OUTPUT_BLOB_NAME = "prob";
//
//
//    //TODO: We can run this in docker container with CUDA instead of installing CUDA to Hivecell
//    @Override
//    public ProcessedFrame<String> detect(RawFrame rawFrame) {
////        Net net = netThreadLocal.get();
//
//        TensorNet net = new TensorNet("", "", new String[]{OUTPUT_BLOB_NAME}, BATCH_SIZE);
//
//        Mat frame = opencv_imgcodecs.imdecode(new Mat(rawFrame.getFrame()), opencv_imgcodecs.IMREAD_UNCHANGED);
//        Mat srcImg = frame.clone();
//        resize(frame, frame, SIZE);
//        int size = SIZE.width() * SIZE.height() * Loader.sizeof(float3.class);
//
//
//        malloc(size);
//
//
//        return null;
//    }
//}
