package com.rickerlyman.iot.common.serialization;

import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ListSerializer<T> implements Serializer<List<T>> {

    private final Serializer<T> valueSerializer;

    /**
     * Constructor used by Kafka Streams.
     * @param valueSerializer
     */
    public ListSerializer(final Serializer<T> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // do nothing
    }

    @Override
    public byte[] serialize(String topic, List<T> data) {
        final int size = data.size();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(baos);
        final Iterator<T> iterator = data.iterator();
        try {
            out.writeInt(size);
            while (iterator.hasNext()) {
                final byte[] bytes = valueSerializer.serialize(topic, iterator.next());
                out.writeInt(bytes.length);
                out.write(bytes);
            }
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("unable to serialize List", e);
        }
        return baos.toByteArray();
    }

    @Override
    public void close() {

    }
}
