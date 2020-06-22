package com.rickerlyman.iot.sense.infrastructure;

import com.rickerlyman.iot.common.catalog.store.WindowedStoreMetaData;
import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import com.rickerlyman.iot.common.serialization.SerdesWrapper;
import com.rickerlyman.iot.sense.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.sense.core.extractor.SensorTimestampExtractor;
import com.rickerlyman.iot.sense.core.handler.IgnoreRecordTooLargeHandler;
import com.rickerlyman.iot.sense.core.topology.AggregatorTopologyBuilder;
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
    private static final Factory instance = new Factory(ConfigFactory.load(), ConfigFactory.parseResources("default-application.conf"));

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
                    config.getString("kafka.stream.aggregator.src.sensor.name"),
                    SerdesWrapper.String(), SerdesWrapper.String(),
                    new SensorTimestampExtractor(),
                    config.getString("kafka.stream.aggregator.src.sensor.start.offset")),
                TopicMetaData.with(
                    config.getString("kafka.stream.aggregator.dst.cloud.name"),
                    SerdesWrapper.String(),
                    SerdesWrapper.String()),
                TopicMetaData.with(
                    config.getString("kafka.stream.aggregator.dst.sensor.name"),
                    SerdesWrapper.String(),
                    SerdesWrapper.String())),
            new TopologyMetaDataCatalog.Stores(
                WindowedStoreMetaData.with(
                    config.getString("kafka.stream.aggregator.store.name"),
                    config.getString("kafka.stream.aggregator.store.time.indicator"),
                    config.getLong("kafka.stream.aggregator.store.retention.period"),
                    config.getLong("kafka.stream.aggregator.store.window.size"),
                    config.getLong("kafka.stream.aggregator.store.late.arrivals"))));
    }

    public KafkaStreams createAggregatorStream() {
        Properties properties = configureStream(config.getString("kafka.stream.aggregator.src.sensor.client.id"));
        final Topology topology =
            new AggregatorTopologyBuilder(this.catalog)
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

        // This parameter controls the number of bytes allocated for caching.
        // Specifically, for a processor topology instance with T threads and C bytes allocated for caching,
        // each thread will have an even C/T bytes to construct its own cache and use as it sees fit among its tasks.
        // This means that there are as many caches as there are threads, but no sharing of caches across threads happens.

        // Records caches defines how much data will be stored in memory in operations such as 'windowed aggregation',
        // and used to calculate results after aggregation.
        // Here we set 100MB and we set 10 NUM_STREAM_THREADS_CONFIG, so each thread will have cache of 10MB
        properties.put(StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG, IgnoreRecordTooLargeHandler.class);
        properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 100 * 1024 * 1024L);

        // Records should be flushed every 10 seconds. This is less than the default
        // in order to keep this example interactive.
        // if 'processing.guarantee' is set to 'exactly_once', the default value is 100, otherwise the default value is 30000.
        // properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        // For illustrative purposes we disable record caches.
        // properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        return properties;
    }

}
