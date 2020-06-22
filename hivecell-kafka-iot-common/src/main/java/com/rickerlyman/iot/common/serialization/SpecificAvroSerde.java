package com.rickerlyman.iot.common.serialization;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class SpecificAvroSerde<T extends SpecificRecordBase> implements Serde<T> {

    private final Serde<T> inner;

    public SpecificAvroSerde(Schema schema) {
        inner = Serdes.serdeFrom(
            new SpecificAvroSerializer<>(schema),
            new SpecificAvroDeserializer<>(schema));
    }

    @Override
    public Serializer<T> serializer() {
        return inner.serializer();
    }

    @Override
    public Deserializer<T> deserializer() {
        return inner.deserializer();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        inner.serializer().configure(configs, isKey);
        inner.deserializer().configure(configs, isKey);
    }

    @Override
    public void close() {
        inner.serializer().close();
        inner.deserializer().close();
    }
}
