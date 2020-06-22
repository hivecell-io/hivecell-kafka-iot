package com.rickerlyman.iot.sense;

import com.rickerlyman.iot.sense.infrastructure.Factory;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Driver {
    private static Logger logger = LoggerFactory.getLogger(Driver.class);
    private static final Factory factory = Factory.getInstance();

    public static void main(String[] args) {
        start(factory.createAggregatorStream());
    }

    private static void start(KafkaStreams stream){
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
