import io
import time
import picamera
import paho.mqtt.client as mqtt
import logging
import argparse

logging.basicConfig(level=logging.INFO)


def parse_args():
    """Parse input arguments."""
    desc = ('Read raw frames from input topic '
            'perform inference through TensorRT '
            'and write detected objects to output topic')
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument('Resolution', metavar='resolution', type=str,
                        help='expected format <width>x<height>, i.e. 640x480')
    parser.add_argument('Fps', metavar='fps', type=int,
                        help='number of fps that will be sent to mqtt topic')
    parser.add_argument('Ttl', metavar='ttl', type=int,
                        help='number of seconds which specify how long application should run')
    parser.add_argument('Broker', metavar='broker', type=str,
                        help='mqtt broker, default 1883')
    parser.add_argument('Port', metavar='port', type=int,
                        help='mqtt port')
    parser.add_argument('TargetTopic', metavar='target', type=str,
                        help='topic where to put inference results')

    return parser.parse_args()


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


def timestamp():
    return str(time.time()).split(".")[0]


def write_img_to_stream(mqttc, mqtt_path, stream, qos=0):
    stream.seek(0)  # seek to location 0 of stream_img
    mqttc.publish(mqtt_path, stream.read(), qos=qos)
    stream.seek(0)
    stream.truncate()


def gen_seq(mqttc, mqtt_path, ttl):
    start = time.time()
    count = 0
    stream = io.BytesIO()
    while True:
        yield stream
        write_img_to_stream(mqttc, mqtt_path, stream)
        count += 1
        if time.time() - start > ttl:
            finish = time.time()
            print('Sent %d images in %d seconds at %.2ffps' % (count, finish - start, count / (finish - start)))
            break


if __name__ == "__main__":
    args = parse_args()
    resolution = args.Resolution
    fps = args.Fps
    ttl = args.Ttl
    broker = args.Broker
    port = args.Port
    target = args.TargetTopic
    (width, height) = resolution.split('x')

    mqttc = mqtt.Client("pi-camera-publisher")
    mqttc.connect(host=broker, port=port, keepalive=60)
    mqttc.on_message = on_message
    mqttc.on_connect = on_connect
    mqttc.on_publish = on_publish
    mqttc.on_subscribe = on_subscribe
    mqttc.on_log = on_log

    # Wait for connection setup to complete
    time.sleep(4)
    mqttc.loop_start()

    try:
        with picamera.PiCamera() as camera:
            camera.resolution = (int(width), int(height))
            camera.framerate = fps
            time.sleep(2)
            # Use the video-port for captures...
            camera.capture_sequence(gen_seq(mqttc, target, ttl), 'jpeg', use_video_port=True)
    finally:
        print("done")
