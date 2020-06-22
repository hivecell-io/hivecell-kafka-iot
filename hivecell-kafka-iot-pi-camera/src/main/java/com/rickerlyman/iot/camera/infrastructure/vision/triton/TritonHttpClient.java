package com.rickerlyman.iot.camera.infrastructure.vision.triton;

import lombok.SneakyThrows;

import java.net.URI;

public class TritonHttpClient extends TritonClient {

    private static final String contentType = "application/octet-stream";
    private final String nVInferRequest;
    private final String uri;
    private final java.net.http.HttpClient client;

    public TritonHttpClient(String host, int port, String modelName, int imgH, int imgW, int numOfLayers, int batchSize) {
        super(host, port);
        this.nVInferRequest = String.format("input { name: \"Input\" dims: %s dims: %s dims: %s  } output { name: \"NMS\" } batch_size: %s",
            numOfLayers, imgH, imgW, batchSize);
        this.uri = String.format("http://%s:%s/api/infer/%s?format=binary", host, port, modelName);
        this.client = java.net.http.HttpClient.newHttpClient();
    }

    @Override
    @SneakyThrows
    public byte[] infer(byte[] bytes) {
        java.net.http.HttpRequest requestBodyOfByteArray = java.net.http.HttpRequest.newBuilder()
            .header("Content-Type", contentType)
            .header("NV-InferRequest", nVInferRequest)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes))
            .uri(URI.create(uri))
            .build();

        java.net.http.HttpResponse<byte[]> responseBodyOfString = client.send(
            requestBodyOfByteArray, java.net.http.HttpResponse.BodyHandlers.ofByteArray());

        return responseBodyOfString.body();
    }

}
