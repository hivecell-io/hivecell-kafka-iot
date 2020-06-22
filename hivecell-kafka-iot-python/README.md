# Hivecell-Kafka-DeepVision-Python

**This repository contains example of SSDMobileNetV2 network integrated into KafkaConsumerProducer application at the Edge.**

**Prerequisites**:
 - 3 Hivecell (CPU intensive) instances, where Confluent stack will be deployed (3 nodes required for Confluent Replicator)
 - 1 Hivecell (GPU intensive) instance, where NVidia TensorRT will be deployed
 - RaspberryPi + PiCamera
 - [NVidia TensorRT 6](https://docs.nvidia.com/deeplearning/tensorrt/release-notes/tensorrt-6.html)
 - [Confluent Kafka 5.4.1](https://docs.confluent.io/5.4.1/installation/installing_cp/index.html?_ga=2.188498408.1568361680.1592296905-895316610.1590138623)
 - [Confluent Replicator 5.4.1](https://docs.confluent.io/5.4.1/connect/kafka-connect-replicator/index.html)
 - [Confluent MQTT Proxy 5.4.1](https://docs.confluent.io/5.4.1/kafka-mqtt/index.html)
 
**To be able to run this application you need to perform next steps**:

1. At HivecellOne build image from [Dockerfile](Dockerfile) and start container
    ```bash
    sudo docker run -it --device=/dev/nvhost-ctrl\
    --device=/dev/nvhost-ctrl-gpu\
    --device=/dev/nvhost-prof-gpu\
    --device=/dev/nvmap\
    --device=/dev/nvhost-gpu\
    --device=/dev/nvhost-as-gpu\
    {image_name}
    ```

2. In Kafka create two topics:
    - pi-cam
    - com.rlr.recognized-frames

3. Copy [stream_frame.py](cam/stream_frame.py) script to raspberry-pi

4. Go to container
    ```bash
    docker exec -it {container_id} bash
    ```
    
5. Run application
    ```bash
    cd stream-inference/hivecell-kafka-iot-python
    python3 topology.py {kafka_bootstrap_servers} {source_topic} {target_topic}
    ```

5. Run python3 stream_frame.py {resolution} {fps} {ttl} {kafka_broker} {kafka_mqtt_proxy_port} {topic}.
   To run script for 10 minutes at 40 FPS with 400x304 resolution execute script with args like in example
    Example: 
    ```bash 
    python3 stream_frame.py 400x304 40 600 {kafka_host} {kafka_mqtt_proxy_port} pi-cam
   ```