package com.rickerlyman.iot.common.serialization;

import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.WindowedSerdes;

import java.util.List;

public class SerdesWrapper extends Serdes {

    public static <T> Serde<List<T>> List(final Serializer<T> serializer, final Deserializer<T> deserializer) {
        return new ListSerde<>(serdeFrom(serializer, deserializer));
    }

    public static <T> Serde<List<T>> List(final Serde<T> serde) {
        return new ListSerde<>(serde);
    }

    @SneakyThrows
    public static  <T extends SpecificRecordBase> Serde<T> avroSerdeFrom(final Class<T> type){
        Class<?> recordClass = Class.forName(type.getName());
        Schema data = new SpecificData(recordClass.getClassLoader()).getSchema(recordClass);
        return new SpecificAvroSerde<>(data);
    }

    public static  <T> Serde<Windowed<T>> timeWindowedSerdeFrom(final Class<T> type){
        return WindowedSerdes.timeWindowedSerdeFrom(type);
    }

}
