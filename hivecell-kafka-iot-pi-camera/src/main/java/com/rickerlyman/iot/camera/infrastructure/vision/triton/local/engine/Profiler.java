//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine;
//
//import org.bytedeco.tensorrt.nvinfer.IProfiler;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//public class Profiler extends IProfiler {
//    private static int TIMING_ITERATIONS = 1000;
//    private static Logger logger = LoggerFactory.getLogger(Profiler.class);
//    private LinkedHashMap<String, Float> mProfile = new LinkedHashMap<String, Float>();
//
//    @Override
//    public void reportLayerTime(String layerName, float ms) {
//        Float time = mProfile.get(layerName);
//        mProfile.put(layerName, (time != null ? time : 0) + ms);
//    }
//
//    public void printLayerTimes() {
//        float totalTime = 0;
//        for (Map.Entry<String, Float> e : mProfile.entrySet()) {
//            logger.info(String.format("%-40.40s %4.3fms\n", e.getKey(), e.getValue() / TIMING_ITERATIONS));
//            totalTime += e.getValue();
//        }
//        logger.info(String.format("Time over all layers: %4.3f\n", totalTime / TIMING_ITERATIONS));
//    }
//
//}