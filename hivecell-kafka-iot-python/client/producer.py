import json
import sys
from typing import Dict

from confluent_kafka import Producer

from util.requirements import require


class StreamProducer(object):
    def __init__(self, conf: Dict, debug: bool = False):
        require(conf.get('buffer.size'))
        require(conf.get('bootstrap.servers'))
        require(conf.get('topic'))
        self.debug = debug
        self.conf = conf
        self.topic = conf.get('topic')
        self.buffer_size = conf.get('buffer.size')
        del self.conf['topic']
        del self.conf['buffer.size']
        self.p = Producer(**self.conf)
        self.counter = 0

    def produce(self, key, msg) -> bool:
        try:
            self.p.produce(self.topic, key=key, value=json.dumps(msg).encode('utf-8'), callback=self._delivery_callback)
            self.counter = self.counter + 1
        except BufferError:
            sys.stderr.write('%% Local producer queue is full (%d messages awaiting delivery): try again\n' %
                             len(self.p))
            return False

        self.p.poll(0)

        if self.counter == self.conf.get('buffer.size'):
            self.p.flush()
            self.counter = 0
            if self.debug:
                sys.stderr.write('%% Waiting for %d deliveries\n' % len(self.p))

        return True

    def _delivery_callback(self, err, msg):
        if err:
            sys.stderr.write('%% Message failed delivery: %s\n' % err)
        else:
            if self.debug:
                sys.stderr.write('%% Message delivered to %s [%d] @ %d\n' %
                                 (msg.topic(), msg.partition(), msg.offset()))
