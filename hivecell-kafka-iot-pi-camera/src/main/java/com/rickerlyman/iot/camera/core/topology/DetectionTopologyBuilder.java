package com.rickerlyman.iot.camera.core.topology;

import com.rickerlyman.iot.camera.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.camera.core.vision.Detection;
import com.rickerlyman.iot.common.TopologyBuilder;
import com.rickerlyman.iot.common.util.Jackson;
import lombok.AllArgsConstructor;
import org.apache.kafka.streams.Topology;

import java.util.Properties;

@AllArgsConstructor
public class DetectionTopologyBuilder extends TopologyBuilder {
    private final TopologyMetaDataCatalog catalog;
    private final Detection<?> detection;

    @Override
    public Topology buildTopology(Properties properties) {

        this.to(
            this.stream(catalog.topics.cameraFrames)
                .mapValues(detection::detect)
                .mapValues(Jackson::toJsonString),
            catalog.topics.objectPosition);

        return this.build(properties);
    }
}
