package com.rickerlyman.iot.common.serialization;

import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListDeserializer<T> implements Deserializer<List<T>> {

    private final Deserializer<T> valueDeserializer;

    /**
     * Constructor used by Kafka Streams.
     * @param valueDeserializer
     */
    public ListDeserializer(final Deserializer<T> valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // do nothing
    }

    @Override
    public List<T> deserialize(String s, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        final List<T> data = new ArrayList<>();
        final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            final int records = dataInputStream.readInt();
            for (int i = 0; i < records; i++) {
                final byte[] valueBytes = new byte[dataInputStream.readInt()];
                dataInputStream.read(valueBytes);
                data.add(valueDeserializer.deserialize(s, valueBytes));
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize List", e);
        }
        return data;
    }

    @Override
    public void close() {

    }
}
