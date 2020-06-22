"""
Capture frames from a camera using openCV and publish on an MQTT topic.
"""
import time
import paho.mqtt.client as mqtt
import io
import base64
import picamera
import picamera.array


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

def main():
    MQTT_PATH = "pi-cam"

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
    time.sleep(2)  # Webcam light should come on if using one

    try:
        while True:
            stream = io.BytesIO()
            camera.capture(stream, format='png')
            stream.seek(0)
            value = base64.b64encode(stream.read())
            ack = mqttc.publish(MQTT_PATH, value)
            ack.wait_for_publish()
            print("after " + str(ack.is_published()))
            # publisher.single(topic=MQTT_PATH, payload=value, hostname="192.168.31.159", port=1883)
            # print("published: "+str(len(value)))
            time.sleep((1 / fps) / 2)
            # time.sleep(1)
    finally:
        mqttc.disconnect()
        camera.close()


if __name__ == "__main__":
    main()
