package com.rickerlyman.iot.sense.core.handler;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IgnoreRecordTooLargeHandler implements ProductionExceptionHandler {
    Logger logger = LoggerFactory.getLogger(IgnoreRecordTooLargeHandler.class);

    @Override
    public ProductionExceptionHandlerResponse handle(ProducerRecord<byte[], byte[]> record, Exception exception) {
        if (exception instanceof RecordTooLargeException) {
            logger.warn("RecordTooLarge");
            return ProductionExceptionHandlerResponse.CONTINUE;
        } else {
            return ProductionExceptionHandlerResponse.FAIL;
        }
    }

    @Override
    public void configure(Map<String, ?> configs) {}
}
