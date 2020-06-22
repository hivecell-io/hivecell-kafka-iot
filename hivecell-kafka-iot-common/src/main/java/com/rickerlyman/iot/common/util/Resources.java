package com.rickerlyman.iot.common.util;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Resources {

    public static <T> T asObject(String resource, TypeReference<T> typeReference) {
        return Jackson.fromJsonString(asInputStream(resource), typeReference);
    }

    public static <T> T byAbsolutePath(String absolutePath, TypeReference<T> typeReference) throws FileNotFoundException {
        return Jackson.fromJsonString(new FileInputStream(absolutePath), typeReference);
    }

    public static InputStream asInputStream(String resource) {
        return Resources.class.getResourceAsStream(resource);
    }

}
