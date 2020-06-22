package com.rickerlyman.iot.camera.core.vision;

import com.rickerlyman.iot.camera.core.model.ProcessedFrame;

public interface Detection<T> {
    ProcessedFrame<T> detect(byte[] rawFrame);
}
