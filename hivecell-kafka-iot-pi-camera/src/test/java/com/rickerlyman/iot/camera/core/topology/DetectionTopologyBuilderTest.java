package com.rickerlyman.iot.camera.core.topology;

import com.rickerlyman.iot.camera.core.TestNGTopologyBase;
import com.rickerlyman.iot.camera.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import com.rickerlyman.iot.common.serialization.SerdesWrapper;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster;
import org.apache.kafka.test.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.util.Locale;
import java.util.Properties;

public class DetectionTopologyBuilderTest extends TestNGTopologyBase {
    public static final EmbeddedKafkaCluster CLUSTER = new EmbeddedKafkaCluster(1);

    private final String rawFrames = "input";
    private final String processedFrames = "output";

    private TopologyMetaDataCatalog catalog;
    private Properties config;

    @BeforeClass
    public void setUpClass() {
        catalog = new TopologyMetaDataCatalog(
            new TopologyMetaDataCatalog.Topics(
                TopicMetaData.with(rawFrames, SerdesWrapper.String(), SerdesWrapper.ByteArray(), "earliest"),
                TopicMetaData.with(processedFrames, SerdesWrapper.String(), SerdesWrapper.String())));
        config = mkProperties(Utils.mkMap(
            Utils.mkEntry(StreamsConfig.APPLICATION_ID_CONFIG, getClass().getSimpleName().toLowerCase(Locale.getDefault())),
            Utils.mkEntry(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getPath()),
            Utils.mkEntry(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0),
            Utils.mkEntry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
            Utils.mkEntry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "bogus")
        ));
    }

    @BeforeMethod
    public void setUpTest() throws InterruptedException {
        CLUSTER.createTopic(rawFrames, 3, 1);
        CLUSTER.createTopic(processedFrames, 3, 1);
    }

    @AfterMethod
    public void tearDownTest() throws InterruptedException {
        CLUSTER.deleteTopics(rawFrames, processedFrames);
    }




}