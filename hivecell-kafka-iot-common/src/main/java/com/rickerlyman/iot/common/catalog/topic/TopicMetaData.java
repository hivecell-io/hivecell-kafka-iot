package com.rickerlyman.iot.common.catalog.topic;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.TimestampExtractor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TopicMetaData<K, V> {
    private final String name;
    private final Serde<K> keySerde;
    private final Serde<V> valueSerde;
    private final TimestampExtractor timestampExtractor;
    private final Topology.AutoOffsetReset resetPolicy;

    public static <K, V> TopicMetaData<K, V> with(String name) {
        return with(name, null, null);
    }

    public static <K, V> TopicMetaData<K, V> with(String name, Serde<K> keySerde, Serde<V> valueSerde) {
        return with(name, keySerde, valueSerde, (TimestampExtractor) null);
    }

    public static <K, V> TopicMetaData<K, V> with(String name, Serde<K> keySerde, Serde<V> valueSerde, TimestampExtractor timestampExtractor) {
        return with(name, keySerde, valueSerde, timestampExtractor, (Topology.AutoOffsetReset) null);
    }

    public static <K, V> TopicMetaData<K, V> with(String name, Serde<K> keySerde, Serde<V> valueSerde, String resetPolicy) {
        return with(name, keySerde, valueSerde, null, resetPolicy);
    }

    public static <K, V> TopicMetaData<K, V> with(String name, Serde<K> keySerde, Serde<V> valueSerde, TimestampExtractor timestampExtractor, String resetPolicy) {
        return with(name, keySerde, valueSerde, timestampExtractor, Topology.AutoOffsetReset.valueOf(resetPolicy.toUpperCase()));
    }

    public static <K, V> TopicMetaData<K, V> with(String name, Serde<K> keySerde, Serde<V> valueSerde, TimestampExtractor timestampExtractor, Topology.AutoOffsetReset resetPolicy) {
        return new TopicMetaData<>(name, keySerde, valueSerde, timestampExtractor, resetPolicy);
    }


}
