package com.rickerlyman.iot.sense.core.topology;

import com.rickerlyman.iot.common.TopologyBuilder;
import com.rickerlyman.iot.common.serialization.SerdesWrapper;
import com.rickerlyman.iot.common.util.Avro;
import com.rickerlyman.iot.sense.core.catalog.TopologyMetaDataCatalog;
import com.rickerlyman.iot.sense.core.transform.AggregatorOps;
import com.rickerlyman.iot.sense.core.transform.SenseOps;
import com.rickerlyman.iot.schema.SenseReadingInternal;
import com.rickerlyman.iot.schema.SenseReadingOutput;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class AggregatorTopologyBuilder extends TopologyBuilder {

    private final TopologyMetaDataCatalog catalog;
    private final String queryableStoreName;
    private final Duration windowChangelogRetention;
    private final Duration windowSize;
    private final Duration windowLateArrivals;

    public AggregatorTopologyBuilder(TopologyMetaDataCatalog catalog) {
        this.catalog = catalog;
        this.queryableStoreName = catalog.stores.avrTemperatureSimpleStore.getName();
        this.windowChangelogRetention = catalog.stores.avrTemperatureSimpleStore.getRetention();
        this.windowSize = catalog.stores.avrTemperatureSimpleStore.getSize();
        this.windowLateArrivals = catalog.stores.avrTemperatureSimpleStore.getAfterWindowEnd();
    }

    @Override
    public Topology buildTopology(Properties properties) {
        Serde<SenseReadingInternal> avroSerde = SerdesWrapper.avroSerdeFrom(SenseReadingInternal.class);
        Materialized<String, SenseReadingInternal, WindowStore<Bytes, byte[]>> aggregationSerde = Materialized
            .<String, SenseReadingInternal>as(Stores.inMemoryWindowStore(queryableStoreName, windowChangelogRetention, windowSize, false))
            .withKeySerde(SerdesWrapper.String())
            .withValueSerde(avroSerde)
            .withLoggingEnabled(Collections.emptyMap()) //this required for fault tolerance, works slower
            .withCachingEnabled(); //this will reduce size of the data that goes in to changelog
//            .withLoggingDisabled() //when disabled changelog topic won't be created no fault tolerance for state, works faster
//            .withCachingDisabled(); //withCachingDisabled no need to cache before writing downstream and changelog topic, works faster

        Grouped<String, SenseReadingInternal> groupingSerde = Grouped.with(SerdesWrapper.String(), avroSerde);

        KStream<String, SenseReadingOutput> stream = this.stream(catalog.topics.rawTemperature)
            .flatMap(AggregatorOps::parseTemperature)
            .groupByKey(groupingSerde)
            .windowedBy(TimeWindows.of(windowSize).grace(windowLateArrivals))
            .aggregate(AggregatorOps::initTempCollection, AggregatorOps::collectTemp, aggregationSerde)
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .mapValues(AggregatorOps::calculateAvgTemp)
            .toStream((key, value) -> key.key());

        this.to(stream.mapValues(Avro::avroToJson), catalog.topics.avrTemperature);
        this.to(stream.flatMapValues(SenseOps::alert).mapValues(Avro::avroToJson), catalog.topics.sensorCommand);

        return this.build(properties);
    }
}
