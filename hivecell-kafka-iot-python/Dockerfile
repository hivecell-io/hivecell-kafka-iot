FROM w251/tensorflow:dev-tx2-4.3_b132-tf1

RUN  apt-get update \
  && apt-get install -y --no-install-recommends git \
                                                wget \
                                                zip \
                                                build-essential \
                                                make \
                                                cmake \
                                                cmake-curses-gui \
                                                g++ \
                                                pkg-config \
                                                curl \
                                                libfreetype6-dev \
                                                libcanberra-gtk-module \
                                                libcanberra-gtk3-module \
                                                python3-dev \
                                                python3-testresources \
                                                python3-pip \
                                                python3-opencv

RUN  wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/graphsurgeon-tf_6.0.1-1+cuda10.0_arm64.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/OpenCV-4.1.1-2-gd5a58aa75-aarch64-dev.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/OpenCV-4.1.1-2-gd5a58aa75-aarch64-libs.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/OpenCV-4.1.1-2-gd5a58aa75-aarch64-licenses.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/OpenCV-4.1.1-2-gd5a58aa75-aarch64-python.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/OpenCV-4.1.1-2-gd5a58aa75-aarch64-samples.deb \
  && wget --no-verbose --no-check-certificate http://169.44.201.108:7002/jetpacks/4.3/uff-converter-tf_6.0.1-1+cuda10.0_arm64.deb \
  && dpkg -i graphsurgeon-tf_6.0.1-1+cuda10.0_arm64.deb \
  && dpkg -i uff-converter-tf_6.0.1-1+cuda10.0_arm64.deb \
  && dpkg --force-all -i OpenCV-4.1.1-2-gd5a58aa75-aarch64-libs.deb \
  && dpkg --force-all -i OpenCV-4.1.1-2-gd5a58aa75-aarch64-licenses.deb \
  && dpkg --force-all -i OpenCV-4.1.1-2-gd5a58aa75-aarch64-dev.deb \
  && dpkg --force-all -i OpenCV-4.1.1-2-gd5a58aa75-aarch64-python.deb \
  && dpkg --force-all -i OpenCV-4.1.1-2-gd5a58aa75-aarch64-samples.deb \
  && apt-get install -f -y \
  && apt install -y ./graphsurgeon-tf_6.0.1-1+cuda10.0_arm64.deb \
  && apt install -y ./uff-converter-tf_6.0.1-1+cuda10.0_arm64.deb \
  && apt install -y ./OpenCV-4.1.1-2-gd5a58aa75-aarch64-libs.deb \
  && apt install -y ./OpenCV-4.1.1-2-gd5a58aa75-aarch64-licenses.deb \
  && apt install -y ./OpenCV-4.1.1-2-gd5a58aa75-aarch64-python.deb \
  && apt install -y ./OpenCV-4.1.1-2-gd5a58aa75-aarch64-dev.deb \
  && apt install -y ./OpenCV-4.1.1-2-gd5a58aa75-aarch64-samples.deb \
  && apt-get update

RUN sudo apt-get install -y libssl-dev zlib1g-dev gcc g++ make \
  && git clone https://github.com/edenhill/librdkafka \
  && cd librdkafka \
  && ./configure --prefix=/usr \
  && make \
  && sudo make install \
  && cd ../ \
  && pip install confluent-kafka \
  && pip install lazy-streams \

RUN mkdir project \
  && cd project \
  && git clone --recursive https://github.com/jkjung-avt/tf_trt_models \
  && cd tf_trt_models \
  && ./install.sh \
  && cd ../ \
  && git clone https://github.com/jkjung-avt/tensorrt_demos.git \
  && cd tensorrt_demos/ssd/ \
  && ./install.sh
#  && python3 build_engine.py ssd_mobilenet_v2_coco

RUN  rm -rf /tmp/*.deb  /var/cache/apt /var/lib/apt/lists/*

COPY trt_ssd_async_m.py trt_ssd_m.py /tmp/project/tensorrt_demos/
COPY tmp_v2_coco.uff tmp_v2_coco.pbtxt TRT_ssd_mobilenet_v2_coco.bin /tmp/project/tensorrt_demos/ssd/

RUN mkdir stream-inference \
    && cd stream-inference \
    && git clone --recursive https://github.com/hivecell-io/hivecell-kafka-iot.git \
    && cd hivecell-kafka-iot/hivecell-kafka-iot-python \
    && mkdir ssd
    && cp ../project/tensorrt_demos/ssd/libflattenconcat.so.5 ssd/
    && cp ../project/tensorrt_demos/ssd/libflattenconcat.so.6 ssd/
    && cp ../project/tensorrt_demos/ssd/TRT_ssd_mobilenet_v2_coco.bin ssd/
    && cp ../project/tensorrt_demos/ssd/ssd_mobilenet_v2_coco.pb ssd/

RUN  apt-get update \
  && apt-get install -y --no-install-recommends nano

WORKDIR /tmp
