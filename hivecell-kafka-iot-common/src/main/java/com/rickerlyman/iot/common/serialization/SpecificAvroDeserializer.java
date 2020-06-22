package com.rickerlyman.iot.common.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class SpecificAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final Schema schema;

    public SpecificAvroDeserializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        Objects.requireNonNull(topic, "Topic can not be null");
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            return readAvroRecord(stream, schema);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private T readAvroRecord(InputStream stream, Schema schema) throws IOException {
        DatumReader<T> datumReader = new SpecificDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(stream, null);
        return datumReader.read(null, decoder);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

}
