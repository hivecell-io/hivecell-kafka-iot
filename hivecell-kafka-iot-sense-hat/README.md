# Hivecell-KafkaStreams-Aggregation
**This repository contains example of KafkaStreams aggregation application at the Edge, for ARM64 architecture.**

This application read sensor readings like _temperature_, _humidity_, _pressure_ from `temperature` topic, aggregate them in 5 min window. 
After which calculate avg on each indicator and perform additional calculations like _summerSimmerIndex_, _heatIndex_, _dewPoint_, _humidityIndex_ based on results.
If average temperature for 5 min is higher than 30 C, command to beep speaker is sent to RaspberryPi through `sensor-command` topic.
Results are written to `com.rlr.aggregated-temperature` topic.

**Prerequisites**:
 - 3 Hivecell (CPU intensive) instances, where Confluent stack will be deployed (3 nodes required for Confluent Replicator)
 - RaspberryPi + PiCamera
 - Speaker connected to RaspberryPi

To able to run this stream you need to perform next steps:

1. Modify [default-application.conf](src/main/resources/default-application.conf) and specify `kafka.brokers` property
2. Build JAR file and copy it to Hivecell.
3. In Kafka create three topics:
 - temperature
 - com.rlr.aggregated-temperature
 - sensor-command
4. Copy next scripts to raspberry-pi:
 - [kafka_consumer.py](src/main/python/kafka_consumer.py)
 - [sense_reading_mqtt.py](src/main/python/sense_reading_mqtt.py)
5. Run stream
    ```bash
    java -cp hivecell-kafka-iot-sense-hat-1.0-SNAPSHOT.jar com.rickerlyman.iot.sense.Driver
    ```
   
6. Run [sense_reading_mqtt.py](src/main/python/sense_reading_mqtt.py) 
   Example:
   ```bash 
   python3 sense_reading_mqtt.py {kafka_broker_host} {kafka_broker_port} temperature
   ```
7. Run [kafka_consumer.py](src/main/python/kafka_consumer.py) 
   Example:
   ```bash 
   python3 kafka_consumer.py {kafka_bootstrap_servers} {consumet_group} sensor-command
   ```