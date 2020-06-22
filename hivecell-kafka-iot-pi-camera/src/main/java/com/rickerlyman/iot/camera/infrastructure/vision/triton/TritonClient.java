package com.rickerlyman.iot.camera.infrastructure.vision.triton;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TritonClient {
    private final String host;
    private final int port;

    public abstract byte[] infer(byte[] in);

}
