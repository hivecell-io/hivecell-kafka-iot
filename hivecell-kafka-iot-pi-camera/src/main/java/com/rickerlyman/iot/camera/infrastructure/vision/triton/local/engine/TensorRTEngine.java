//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine;
//
//import org.bytedeco.javacpp.BytePointer;
//import org.bytedeco.javacpp.Pointer;
//import org.bytedeco.javacpp.PointerPointer;
//import org.bytedeco.tensorrt.global.nvinfer;
//import org.bytedeco.tensorrt.nvinfer.*;
//import org.bytedeco.tensorrt.nvparsers.IBlobNameToTensor;
//import org.bytedeco.tensorrt.nvparsers.ICaffeParser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.nio.ByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//import static com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine.TensorRT.engineInfo;
//import static com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local.engine.TensorRT.nullPointer;
//import static org.bytedeco.cuda.global.cudart.*;
//import static org.bytedeco.tensorrt.global.nvinfer.createInferBuilder;
//import static org.bytedeco.tensorrt.global.nvinfer.createInferRuntime;
//import static org.bytedeco.tensorrt.global.nvparsers.createCaffeParser;
//import static org.bytedeco.tensorrt.global.nvparsers.shutdownProtobufLibrary;
//
//
//public class TensorRTEngine {
//
//    private static Logger logger = LoggerFactory.getLogger(TensorRTEngine.class);
//    private static String OUTPUT_BLOB_NAME = "prob";
//
//    private ICudaEngine engine;
//    private IExecutionContext exeContext;
//    private Profiler profiler;
//    //Data flow
//    //    Input CPU Buffer =======>>>  Input GPU Buffer
//    //                                          v
//    //                                          v
//    //                                        Infer
//    //                                          v
//    //                                          v
//    //   Output CPU Buffer <<<=======  Output GPU Buffer
//
//    private int maxBatchSize;
//    private int numBindings;
//
//    //List of the pointers of GPU.
//    private PointerPointer<Pointer> mBindings;
//
//    private Map<String, Pointer> inGPUBindings;
//    private Map<String, Pointer> outGPUBindings;
//
//    public TensorRTEngine(String path2Engine) {
//        engine = loadEngine(path2Engine);
//        profiler = new Profiler();
//        engineInfo(this.engine);
//
//        exeContext = engine.createExecutionContext();
//
//        this.maxBatchSize = this.engine.getMaxBatchSize();
//        this.numBindings = this.engine.getNbBindings();
//        this.mBindings = new PointerPointer<>(numBindings);
//        //-------------------------------------------------//
//        inGPUBindings = new HashMap<>();
//        outGPUBindings = new HashMap<>();
//
//        initBuffer();
//    }
//
//    public TensorRTEngine(String protoTxtPath, String modelPath) {
//        //todo: bath size and buffer
//        engine = loadEngine(protoTxtPath, modelPath, new String[]{OUTPUT_BLOB_NAME}, 1);
//        profiler = new Profiler();
//        engineInfo(this.engine);
//
//        exeContext = engine.createExecutionContext();
//
//        this.maxBatchSize = this.engine.getMaxBatchSize();
//        this.numBindings = this.engine.getNbBindings();
//        this.mBindings = new PointerPointer<>(numBindings);
//        //-------------------------------------------------//
//        inGPUBindings = new HashMap<>();
//        outGPUBindings = new HashMap<>();
//
//        initBuffer();
//    }
//
//    private void initBuffer() {
//        for (int i = 0; i < numBindings; i++) {
//            String bindingName = engine.getBindingName(i);
//            System.out.println("bindingName: -------------------------------------------> "+bindingName);
//
//            long size = volume(engine.getBindingDimensions(i));
//            size *= sizeOf(engine.getBindingDataType(i));
//            size *= this.maxBatchSize;
//
//            Pointer pointer = gpuMalloc(size);
//
//            if (engine.bindingIsInput(i)) {
//                inGPUBindings.put(bindingName, pointer);
//            } else {
//                outGPUBindings.put(bindingName, pointer);
//            }
//
//            mBindings.put(i, pointer);
//        }
//    }
//
//
//    private ICudaEngine loadEngine(String path2Engine) {
//        ByteBuffer buffer = readFileAll(path2Engine);
//        Pointer pointer = new BytePointer(buffer);
//        IRuntime iRuntime = createInferRuntime(TensorRT.logger);
//        ICudaEngine engine = iRuntime.deserializeCudaEngine(pointer, buffer.capacity());
//        iRuntime.destroy();
//        return engine;
//    }
//
//    private ICudaEngine loadEngine(String protoTxtPath, String modelPath, String[] outputBlobs, int maxBatchSize) {
//        IHostMemory[] gieModelStream = caffeToGIEModel(protoTxtPath, modelPath, outputBlobs, maxBatchSize);
//        IRuntime iRuntime = createInferRuntime(TensorRT.logger);
//        ICudaEngine engine = iRuntime.deserializeCudaEngine(gieModelStream[0].data(), gieModelStream[0].size());
//        iRuntime.destroy();
//        return engine;
//    }
//
//    public IHostMemory[] caffeToGIEModel(String deployFile, String modelFile, String[] outputs, int maxBatchSize) {
//        // create API root class - must span the lifetime of the engine usage
//        IHostMemory[] gieModelStream = {null};
//        IBuilder builder = createInferBuilder(TensorRT.logger);
//        INetworkDefinition network = builder.createNetwork();
//
//        // parse the caffe model to populate the network, then set the outputs
//        ICaffeParser parser = createCaffeParser();
//
//        boolean useFp16 = builder.platformHasFastFp16();
//
//        nvinfer.DataType modelDataType = useFp16 ? nvinfer.DataType.kHALF : nvinfer.DataType.kFLOAT; // create a 16-bit model if it's natively supported
//        // deployFile - caffe deploy file
//        // modelFile - caffe model file
//        // network - network definition that the parser will populate
//        IBlobNameToTensor blobNameToTensor = parser.parse(deployFile, modelFile, network, modelDataType);
//
//        if (Objects.isNull(blobNameToTensor)) {
//            throw new IllegalStateException("blobNameToTensor can't be null");
//        }
//
//        // the caffe file has no notion of outputs, so we need to manually say which tensors the engine should generate
//        for (String s : outputs)
//            network.markOutput(blobNameToTensor.find(s));
//
//        // Build the engine
//        builder.setMaxBatchSize(maxBatchSize);
//        builder.setMaxWorkspaceSize(16 << 20);
//
//        // set up the network for paired-fp16 format if available
//        if (useFp16)
//            builder.setHalf2Mode(true);
//
//        ICudaEngine engine = builder.buildCudaEngine(network);
//
//        if (Objects.isNull(engine)) {
//            throw new IllegalStateException("engine can't be null");
//        }
//        // we don't need the network any more, and we can destroy the parser
//        network.destroy();
//        parser.destroy();
//
//        // serialize the engine, then close everything down
//        gieModelStream[0] = engine.serialize();
//        engine.destroy();
//        builder.destroy();
//        shutdownProtobufLibrary();
//
//        return gieModelStream;
//    }
//
//
//    public Map<String, Pointer> infer(Map<String, Pointer> inputs, int batchSize) {
//        copyHostToDevice(inputs);
//        this.exeContext.setProfiler(profiler);
//        this.exeContext.execute(batchSize, mBindings.position(0));
//        return copyDeviceToHost();
//    }
//
//
//    private void copyHostToDevice(Map<String, Pointer> inputBindings) {
//        for (Map.Entry<String, Pointer> binding : inputBindings.entrySet()) {
//            String bindingName = binding.getKey();
//            Pointer srcPointer = binding.getValue();
//            Pointer destPointer = inGPUBindings.get(bindingName);
//
//            long size = destPointer.capacity();
//
//            if (srcPointer.sizeof() * srcPointer.capacity() != destPointer.sizeof() * destPointer.capacity()) {
//                logger.warn("size of src Pointer != size of dest Pointer" +
//                    " expect " + destPointer.capacity() +
//                    " actual " + srcPointer.capacity() * srcPointer.sizeof());
//            }
//
//            cudaMemcpy(destPointer, srcPointer, size, cudaMemcpyHostToDevice);
//        }
//
//    }
//
//    private Map<String, Pointer> copyDeviceToHost() {
//        HashMap<String, Pointer> output = new HashMap<>();
//        for (Map.Entry<String, Pointer> entry : outGPUBindings.entrySet()) {
//            String bindingName = entry.getKey();
//            Pointer srcPointer = entry.getValue();
//            Pointer destPointer = new BytePointer(srcPointer.capacity());
//
//            if (srcPointer.sizeof() * srcPointer.capacity() != destPointer.sizeof() * destPointer.capacity()) {
//                logger.warn("size of src Pointer != size of dest Pointer" +
//                    " expect " + destPointer.capacity() +
//                    " actual " + srcPointer.capacity() * srcPointer.sizeof());
//            }
//
//
//            cudaMemcpy(destPointer, srcPointer, destPointer.capacity(), cudaMemcpyDeviceToHost);
//            output.put(bindingName, destPointer);
//        }
//        return output;
//    }
//
//
//    protected long volume(Dims dims) {
//        long acc = 1;
//        for (int i = 0; i < dims.nbDims(); i++) {
//            acc = acc * dims.d(i);
//        }
//        return acc;
//    }
//
//
//    public int getMaxBatchSize() {
//        return maxBatchSize;
//    }
//
//
//    protected int sizeOf(nvinfer.DataType dataType) {
//        switch (dataType) {
//            case kINT8:
//                return 1;
//            case kHALF:
//                return 2;
//            case kINT32:
//            case kFLOAT:
//                return 4;
//            default: {
//                System.err.print("Invalid DataType");
//                return 0;
//            }
//        }
//    }
//
//    private Pointer gpuMalloc(long size) {
//        Pointer p = new BytePointer(size);
//        if (cudaMalloc(p, size) == cudaSuccess) {
//            logger.info("Allocate " + size + " bytes on GPU");
//            return p;
//        } else {
//            logger.error("Unable to allocate memory on GPU");
//            return nullPointer;
//        }
//    }
//
//    private void gpuFree(Pointer pointer) {
//        if (cudaFree(pointer) != cudaSuccess) {
//            logger.info("Unable to free the memory on GPU");
//        }
//    }
//
//    public void shutdown() {
//        this.exeContext.destroy();
//        this.engine.destroy();
//
//        for (Map.Entry<String, Pointer> binding : inGPUBindings.entrySet()) {
//            gpuFree(binding.getValue());
//        }
//    }
//
//    private ByteBuffer readFileAll(String path2File) {
//        try {
//            RandomAccessFile accessFile = new RandomAccessFile(path2File, "r");
//            FileChannel channel = accessFile.getChannel();
//            int size = (int) channel.size();
//            ByteBuffer buffer = ByteBuffer.allocate(size);
//            channel.read(buffer);
//            buffer.rewind();
//            return buffer;
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e.toString());
//        } catch (IOException e) {
//            throw new RuntimeException(e.toString());
//        }
//    }
//}
