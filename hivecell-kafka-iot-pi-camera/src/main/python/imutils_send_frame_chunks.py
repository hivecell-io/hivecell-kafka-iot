"""
Capture frames from a camera using openCV and publish on an MQTT topic.
"""
import time
import paho.mqtt.client as mqtt
import base64
import math
import random, string
import json

import cv2
import logging
from imutils.video import FPS
from imutils.video import VideoStream

logging.basicConfig(level=logging.INFO)


def on_connect(mqttc, obj, flags, rc):
    logging.info("connect rc: " + str(rc))


def on_message(mqttc, obj, msg):
    logging.debug(msg.topic + " " + str(msg.qos) + " " + str(msg.payload))


def on_publish(mqttc, obj, mid):
    logging.debug("mid: " + str(mid))


def on_subscribe(mqttc, obj, mid, granted_qos):
    logging.info("Subscribed: " + str(mid) + " " + str(granted_qos))


def on_log(mqttc, obj, level, string):
    logging.debug(string)


def randomword(length):
    return ''.join(random.choice(string.ascii_lowercase) for i in range(length))


def timestamp():
    return str(time.time()).split(".")[0]


def publish_encoded_image(mqttc, topic, encoded, packet_size):
    end = packet_size
    start = 0
    length = len(encoded)
    pic_id = randomword(8)
    pos = 0
    no_of_packets = math.ceil(length / packet_size)

    logging.debug(
        {"end": end, "start": start, "length": length, "pic_id": pic_id, "pos": pos, "no_of_packets": no_of_packets})

    while start <= len(encoded):
        data = {"data": encoded[start:end], "pic_id": pic_id, "pos": pos, "size": no_of_packets, "time": timestamp()}
        mqttc.publish(topic, json.JSONEncoder().encode(data))
        end += packet_size
        start += packet_size
        pos = pos + 1
        logging.debug({"end": end, "start": start, "length": length, "pic_id": pic_id, "pos": pos,
                       "no_of_packets": no_of_packets})


def main():
    MQTT_PATH = "pi-cam"
    PACKET_SIZE = 4000

    mqttc = mqtt.Client("pi-camera-publisher")
    mqttc.connect(host="192.168.31.159", port=1883, keepalive=60)
    mqttc.on_message = on_message
    mqttc.on_connect = on_connect
    mqttc.on_publish = on_publish
    mqttc.on_subscribe = on_subscribe
    mqttc.on_log = on_log

    # Wait for connection setup to complete
    time.sleep(4)
    mqttc.loop_start()

    # Open camera
    framerate = 12
    vs = VideoStream(src=0, usePiCamera=True, resolution=(640, 480), framerate=framerate).start()
    time.sleep(2.0)
    fps = FPS().start()

    # Webcam light should come on if using one
    time.sleep(2)

    try:
        while True:
            frame = vs.read()
            retval, buffer = cv2.imencode('.png', frame)
            value = base64.b64encode(buffer).decode()
            publish_encoded_image(mqttc, MQTT_PATH, value, PACKET_SIZE)
            time.sleep(1 / framerate)
    finally:
        fps.stop()
        vs.close()
        mqttc.disconnect()


if __name__ == "__main__":
    main()
