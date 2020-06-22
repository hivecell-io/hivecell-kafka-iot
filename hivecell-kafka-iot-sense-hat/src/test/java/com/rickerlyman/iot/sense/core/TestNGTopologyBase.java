package com.rickerlyman.iot.sense.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickerlyman.iot.common.util.Resources;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.KeyValueTimestamp;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.testng.Assert;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class TestNGTopologyBase {

    protected <K, V> List<KeyValue<K, V>> createTopicInput(String resource) {
        List<KeyValueContainer<K, V>> data = Resources.asObject(resource, new TypeReference<List<KeyValueContainer<K, V>>>() {
        });
        return data.stream()
            .map(entry -> KeyValue.pair(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    protected <K, V> List<KeyValueTimestamp<K, V>> createTopicOutput(String resource) {
        List<KeyValueTimestampContainer<K, V>> data =
            Resources.asObject(resource, new TypeReference<List<KeyValueTimestampContainer<K, V>>>() {
            });

        return data.stream()
            .map(kvt -> new KeyValueTimestamp<>(kvt.getKey(), kvt.getValue(), kvt.getTimestamp()))
            .collect(Collectors.toList());
    }

    protected <K, V> List<TestRecord<K, V>> drainProducerRecords(final TopologyTestDriver driver,
                                                                 final String topic,
                                                                 final Deserializer<K> keyDeserializer,
                                                                 final Deserializer<V> valueDeserializer) {
        return driver.createOutputTopic(topic, keyDeserializer, valueDeserializer).readRecordsToList();
    }

    protected <K, V> void verify(final List<TestRecord<K, V>> actualResults,
                                 final List<KeyValueTimestamp<K, V>> expectedResults) {
        if (actualResults.size() != expectedResults.size()) {
            throw new AssertionError(String.format("\nExpected: %s,\nbut was: %s", printExpected(expectedResults), printActual(actualResults)));
        }
        final Iterator<KeyValueTimestamp<K, V>> expectedIterator = expectedResults.iterator();
        for (final TestRecord<K, V> result : actualResults) {
            final KeyValueTimestamp<K, V> expected = expectedIterator.next();
            try {
                Assert.assertEquals(result, TestRecord(expected.key(), expected.value(), null, expected.timestamp()));
            } catch (final AssertionError e) {
                throw new AssertionError(String.format("\nExpected: %s,\nbut was: %s", printExpected(expectedResults), printActual(actualResults)), e);
            }
        }
    }

    protected <K, V> String printExpected(final List<KeyValueTimestamp<K, V>> expectedResults) {
        final StringBuilder resultStr = new StringBuilder();
        resultStr.append("[\n");
        for (final KeyValueTimestamp<?, ?> record : expectedResults) {
            resultStr.append("  ").append(record).append("\n");
        }
        resultStr.append("]");
        return resultStr.toString();
    }

    protected <K, V> String printActual(final List<TestRecord<K, V>> result) {
        final StringBuilder resultStr = new StringBuilder();
        resultStr.append("[\n");
        for (final TestRecord<?, ?> record : result) {
            resultStr.append("  ").append(record).append("\n");
        }
        resultStr.append("]");
        return resultStr.toString();
    }

    protected <K, V> TestRecord<K, V> TestRecord(K k, V v, Headers headers, Long timestamp) {
        return new TestRecord<K, V>(k, v, headers, timestamp);
    }

    protected <K, V> TestRecord<K, V> TestRecord(K k, V v, Long timestamp) {
        return TestRecord(k, v, null, timestamp);
    }

    public static Properties mkProperties(final Map<Object, Object> properties) {
        final Properties result = new Properties();
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class KeyValueTimestampContainer<K, V> {
        private K key;
        private V value;
        private long timestamp;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    private static class KeyValueContainer<K, V> {
        private K key;
        private V value;
    }

}
