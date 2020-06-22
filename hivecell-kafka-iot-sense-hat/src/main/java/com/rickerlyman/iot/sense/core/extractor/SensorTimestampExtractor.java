package com.rickerlyman.iot.sense.core.extractor;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.rickerlyman.iot.common.util.TimeUtils.PT1S;

public class SensorTimestampExtractor implements TimestampExtractor {
    Logger logger = LoggerFactory.getLogger(SensorTimestampExtractor.class);

    @Override
    public long extract(ConsumerRecord<Object, Object> consumerRecord, long partitionTime) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("Invalid input record: {}", consumerRecord);
            return -1;
        }

        String record = (String) consumerRecord.value();
        String[] columns = record.split(",");

        if (columns.length != 5){
            logger.error("Invalid input record: {}", consumerRecord);
            return 0;
        }

        return Long.parseLong(columns[1]) * PT1S;
    }
}
