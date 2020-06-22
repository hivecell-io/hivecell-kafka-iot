//package com.rickerlyman.iot.camera.infrastructure.vision.tensorflow.local;
//
//import java.io.*;
//import java.util.*;
//
//import com.rickerlyman.iot.camera.Driver;
//import org.bytedeco.javacpp.*;
//
//import org.bytedeco.tensorrt.nvinfer.*;
//import org.bytedeco.tensorrt.nvparsers.*;
//
//import static org.bytedeco.cuda.global.cudart.*;
//import static org.bytedeco.tensorrt.global.nvinfer.*;
//import static org.bytedeco.tensorrt.global.nvparsers.createCaffeParser;
//import static org.bytedeco.tensorrt.global.nvparsers.shutdownProtobufLibrary;
//
//public class TensorNet {
//    private static Logger gLogger = new Logger();
//    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Driver.class);
//    // stuff we know about the network and the caffe input/output blobs
//    private static int BATCH_SIZE = 4;
//    private static int TIMING_ITERATIONS = 1000;
//
//    private static String INPUT_BLOB_NAME = "data";
//    private static String OUTPUT_BLOB_NAME = "prob";
//
//    private static Profiler gProfiler = new Profiler();
//
//    private IRuntime infer;
//    private ICudaEngine engine;
//
//    public TensorNet(String protoTxtPath, String modelPath, String[] outputBlobs, int maxBatchSize) {
//        logger.info("Building and running a GPU inference engine");
//
//        // parse the caffe model and the mean file
//        IHostMemory[] gieModelStream = caffeToGIEModel(protoTxtPath, modelPath, outputBlobs, maxBatchSize);
//
//        // create an engine
//        IRuntime infer = createInferRuntime(gLogger);
//        ICudaEngine engine = infer.deserializeCudaEngine(gieModelStream[0].data(), gieModelStream[0].size(), null);
//
//        logger.info("Bindings after deserializing:");
//        for (int bi = 0; bi < engine.getNbBindings(); bi++) {
//            if (engine.bindingIsInput(bi)) {
//                logger.info(String.format("Binding %d (%s): Input.\n", bi, engine.getBindingName(bi)));
//            } else {
//                logger.info(String.format("Binding %d (%s): Output.\n", bi, engine.getBindingName(bi)));
//            }
//        }
//
//        PointerPointer buffers = new PointerPointer(2);
//
//        // In order to bind the buffers, we need to know the names of the input and output tensors.
//        // note that indices are guaranteed to be less than ICudaEngine::getNbBindings()
//        int inputIndex = engine.getBindingIndex(INPUT_BLOB_NAME);
//        int outputIndex = engine.getBindingIndex(OUTPUT_BLOB_NAME);
//
//        // allocate GPU buffers
//        DimsCHW inputDims = new DimsCHW(engine.getBindingDimensions(inputIndex));
//        DimsCHW outputDims = new DimsCHW(engine.getBindingDimensions(outputIndex));
//        long inputSize = BATCH_SIZE * inputDims.c().get() * inputDims.h().get() * inputDims.w().get() * Float.SIZE / 8;
//        long outputSize = BATCH_SIZE * outputDims.c().get() * outputDims.h().get() * outputDims.w().get() * Float.SIZE / 8;
//
//        check(cudaMalloc(buffers.position(inputIndex), inputSize));
//        check(cudaMalloc(buffers.position(outputIndex), outputSize));
//    }
//
//    private static class Profiler extends IProfiler {
//        LinkedHashMap<String, Float> mProfile = new LinkedHashMap<String, Float>();
//
//        @Override
//        public void reportLayerTime(String layerName, float ms) {
//            Float time = mProfile.get(layerName);
//            mProfile.put(layerName, (time != null ? time : 0) + ms);
//        }
//
//        public void printLayerTimes() {
//            float totalTime = 0;
//            for (Map.Entry<String, Float> e : mProfile.entrySet()) {
//                logger.info(String.format("%-40.40s %4.3fms\n", e.getKey(), e.getValue() / TIMING_ITERATIONS));
//                totalTime += e.getValue();
//            }
//            logger.info(String.format("Time over all layers: %4.3f\n", totalTime / TIMING_ITERATIONS));
//        }
//
//    }
//
//    private static void caffeToGIEModel(String deployFile,     // name for caffe prototxt
//                                        String modelFile,      // name for model
//                                        String[] outputs,      // network outputs
//                                        int maxBatchSize,      // batch size - NB must be at least as large as the batch we want to run with)
//                                        IHostMemory[] gieModelStream) {
//        // create API root class - must span the lifetime of the engine usage
//        IHostMemory[] gieModelStream1 = {null};
//        IBuilder builder = createInferBuilder(gLogger);
//        INetworkDefinition network = builder.createNetwork();
//
//        // parse the caffe model to populate the network, then set the outputs
//        ICaffeParser parser = createCaffeParser();
//
//        boolean useFp16 = builder.platformHasFastFp16();
//
//        DataType modelDataType = useFp16 ? DataType.kHALF : DataType.kFLOAT; // create a 16-bit model if it's natively supported
//        //TODO: pass here full path to deploy and model files and do not use locateFile
//        IBlobNameToTensor blobNameToTensor =
//            parser.parse(locateFile(deployFile),  // caffe deploy file
//                locateFile(modelFile),            // caffe model file
//                network,                          // network definition that the parser will populate
//                modelDataType);
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
//    }
//
//    public IHostMemory[] caffeToGIEModel(String deployFile,     // name for caffe prototxt
//                                         String modelFile,      // name for model
//                                         String[] outputs,      // network outputs
//                                         int maxBatchSize       // batch size - NB must be at least as large as the batch we want to run with)
//    ) {
//        // create API root class - must span the lifetime of the engine usage
//        IHostMemory[] gieModelStream = {null};
//        IBuilder builder = createInferBuilder(gLogger);
//        INetworkDefinition network = builder.createNetwork();
//
//        // parse the caffe model to populate the network, then set the outputs
//        ICaffeParser parser = createCaffeParser();
//
//        boolean useFp16 = builder.platformHasFastFp16();
//
//        DataType modelDataType = useFp16 ? DataType.kHALF : DataType.kFLOAT; // create a 16-bit model if it's natively supported
//        IBlobNameToTensor blobNameToTensor =
//            parser.parse(deployFile,  // caffe deploy file
//                         modelFile,   // caffe model file
//                         network,     // network definition that the parser will populate
//                         modelDataType);
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
//    private static String locateFile(String input) {
//        String[] dirs = {"data/samples/googlenet/", "data/googlenet/"};
//        return locateFile(input, dirs);
//    }
//
//    private static String locateFile(String input, String[] directories) {
//        String file = "";
//        int MAX_DEPTH = 10;
//        boolean found = false;
//        for (String dir : directories) {
//            file = dir + input;
//            for (int i = 0; i < MAX_DEPTH && !found; i++) {
//                File checkFile = new File(file);
//                found = checkFile.exists();
//                if (found) break;
//                file = "../" + file;
//            }
//            if (found) break;
//            file = "";
//        }
//
//        if (file.isEmpty()) {
//            logger.error("Could not find a file due to it not existing in the data directory.");
//        }
//
//        return file;
//    }
//
//    public static void timeInference(ICudaEngine engine, int batchSize) {
//        // input and output buffer pointers that we pass to the engine - the engine requires exactly ICudaEngine::getNbBindings(),
//        // of these, but in this case we know that there is exactly one input and one output.
//
//        int nbBindings = engine.getNbBindings();
//        if (nbBindings != 2) {
//            throw new IllegalStateException(String.format("Expected 2 bindings, but was: %s", nbBindings));
//        }
//
//        PointerPointer buffers = new PointerPointer(2);
//
//        // In order to bind the buffers, we need to know the names of the input and output tensors.
//        // note that indices are guaranteed to be less than ICudaEngine::getNbBindings()
//        int inputIndex = engine.getBindingIndex(INPUT_BLOB_NAME);
//        int outputIndex = engine.getBindingIndex(OUTPUT_BLOB_NAME);
//
//        // allocate GPU buffers
//        DimsCHW inputDims = new DimsCHW(engine.getBindingDimensions(inputIndex));
//        DimsCHW outputDims = new DimsCHW(engine.getBindingDimensions(outputIndex));
//        long inputSize = batchSize * inputDims.c().get() * inputDims.h().get() * inputDims.w().get() * Float.SIZE / 8;
//        long outputSize = batchSize * outputDims.c().get() * outputDims.h().get() * outputDims.w().get() * Float.SIZE / 8;
//
//        check(cudaMalloc(buffers.position(inputIndex), inputSize));
//        check(cudaMalloc(buffers.position(outputIndex), outputSize));
//
//        IExecutionContext context = engine.createExecutionContext();
//        context.setProfiler(gProfiler);
//
//        // zero the input buffer
//        check(cudaMemset(buffers.position(inputIndex).get(), 0, inputSize));
//
//        for (int i = 0; i < TIMING_ITERATIONS; i++)
//            context.execute(batchSize, buffers.position(0));
//
//        // release the context and buffers
//        context.destroy();
//        check(cudaFree(buffers.position(inputIndex).get()));
//        check(cudaFree(buffers.position(outputIndex).get()));
//    }
//
//    public static void imageInference(ICudaEngine engine, int batchSize) {
//        if (engine.getNbBindings() != batchSize) {
//            throw new IllegalArgumentException("Batch size should be equal to number of bindings");
//        }
//        IExecutionContext context = engine.createExecutionContext();
//        context.setProfiler(gProfiler);
//        context.execute(batchSize, new PointerPointer(2).position(0));
//        context.destroy();
//    }
//
//    public DimsCHW getTensorDims(final String name) {
//        for (int b = 0; b < engine.getNbBindings(); b++) {
//            if (name.equals(engine.getBindingName(b))) {
//                return new DimsCHW(engine.getBindingDimensions(b));
//            }
//        }
//        return new DimsCHW(0, 0, 0);
//    }
//
//    public void destroy() {
//        engine.destroy();
//        infer.destroy();
//    }
//
//    public void loadNetwork(String protoTxtPath, String modelPath, String[] outputBlobs, int maxBatchSize) {
//        logger.info("Building and running a GPU inference engine");
//
//        // parse the caffe model and the mean file
//        IHostMemory[] gieModelStream = caffeToGIEModel(protoTxtPath, modelPath, outputBlobs, maxBatchSize);
//
//        // create an engine
//        IRuntime infer = createInferRuntime(gLogger);
//        ICudaEngine engine = infer.deserializeCudaEngine(gieModelStream[0].data(), gieModelStream[0].size(), null);
//
//        logger.info("Bindings after deserializing:");
//        for (int bi = 0; bi < engine.getNbBindings(); bi++) {
//            if (engine.bindingIsInput(bi)) {
//                logger.info(String.format("Binding %d (%s): Input.\n", bi, engine.getBindingName(bi)));
//            } else {
//                logger.info(String.format("Binding %d (%s): Output.\n", bi, engine.getBindingName(bi)));
//            }
//        }
//    }
//
//
//    //TODO: We can run this in docker container with CUDA instead of installing CUDA to Hivecell
//    public static void main(String[] args) {
//        logger.info("Building and running a GPU inference engine for GoogleNet, N=4...");
//
//        // parse the caffe model and the mean file
//        IHostMemory[] gieModelStream = {null};
//        caffeToGIEModel("googlenet.prototxt", "googlenet.caffemodel", new String[]{OUTPUT_BLOB_NAME}, BATCH_SIZE, gieModelStream);
//
//        // create an engine
//        IRuntime infer = createInferRuntime(gLogger);
//        ICudaEngine engine = infer.deserializeCudaEngine(gieModelStream[0].data(), gieModelStream[0].size(), null);
//
//        logger.info("Bindings after deserializing:");
//        for (int bi = 0; bi < engine.getNbBindings(); bi++) {
//            if (engine.bindingIsInput(bi)) {
//                logger.info(String.format("Binding %d (%s): Input.\n", bi, engine.getBindingName(bi)));
//            } else {
//                logger.info(String.format("Binding %d (%s): Output.\n", bi, engine.getBindingName(bi)));
//            }
//        }
//
//
//        //TODO: RUN INFERENCE WITH NULL DATA TO TIME NETWORK PERFORMANCE
//        timeInference(engine, BATCH_SIZE);
//
//        engine.destroy();
//        infer.destroy();
//
//        gProfiler.printLayerTimes();
//
//        logger.info("Done.");
//
//        System.exit(0);
//    }
//
//    public static void check(int status) {
//        if (status != 0) {
//            throw new IllegalStateException("Cuda failure: " + status);
//        }
//    }
//
//    // Logger for GIE info/warning/errors
//    static class Logger extends ILogger {
//        @Override
//        public void log(Severity severity, String msg) {
//            severity = severity.intern();
//
//            // suppress info-level messages
//            if (severity == Severity.kINFO) return;
//
//            switch (severity) {
//                case kINTERNAL_ERROR:
//                    logger.error("INTERNAL_ERROR: ");
//                    break;
//                case kERROR:
//                    logger.error("ERROR: ");
//                    break;
//                case kWARNING:
//                    logger.error("WARNING: ");
//                    break;
//                case kINFO:
//                    logger.error("INFO: ");
//                    break;
//                default:
//                    logger.error("UNKNOWN: ");
//                    break;
//            }
//            logger.error(msg);
//        }
//    }
//}
