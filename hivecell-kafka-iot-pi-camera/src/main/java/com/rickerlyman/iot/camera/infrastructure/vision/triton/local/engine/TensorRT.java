//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine;
//
//import org.bytedeco.javacpp.Pointer;
//import org.bytedeco.tensorrt.nvinfer.ICudaEngine;
//import org.bytedeco.tensorrt.nvinfer.ILogger;
//
//import static org.bytedeco.tensorrt.global.nvinfer_plugin.initLibNvInferPlugins;
//
//public class TensorRT {
//    static ILogger logger;
//    static Pointer nullPointer;
//
//    static {
//        logger = new CudaEngineLogger();
//        nullPointer = new Pointer();
//        nullPointer.setNull();
//        initLibNvInferPlugins(logger, "");
//    }
//
//    public static void engineInfo(ICudaEngine iCudaEngine) {
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append("Name : ").append(iCudaEngine.getName()).append("\n");
//        stringBuilder.append("Number of Bindings    : ").append(iCudaEngine.getNbBindings()).append("\n");
//        for (int i = 0; i < iCudaEngine.getNbBindings(); i++) {
//            stringBuilder.append("binding ").append("[").append(i).append("] :").append("\n");
//            stringBuilder.append("   ").append("binding name: ").append(iCudaEngine.getBindingName(i)).append("\n");
//            stringBuilder.append("   ").append("format      : ").append(iCudaEngine.getBindingFormat(i)).append("\n");
//            if (iCudaEngine.bindingIsInput(i))
//                stringBuilder.append("   ").append("is Input    :").append(true).append("\n");
//        }
//        stringBuilder.append("Max batch size    : ").append(iCudaEngine.getMaxBatchSize()).append("\n");
//        stringBuilder.append("Device mem size   : ").append(iCudaEngine.getDeviceMemorySize()).append("\n");
//        stringBuilder.append("Max Work Space    : ").append(iCudaEngine.getWorkspaceSize()).append("\n");
//        stringBuilder.append("Engine capability : ").append(iCudaEngine.getEngineCapability()).append("\n");
//        stringBuilder.append("Engine capability intern: ").append(iCudaEngine.getEngineCapability().intern()).append("\n");
//        System.out.println(stringBuilder.toString());
//    }
//
//    private static class CudaEngineLogger extends ILogger {
//        @Override
//        public void log(Severity severity, String msg) {
//            severity = severity.intern();
//            switch (severity) {
//                case kINTERNAL_ERROR:  System.err.print("INTERNAL_ERROR: "); break;
//                case kERROR: System.err.print("ERROR: "); break;
//                case kWARNING: System.err.print("WARNING: "); break;
//                case kINFO: System.err.print("INFO: "); break;
//                case kVERBOSE: System.err.print("kVERBOSE: "); break;
//                default: System.err.print("UNKNOWN: "); break;
//            }
//            System.err.println(msg);
//        }
//    }
//}
//
