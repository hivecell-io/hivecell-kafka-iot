package com.rickerlyman.iot.sense.core.topology;

import com.rickerlyman.iot.sense.core.TestNGTopologyBase;
import com.rickerlyman.iot.sense.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.common.catalog.store.WindowedStoreMetaData;
import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import com.rickerlyman.iot.sense.core.extractor.SensorTimestampExtractor;
import com.rickerlyman.iot.common.serialization.SerdesWrapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.test.TestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

public class AggregatorTopologyBuilderTest extends TestNGTopologyBase {
    private static final StringDeserializer STRING_DESERIALIZER = new StringDeserializer();
    private static final StringSerializer STRING_SERIALIZER = new StringSerializer();

    private final String rawTemperature = "input";
    private final String avrTemperature = "output";
    private final String senseCommand = "command";
    private final String queryableStoreName = "temperature-store";

    private TopologyMetaDataCatalog catalog;
    private Properties config;


    @BeforeClass
    public void setUp() {
        catalog = new TopologyMetaDataCatalog(
            new TopologyMetaDataCatalog.Topics(
                TopicMetaData.with(rawTemperature, SerdesWrapper.String(), SerdesWrapper.String(), new SensorTimestampExtractor(), "earliest"),
                TopicMetaData.with(avrTemperature, SerdesWrapper.String(), SerdesWrapper.String()),
                TopicMetaData.with(senseCommand, SerdesWrapper.String(), SerdesWrapper.String())),
            new TopologyMetaDataCatalog.Stores(
                WindowedStoreMetaData.with(queryableStoreName, "PT_M", 10, 5, 1)));
        config = mkProperties(Utils.mkMap(
            Utils.mkEntry(StreamsConfig.APPLICATION_ID_CONFIG, getClass().getSimpleName().toLowerCase(Locale.getDefault())),
            Utils.mkEntry(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, "com.rickerlyman.iot.sense.core.extractor.SensorTimestampExtractor"),
            Utils.mkEntry(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getPath()),
            Utils.mkEntry(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0),
            Utils.mkEntry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
            Utils.mkEntry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "bogus")
        ));
    }

    @Test
    public void testBuildTopologyNoAlert() {

        AggregatorTopologyBuilder topology = new AggregatorTopologyBuilder(catalog);

        try (final TopologyTestDriver driver = new TopologyTestDriver(topology.buildTopology(config), config)) {
            final TestInputTopic<String, String> inputTopic =
                driver.createInputTopic(rawTemperature, STRING_SERIALIZER, STRING_SERIALIZER);

            inputTopic.pipeKeyValueList(createTopicInput("/data/Aggregator/NoAlert/SenseReading.json"));

            verify(
                drainProducerRecords(driver, avrTemperature, STRING_DESERIALIZER, STRING_DESERIALIZER),
                createTopicOutput("/data/Aggregator/NoAlert/AggregatedData.json"));
            verify(
                drainProducerRecords(driver, senseCommand, STRING_DESERIALIZER, STRING_DESERIALIZER),
                Collections.emptyList());
        }
    }

    @Test
    public void testBuildTopologyWithAlert() {

        AggregatorTopologyBuilder topology = new AggregatorTopologyBuilder(catalog);

        try (final TopologyTestDriver driver = new TopologyTestDriver(topology.buildTopology(config), config)) {
            final TestInputTopic<String, String> inputTopic =
                driver.createInputTopic(rawTemperature, STRING_SERIALIZER, STRING_SERIALIZER);

            inputTopic.pipeKeyValueList(createTopicInput("/data/Aggregator/WithAlert/SenseReading.json"));

            verify(
                drainProducerRecords(driver, avrTemperature, STRING_DESERIALIZER, STRING_DESERIALIZER),
                createTopicOutput("/data/Aggregator/WithAlert/AggregatedData.json"));
            verify(
                drainProducerRecords(driver, senseCommand, STRING_DESERIALIZER, STRING_DESERIALIZER),
                createTopicOutput("/data/Aggregator/WithAlert/SenseCommand.json"));
        }
    }

}