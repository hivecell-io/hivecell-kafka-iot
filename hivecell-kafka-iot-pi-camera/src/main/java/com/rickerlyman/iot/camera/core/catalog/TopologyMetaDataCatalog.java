package com.rickerlyman.iot.camera.core.catalog;

import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TopologyMetaDataCatalog {

    public final Topics topics;

    @AllArgsConstructor
    public static class Topics {
        public final TopicMetaData<String, byte[]> cameraFrames;
        public final TopicMetaData<String, String> objectPosition;
    }


}
