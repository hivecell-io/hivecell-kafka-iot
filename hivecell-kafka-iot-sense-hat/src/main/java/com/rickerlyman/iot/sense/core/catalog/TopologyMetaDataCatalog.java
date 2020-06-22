package com.rickerlyman.iot.sense.core.catalog;

import com.rickerlyman.iot.common.catalog.store.WindowedStoreMetaData;
import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TopologyMetaDataCatalog {

    public final Topics topics;
    public final Stores stores;

    //TODO: In future maybe it will be more convenient to create separate container which will contain all necessary
    // configuration for Topology in general like, input and output topics and state stores in one object,
    // instead of passing separate objects

    @AllArgsConstructor
    public static class Topics {
        public final TopicMetaData<String, String> rawTemperature;
        public final TopicMetaData<String, String> avrTemperature;
        public final TopicMetaData<String, String> sensorCommand;
    }

    @AllArgsConstructor
    public static class Stores {
        public final WindowedStoreMetaData avrTemperatureSimpleStore;
    }


}
