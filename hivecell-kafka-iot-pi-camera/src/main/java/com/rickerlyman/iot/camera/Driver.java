package com.rickerlyman.iot.camera;

import com.rickerlyman.iot.camera.infrastructure.Factory;
import com.rickerlyman.iot.camera.infrastructure.vision.Detections;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Driver {
    private static Logger logger = LoggerFactory.getLogger(Driver.class);
    private static final Factory factory = Factory.getInstance();

    public static void main(String[] args) {
        switch (Detections.valueOf(args[0].toUpperCase())) {
            case YOLO_CV:
                start(factory.createYoloDetectionStream(args));
                break;
            case SSDM_CV:
                start(factory.createSsdmDetectionStream(args));
                break;
            case ULFG_CV:
                start(factory.createUlfgDetectionStream(args));
                break;
            case SSD_V2_TRITON:
                start(factory.createSsdV2TritonDetectionStream(args));
                break;
            default:
                throw new IllegalArgumentException(String.format("Algorithm %s is not supported", args[0]));
        }
    }

    private static void start(KafkaStreams stream) {
        try {
            logger.info("Start processing stream");
            stream.start();
        } catch (Exception e) {
            try {
                logger.info("Unexpected error occurred, stopping streams");
                if (stream != null) {
                    // Additional cleanup before closing the stream.
                    stream.cleanUp();
                }
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw e;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(stream::close));
    }
}
