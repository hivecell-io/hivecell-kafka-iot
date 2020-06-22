import paho.mqtt.publish as publish
import random
import time
import sys
import getopt
from sense_hat import SenseHat

SENSORS = ['s1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9']


class Light(object):

    Rd = (255, 0, 0)
    Gn = (0, 255, 0)
    Bl = (0, 0, 255)
    Yl = (255, 255, 0)
    Og = (255, 128, 0)
    Wt = (255, 255, 255)
    Gy = (255, 0, 255)
    __ = (0, 0, 0)

    def __init__(self, sensor):
        self.sensor = sensor
        self.coordinates = [[x, y] for x in range(8) for y in range(8)]
        self.size = len(self.coordinates)
        self.position = 0
        self.x = 0
        self.y = 0

    def display_pixel(self, temperature):
        self.sensor.set_pixel(self.x, self.y, self._set_color(temperature))

        if self.position < self.size - 1:
            self.position = self.position + 1
        else:
            self.position = 0
            self.sensor.clear()

        self.x = self.coordinates[self.position][0]
        self.y = self.coordinates[self.position][1]

    def _set_color(self, temperature):
        if temperature < 0:
            return self.Bl
        elif 0 < temperature < 10:
            return self.Wt
        elif 10 < temperature < 20:
            return self.Yl
        elif 20 < temperature < 30:
            return self.Og
        else:
            return self.Rd


def timestamp():
    return str(time.time()).split(".")[0]


if __name__ == "__main__":
    argv = getopt.getopt(sys.argv)
    if len(argv) < 3:
        raise ValueError("Please specify MQTT broker host, port, and target topic")

    broker = argv[0]
    port = int(argv[1])
    topic = argv[2]

    sense = SenseHat()
    sense.clear()

    l = Light(sense)

    previous_ts = timestamp()
    msgs = list()

    try:
        while True:
            # Round the values to one decimal place
            t = round(sense.get_temperature(), 8)
            p = round(sense.get_pressure(), 8)
            h = round(sense.get_humidity(), 8)

            ts = timestamp()
            sensor_id = random.choice(SENSORS)

            # increase qos if there will be more that one broker
            # msgs.append({
            #     'topic': MQTT_PATH, 'qos': 1,
            #     'payload': "{sensor},{time},{t}C,{h}%,{p}hPa"
            #         .format(sensor=sensor_id, time=ts, t=t, h=h, p=p)})

            msgs.append({
                'topic': topic,
                'payload': "{sensor},{time},{t}C,{h}%,{p}hPa"
                    .format(sensor=sensor_id, time=ts, t=t, h=h, p=p)})

            if ts != previous_ts:
                l.display_pixel(t)
                publish.multiple(msgs, hostname=broker, port=port)
                previous_ts = ts
                msgs = list()

    except KeyboardInterrupt:
        sense.clear()
        del msgs
        exit(0)
