/*
 * Copyright 2022 dope4j project
 * 
 * Website: https://github.com/lambdaprime/dope4j
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.dope4j.app;

import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Point3D;
import id.dope4j.DeepObjectPoseEstimationService;
import id.dope4j.DopeConstants;
import id.dope4j.decoders.ObjectsDecoder;
import id.dope4j.decoders.ObjectsDecoder.Inspector;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputPoses;
import id.dope4j.jackson.JsonUtils;
import id.opentelemetry.exporters.CsvMetricExporter;
import id.opentelemetry.exporters.ElasticSearchMetricExporter;
import id.xfunction.ResourceUtils;
import id.xfunction.cli.ArgumentParsingException;
import id.xfunction.cli.CommandOptions;
import id.xfunction.function.LazyInitializer;
import id.xfunction.logging.XLogger;
import id.xfunction.nio.file.FilePredicates;
import id.xfunction.nio.file.XFiles;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements access to dope4j functionality from command-line.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DeepObjectPoseEstimationApp implements Inspector.Builder, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepObjectPoseEstimationApp.class);
    private static final String CACHE_FOLDER_NAME = "_cache_dope4j";
    private static final JsonUtils jsonUtils = new JsonUtils();
    private CommandOptions commandOptions;
    private ObjectsDecoder objectsDecoder;
    private Optional<CacheFileMapper> cacheFileMapper = Optional.empty();
    private PrintStream out;
    private Optional<SdkMeterProvider> sdkMeterProvider = Optional.empty();

    static {
        OpenCV.loadLocally();
    }

    public DeepObjectPoseEstimationApp(CommandOptions commandOptions) {
        this(commandOptions, System.out);
    }

    public DeepObjectPoseEstimationApp(CommandOptions commandOptions, PrintStream out) {
        this.commandOptions = commandOptions;
        this.out = out;
    }

    private static void usage() throws IOException {
        new ResourceUtils().readResourceAsStream("README-dope4j.md").forEach(System.out::println);
    }

    public void run() throws Exception {
        switch (commandOptions.getRequiredOption("action")) {
            case "runInference" -> runInference();
            case "showResults" -> showResults();
            default -> throw new ArgumentParsingException(
                    "Unknown action: " + commandOptions.getRequiredOption("action"));
        }
        sdkMeterProvider.ifPresent(SdkMeterProvider::forceFlush);
    }

    private void showResults() throws IOException {
        var results =
                jsonUtils.readDope4jResults(
                        Paths.get(commandOptions.getRequiredOption("resultsJson")));
        var imagesRoot =
                commandOptions.getOption("imagesRoot").map(Paths::get).orElse(Paths.get(""));
        commandOptions.addOption("showProjectedCuboids2D", true);
        for (var result : results) {
            try (var inspector =
                    build(new InputImage(imagesRoot.resolve(result.imagePath().orElseThrow())))) {
                inspector.inspectPoses(result.detectedPoses());
            }
        }
    }

    public void runInference() throws Exception {
        if (commandOptions.isOptionTrue("debug")) XLogger.load("logging-dope4j-debug.properties");
        var imagePath = Paths.get(commandOptions.getRequiredOption("imagePath"));
        if (!imagePath.toFile().exists())
            throw new RuntimeException("Path does not exist: " + imagePath);
        LOGGER.info("Image path: {}", imagePath.toAbsolutePath());
        if (commandOptions.isOptionTrue("cache")) {
            cacheFileMapper =
                    commandOptions
                            .getOption("cacheFolder")
                            .map(Paths::get)
                            .or(() -> XFiles.TEMP_FOLDER.map(p -> p.resolve(CACHE_FOLDER_NAME)))
                            .or(() -> Optional.of(Paths.get(CACHE_FOLDER_NAME).toAbsolutePath()))
                            .map(cacheFolder -> new CacheFileMapper(imagePath, cacheFolder));
            LOGGER.info("Cache folder: {}", cacheFileMapper.get().getCacheHome());
        }
        var cameraInfoPath = commandOptions.getRequiredOption("cameraInfo");
        LOGGER.info("Reading camera info from: {}", cameraInfoPath);
        var objectSize = commandOptions.getRequiredOption("objectSize");
        LOGGER.info("Object cuboid size: {}", objectSize);
        commandOptions
                .getOption("exportMetricsToElastic")
                .map(URI::create)
                .ifPresent(
                        uri -> {
                            LOGGER.info("Emitting metrics to ElasticSearch");
                            var exporter = new ElasticSearchMetricExporter(uri, true);
                            configureMetrics(exporter);
                        });
        commandOptions
                .getOption("exportMetricsToCsv")
                .map(Paths::get)
                .ifPresent(
                        path -> {
                            LOGGER.info("Emitting metrics to CSV files in {}", path);
                            try {
                                configureMetrics(new CsvMetricExporter(path));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
        objectsDecoder =
                new ObjectsDecoder(
                        commandOptions
                                .getOption("threshold")
                                .map(Double::parseDouble)
                                .orElse(DopeConstants.DEFAULT_PEAK_THRESHOLD),
                        newCuboid(objectSize),
                        jsonUtils.readCameraInfo(Paths.get(cameraInfoPath)),
                        this);
        commandOptions
                .getOption("cameraInfo")
                .map(Paths::get)
                .ifPresent(
                        path -> {
                            LOGGER.info("Reading camera info from: {}", path);
                            var cameraInfo =
                                    objectsDecoder.cameraInfo = jsonUtils.readCameraInfo(path);
                            LOGGER.info("Camera info: {}", cameraInfo);
                            objectsDecoder.cameraInfo = cameraInfo;
                        });
        var imageFilesList = listImageFiles(imagePath);
        LOGGER.info("Found {} images to run inference on", imageFilesList.size());
        if (imageFilesList.isEmpty())
            throw new RuntimeException("No image files found in " + imagePath);
        var serviceGetter =
                new LazyInitializer<DeepObjectPoseEstimationService<OutputPoses>>(
                        () -> {
                            var modelUrl = commandOptions.getRequiredOption("modelUrl");
                            LOGGER.info("Model URL: {}", modelUrl);
                            return new DeepObjectPoseEstimationService<OutputPoses>(
                                    modelUrl, objectsDecoder);
                        });
        try {
            for (var imageFile : imageFilesList) {
                try {
                    if (processFromCache(imageFile).isPresent()) continue;
                    var service = serviceGetter.get();
                    service.analyze(imageFile).stream().findFirst();
                } catch (Exception e) {
                    LOGGER.error("Failed to decode image " + imageFile + ": ", e);
                }
            }
        } finally {
            serviceGetter.ifInitialized(AutoCloseable::close);
        }
    }

    private void configureMetrics(MetricExporter exporter) {
        if (sdkMeterProvider.isPresent()) {
            LOGGER.warn("Metrics already configured, not configuring them second time");
            return;
        }
        var metricReader =
                PeriodicMetricReader.builder(exporter).setInterval(Duration.ofSeconds(3)).build();
        var provider = SdkMeterProvider.builder().registerMetricReader(metricReader).build();
        OpenTelemetrySdk.builder().setMeterProvider(provider).buildAndRegisterGlobal();
        sdkMeterProvider = Optional.of(provider);
    }

    private Cuboid3D newCuboid(String objectSize) {
        var vals =
                Pattern.compile(",")
                        .splitAsStream(objectSize)
                        .map(String::trim)
                        .mapToDouble(Double::parseDouble)
                        .toArray();
        if (vals.length != 3)
            throw new ArgumentParsingException("Could not parse objectSize value: " + objectSize);
        return new Cuboid3D(new Point3D(), vals[0], vals[1], vals[2]);
    }

    private Optional<OutputPoses> processFromCache(Path imageFile) throws IOException {
        if (cacheFileMapper.isEmpty()) return Optional.empty();
        var tensorFile = cacheFileMapper.get().getTensorFile(imageFile);
        if (!tensorFile.toFile().exists()) return Optional.empty();
        LOGGER.debug(
                "Image data found in cache, do not run inference and use it instead: image {}",
                imageFile);
        var tensor =
                NDArray.decode(
                        Engine.getInstance().newBaseManager(), Files.readAllBytes(tensorFile));
        return objectsDecoder.decode(new InputImage(imageFile), tensor);
    }

    private List<Path> listImageFiles(Path imagePath) throws IOException {
        var depth = commandOptions.isOptionTrue("recursiveScan") ? Integer.MAX_VALUE : 1;
        var regexp = commandOptions.getOption("imageFileRegexp").orElse(".*\\.(png|jpg)");
        return Files.walk(imagePath, depth).filter(FilePredicates.match(regexp)).sorted().toList();
    }

    public static void main(String[] args) throws Exception {
        var code = 1;
        if (args.length < 1) {
            usage();
        } else {
            Optional<Instant> startAtOpt = Optional.empty();
            try {
                var options = CommandOptions.collectOptions(args);
                if (options.isOptionTrue("totalRunTime")) startAtOpt = Optional.of(Instant.now());
                new DeepObjectPoseEstimationApp(options).run();
                code = 0;
            } catch (ArgumentParsingException e) {
                System.err.println(e.getMessage());
                System.err.println(
                        "Run command without arguments to see 'Usage' for more information.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            startAtOpt.ifPresent(
                    startAt ->
                            LOGGER.debug(
                                    "Total execution time: {}",
                                    Duration.between(startAt, Instant.now())));
        }
        System.exit(code);
    }

    @Override
    public Inspector build(InputImage inputImage) {
        Mat mat = (Mat) inputImage.image().getWrappedImage();
        return new Dope4jInspector(
                out,
                mat,
                inputImage,
                cacheFileMapper,
                commandOptions.isOptionTrue("showVerticesBeliefs"),
                commandOptions.isOptionTrue("showCenterPointBeliefs"),
                commandOptions.isOptionTrue("showAffinityFields"),
                commandOptions.isOptionTrue("showMatchedVertices"),
                commandOptions.isOptionTrue("showCuboids2D"),
                commandOptions.isOptionTrue("showProjectedCuboids2D"));
    }

    @Override
    public void close() {
        LOGGER.info("Closing the application");
        sdkMeterProvider.ifPresent(SdkMeterProvider::close);
    }
}
