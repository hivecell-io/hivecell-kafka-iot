package com.rickerlyman.iot.common;

import com.rickerlyman.iot.common.catalog.topic.TopicMetaData;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


public abstract class TopologyBuilder extends StreamsBuilder {

    public abstract Topology buildTopology(Properties properties);

    protected <K, V, KR, VR> KStream<KR, VR> stream(TopicMetaData<K, V> topicMetaData,
                                                    Function<? super K, ? extends KR> keyTransformer,
                                                    Function<? super V, ? extends VR> valueTransformer) {
        return stream(topicMetaData).map((k, v) -> KeyValue.pair(keyTransformer.apply(k), valueTransformer.apply(v)));
    }


    protected <K, V, VR> KStream<K, VR> stream(TopicMetaData<K, V> topicMetaData,
                                               Function<? super V, ? extends VR> valueTransformer) {
        return stream(topicMetaData).mapValues(valueTransformer::apply);
    }

    protected <K, V, KR> KStream<KR, V> stream(TopicMetaData<K, V> topicMetaData,
                                               BiFunction<? super K, ? super V, ? extends KR> keyTransformer) {
        return stream(topicMetaData).selectKey(keyTransformer::apply);
    }

    protected <K, V> KStream<K, V> stream(TopicMetaData<K, V> topicMetaData) {
        return this.stream(topicMetaData.getName(),
            Consumed.with(
                topicMetaData.getKeySerde(),
                topicMetaData.getValueSerde(),
                topicMetaData.getTimestampExtractor(),
                topicMetaData.getResetPolicy()));
    }

    protected <K, V, KR, VR> void to(KStream<K, V> stream,
                                     TopicMetaData<KR, VR> topicMetaData,
                                     BiFunction<? super K, ? super V, ? extends KeyValue<? extends KR, ? extends VR>> pairTransformer) {
        stream
            .<KR, VR>map(pairTransformer::apply)
            .to(topicMetaData.getName(), Produced.with(topicMetaData.getKeySerde(), topicMetaData.getValueSerde()));
    }

    protected <K, V, VR> void to(KStream<K, V> stream,
                                 TopicMetaData<K, VR> topicMetaData,
                                 Function<? super V, ? extends VR> valueTransformer) {
        stream
            .<VR>mapValues(valueTransformer::apply)
            .to(topicMetaData.getName(), Produced.with(topicMetaData.getKeySerde(), topicMetaData.getValueSerde()));
    }

    protected <K, V> void to(KStream<K, V> stream, TopicMetaData<K, V> topicMetaData) {
        //Example how dynamically write messages to different topics
        //stream.to((key, value, recordContext) -> recordContext.topic());
        stream.to(topicMetaData.getName(), Produced.with(topicMetaData.getKeySerde(), topicMetaData.getValueSerde()));
    }

    protected <K, V> void to(KStream<K, V> stream) {
        to(stream, (key, val) -> System.out.println("Key: " + key + " -> Value:" + val));
    }

    protected <K, V> void to(KStream<K, V> stream, BiConsumer<K, V> consumer) {
        stream.foreach(consumer::accept);
    }

}
