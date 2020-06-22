"""
Capture frames from a camera using openCV and publish on an MQTT topic.
"""
import time
import paho.mqtt.client as mqtt
import io
import base64
import picamera
import picamera.array
import math
import random, string
import json


def on_connect(mqttc, obj, flags, rc):
    print("connect rc: " + str(rc))


def on_message(mqttc, obj, msg):
    print(msg.topic + " " + str(msg.qos) + " " + str(msg.payload))


def on_publish(mqttc, obj, mid):
    print("mid: " + str(mid))


def on_subscribe(mqttc, obj, mid, granted_qos):
    print("Subscribed: " + str(mid) + " " + str(granted_qos))


def on_log(mqttc, obj, level, string):
    print(string)


def randomword(length):
    return ''.join(random.choice(string.ascii_lowercase) for i in range(length))


def publish_encoded_image(mqttc, topic, encoded, packet_size):
    end = packet_size
    start = 0
    length = len(encoded)
    pic_id = randomword(8)
    pos = 0
    no_of_packets = math.ceil(length / packet_size)

    print({"end":end, "start": start, "length": length, "pic_id": pic_id, "pos": pos, "no_of_packets": no_of_packets})

    while start <= len(encoded):
        data = {"data": encoded[start:end], "pic_id": pic_id, "pos": pos, "size": no_of_packets}
        mqttc.publish(topic, json.JSONEncoder().encode(data))
        end += packet_size
        start += packet_size
        pos = pos + 1
        print({"end": end, "start": start, "length": length, "pic_id": pic_id, "pos": pos,
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
    fps = 12
    camera = picamera.PiCamera(sensor_mode=7, framerate=fps)
    camera.resolution = (640, 480)

    # Webcam light should come on if using one
    time.sleep(2)

    try:
        while True:
            stream = io.BytesIO()
            camera.capture(stream, format='png')
            stream.seek(0)
            value = base64.b64encode(stream.read()).decode()
            publish_encoded_image(mqttc, MQTT_PATH, value, PACKET_SIZE)
            time.sleep(1 / fps)
    finally:
        mqttc.disconnect()
        camera.close()


if __name__ == "__main__":
    main()
