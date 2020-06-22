import time

import cv2
import numpy as np
import pycuda.autoinit
from inference.models.ssd import TrtSSD
from inference.models.ssd_classes import get_cls_dict

INPUT_HW = (300, 300)


class Detector(object):
    def __init__(self, model="ssd_mobilenet_v2_coco", conf_th=0.3):
        self.conf_th = conf_th
        self.cls_dict = get_cls_dict(model.split('_')[-1])
        self.trt_ssd = TrtSSD(model, INPUT_HW)

    def detect(self, im_bytes: bytes):
        """Continuously capture images from camera and do object detection.

        # Arguments
          image: image.
          trt_ssd: the TRT SSD object detector instance.
          conf_th: confidence/score threshold for object detection.
        """
        fps = 0.0
        tic = time.time()
        if im_bytes is not None:
            image = self.to_numpy_arr(im_bytes)
            boxes, confidence, cls = self.trt_ssd.detect(image, self.conf_th)
            toc = time.time()
            curr_fps = 1.0 / (toc - tic)
            fps = curr_fps if fps == 0.0 else (fps * 0.95 + curr_fps * 0.05)
            if boxes and confidence and cls:
                return {"fps": fps, "detects": self._draw_bboxes(boxes, confidence, cls)}
            else:
                return {"fps": fps, "detects": []}
        else:
            return None

    def _draw_bboxes(self, boxes, confs, clss):
        """Draw detected bounding boxes on the original image."""
        res = list()
        for bb, cf, cl in zip(boxes, confs, clss):
            cl = int(cl)
            cls_name = self.cls_dict.get(cl, 'CLS{}'.format(cl))
            txt = '{} {:.2f}'.format(cls_name, cf)
            x_min, y_min, x_max, y_max = bb[0], bb[1], bb[2], bb[3]
            res.append({"cls": cls_name, "conf": txt,
                        "pos": {"x_min": x_min, "y_min": y_min, "x_max": x_max, "y_max": y_max}})
        return res

    def to_numpy_arr(self, image):
        im_arr = np.frombuffer(image, dtype=np.uint8)
        return cv2.imdecode(im_arr, flags=cv2.IMREAD_COLOR)
