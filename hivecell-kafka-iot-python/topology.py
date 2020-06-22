import base64
import functools
import json
import argparse

from collections import namedtuple
from typing import Dict, Callable, Iterable, Any

from lazy_streams import stream, _LazyListStream
from inference.object_detection import Detector

from client.consumer import StreamConsumer
from client.producer import StreamProducer

RawFrame = namedtuple('RawFrame', 'time data')
ProcessedFrame = namedtuple('ProcessedFrame', 'time data')


class Topology(object):
    def __init__(self, consumer_conf: Dict, producer_conf: Dict):
        self.consumer = StreamConsumer(consumer_conf)
        self.producer = StreamProducer(producer_conf)

    def start(self, chain: Callable[[Detector, Iterable[Any]], _LazyListStream]):
        chain = functools.partial(chain, Detector())
        self.consumer.consume(chain, self.producer.produce)


def from_json(json_data):
    return json.loads(json_data, object_hook=lambda d: namedtuple('RawFrame', d.keys())(*d.values()))


def decode(raw_frame: namedtuple):
    im_bytes = base64.b64decode(raw_frame.data.encode())
    return RawFrame(raw_frame.time, im_bytes)


def process(detector: Detector, data) -> "_LazyListStream":
    lst = list()
    lst.append(data)
    return stream(lst) \
        .map(lambda json_data: from_json(json_data)) \
        .map(lambda kafka_message: decode(kafka_message)) \
        .map(lambda j: detector.detect(j.data))


def parse_args():
    """Parse input arguments."""
    desc = ('Read raw frames from input topic '
            'perform inference through TensorRT '
            'and write detected objects to output topic')
    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument('Broker', metavar='broker', type=str, help='<host>:<port>')
    parser.add_argument('Source', metavar='source-topic', type=str, help='topic with raw frames')
    parser.add_argument('Target', metavar='target-topic', type=str, help='topic where to put inference results')

    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    broker = args.Broker

    consumer_conf = conf = {
        'bootstrap.servers': broker,
        'group.id': 'hivecell_kstream_camera_1_0.0.1',
        'client.id': 'sensor-frame-recognizer',
        'session.timeout.ms': 6000,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False,
        'auto.commit.interval.ms': 1000,
        'ack': 1,
        'topic': args.Source}

    producer_conf = conf = {
        'bootstrap.servers': broker,
        'buffer.size': 10,
        'topic': args.Target}

    t = Topology(consumer_conf, producer_conf)
    t.start(process)
