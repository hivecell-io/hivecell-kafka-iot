import json
import logging
import sys
from pprint import pformat
from typing import Dict, Callable, Iterable, Any

from confluent_kafka import Consumer, KafkaException
from lazy_streams import _LazyListStream
from util.requirements import require

from client.producer import StreamProducer


class StreamConsumer(object):
    logger = logging.getLogger('consumer')
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
    logger.addHandler(handler)

    def __init__(self, conf: Dict, debug: bool = False):
        require(conf.get('bootstrap.servers'))
        require(conf.get('topic'))
        require(conf.get('group.id'))
        require(conf.get('session.timeout.ms'))
        require(conf.get('auto.offset.reset'))
        require(conf.get('ack'))
        self.debug = debug
        self.conf = conf
        self.topics = [str(conf.get('topic'))]

        if conf.get('ack') > 3:
            self.ack = 3
        elif conf.get('ack') < 0:
            self.ack = -1
        else:
            self.ack = conf.get('ack')

        del self.conf['topic']
        del self.conf['ack']

        if 'stats_cb' in conf.keys():
            conf['stats_cb'] = self._stats_cb
            require(conf.get('statistics.interval.ms'))
            conf['statistics.interval.ms'] = conf.get('statistics.interval.ms')

        self.c = Consumer(self.conf)

    def _stats_cb(self, stats_json_str):
        stats_json = json.loads(stats_json_str)
        print('\nKAFKA Stats: {}\n'.format(pformat(stats_json)))

    def _print_assignment(self, consumer, partitions):
        print('Assignment:', partitions)

    def consume_bypass(self, producer: StreamProducer):
        self.c.subscribe(self.topics, on_assign=self._print_assignment)
        try:
            msg_count = 0
            while True:
                msg = self.c.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    raise KafkaException(msg.error())
                else:
                    sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                     (msg.topic(), msg.partition(), msg.offset(),
                                      str(msg.key())))

                    producer.produce(msg.key(), msg.value())
                    msg_count += 1

                    if msg_count % self.ack == 0:
                        self.c.commit(asynchronous=True)

        except KeyboardInterrupt:
            sys.stderr.write('%% Aborted by user\n')

        finally:
            self.c.close()


    def consume(self, chain: Callable[[Iterable[Any]], _LazyListStream], produce: Callable[[Any, Any], bool]):
        self.c.subscribe(self.topics, on_assign=self._print_assignment)

        try:
            msg_count = 0
            while True:
                msg = self.c.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    raise KafkaException(msg.error())
                else:
                    if self.debug:
                        sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                         (msg.topic(), msg.partition(), msg.offset(),
                                          str(msg.key())))
                    key = msg.key()
                    for m in chain(msg.value()).to_list():
                        produce(key, m)
                        msg_count += 1
                        if msg_count % self.ack == 0:
                            self.c.commit(asynchronous=True)

        except KeyboardInterrupt:
            sys.stderr.write('%% Aborted by user\n')

        finally:
            self.c.close()
