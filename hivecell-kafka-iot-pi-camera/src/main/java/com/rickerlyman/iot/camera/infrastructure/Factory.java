package com.rickerlyman.iot.camera.infrastructure;

import com.rickerlyman.iot.camera.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.camera.core.topology.DetectionTopologyBuilder;
import com.rickerlyman.iot.camera.infrastructure.vision.opencv.SSDMobileDetectionNetwork;
import com.rickerlyman.iot.camera.infrastructure.vision.opencv.ULFGFaceDetectionNetwork;
import com.rickerlyman.iot.camera.infrastructure.vision.opencv.YOLODetectionNetwork;
import com.rickerlyman.iot.camera.infrastructure.vision.triton.SSDV2DetectionNetwork;
import com.rickerlyman.iot.camera.infrastructure.vision.triton.TritonHttpClient;
import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import com.rickerlyman.iot.common.serialization.SerdesWrapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class Factory {

    @Getter(lazy = true)
    private static final Factory instance = new Factory(
        ConfigFactory.load(),
        ConfigFactory.parseResources("default-application.conf"));

    private Config config;
    private TopologyMetaDataCatalog catalog;

    private Factory(Config... configs) {
        this.config = Arrays.stream(configs)
            .reduce(Config::withFallback)
            .orElseThrow(IllegalStateException::new)
            .resolve();
        this.catalog = initCatalog();
    }

    private TopologyMetaDataCatalog initCatalog() {
        return new TopologyMetaDataCatalog(
            new TopologyMetaDataCatalog.Topics(
                TopicMetaData.with(
                    config.getString("kafka.stream.detection.src.sensor.name"),
                    SerdesWrapper.String(),
                    SerdesWrapper.ByteArray(),
                    config.getString("kafka.stream.detection.src.sensor.start.offset")),
                TopicMetaData.with(
                    config.getString("kafka.stream.detection.dst.cloud.name"),
                    SerdesWrapper.String(),
                    SerdesWrapper.String())));
    }

    public KafkaStreams createYoloDetectionStream(String[] args) {
        Properties properties = configureStream(config.getString("kafka.stream.detection.src.sensor.client.id"));
        final Topology topology =
            new DetectionTopologyBuilder(this.catalog, new YOLODetectionNetwork(args[1], args[2], args[3]))
                .buildTopology(properties);
        return new KafkaStreams(topology, properties);
    }

    public KafkaStreams createSsdmDetectionStream(String[] args) {
        Properties properties = configureStream(config.getString("kafka.stream.detection.src.sensor.client.id"));
        final Topology topology =
            new DetectionTopologyBuilder(this.catalog, new SSDMobileDetectionNetwork(args[1], args[2], args[3]))
                .buildTopology(properties);
        return new KafkaStreams(topology, properties);
    }

    public KafkaStreams createUlfgDetectionStream(String[] args) {
        Properties properties = configureStream(config.getString("kafka.stream.detection.src.sensor.client.id"));
        final Topology topology =
            new DetectionTopologyBuilder(this.catalog, new ULFGFaceDetectionNetwork(args[1]))
                .buildTopology(properties);
        return new KafkaStreams(topology, properties);
    }

    public KafkaStreams createSsdV2TritonDetectionStream(String[] args) {
        Properties properties = configureStream(config.getString("kafka.stream.detection.src.sensor.client.id"));
        final Topology topology =
            new DetectionTopologyBuilder(this.catalog,
                new SSDV2DetectionNetwork(() -> new TritonHttpClient(args[1], Integer.parseInt(args[2]),
                    "ssd_mobilenet_v2", 300, 300, 3, 1),
                    args[3]))
                .buildTopology(properties);
        return new KafkaStreams(topology, properties);
    }

    private Properties configureStream(final String clientId) {
        final Properties properties = new Properties();

        String patternStr = "[^\\:]+:[0-9]+$";
        Pattern pattern = Pattern.compile(patternStr);
        List<String> brokers = config.getStringList("kafka.brokers");
        if (!brokers.stream().allMatch(broker -> pattern.matcher(broker).matches())) {
            throw new IllegalArgumentException(
                String.format("Please set correct kafka.brokers in default-application.conf, found: %s", brokers));
        }

        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", brokers));
        properties.put(StreamsConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,
            String.format("%s-%s",
                config.getString("kafka.application.id"),
                config.getString("kafka.version.number")));
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties.put(StreamsConfig.STATE_DIR_CONFIG, config.getString("kafka.state.dir"));

        properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, config.getString("kafka.processing.num.stream.threads"));
        properties.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, config.getString("kafka.processing.num.standby.replicas"));
        properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, config.getString("kafka.processing.replication.factor"));

        properties.put(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);

        //this will be required in case of frame chunk processing
        //properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        return properties;
    }

}
