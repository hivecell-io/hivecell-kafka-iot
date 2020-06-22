package com.rickerlyman.iot.common.util;

import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Avro {

    private Avro() {
    }

    @SneakyThrows
    public static <T extends SpecificRecordBase> String avroToJson(T value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder enc = EncoderFactory.get().jsonEncoder(value.getSchema(), out);
        DatumWriter<T> writer = new SpecificDatumWriter<>(value.getSchema());
        writer.write(value, enc);
        enc.flush();
        return out.toString();
    }

    public static <T extends SpecificRecordBase> T avroFromJson(String json, Schema schema) throws IOException {
        Decoder dec = DecoderFactory.get().jsonDecoder(schema, json);
        DatumReader<T> reader = new SpecificDatumReader<T>(schema);
        return reader.read(null, dec);
    }
}
