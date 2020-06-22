package com.rickerlyman.iot.camera.core.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RawFrame {
    private final Long timestamp;
    private final byte[] frame;
}
