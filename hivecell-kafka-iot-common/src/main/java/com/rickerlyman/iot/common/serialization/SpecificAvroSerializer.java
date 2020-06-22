package com.rickerlyman.iot.common.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class SpecificAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    private final Schema schema;

    public SpecificAvroSerializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public byte[] serialize(String topic, T data) {
        Objects.requireNonNull(topic, "Topic can not be null");
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            writeSerializedAvro(stream, data);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize data", e);
        }
    }

    private void writeSerializedAvro(ByteArrayOutputStream stream, T data) throws IOException {
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(stream, null);
        DatumWriter<T> datumWriter = new SpecificDatumWriter<>(schema);
        datumWriter.write(data, encoder);
        encoder.flush();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

}
