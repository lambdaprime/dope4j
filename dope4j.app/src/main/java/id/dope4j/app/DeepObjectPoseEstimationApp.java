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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.impl.RecordNamingStrategyPatchModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Point3D;
import id.dope4j.DeepObjectPoseEstimationService;
import id.dope4j.DopeConstants;
import id.dope4j.decoders.ObjectsDecoder;
import id.dope4j.decoders.ObjectsDecoder.Inspector;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputPoses;
import id.matcv.camera.CameraInfo;
import id.xfunction.ResourceUtils;
import id.xfunction.cli.ArgumentParsingException;
import id.xfunction.cli.CommandOptions;
import id.xfunction.logging.XLogger;
import id.xfunction.nio.file.FilePredicates;
import id.xfunction.nio.file.XFiles;
import java.io.IOException;
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
public class DeepObjectPoseEstimationApp implements Inspector.Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepObjectPoseEstimationApp.class);
    private static final String CACHE_FOLDER_NAME = "_cache_dope4j";
    private CommandOptions commandOptions;
    private ObjectsDecoder objectsDecoder;
    private Optional<CacheFileMapper> cacheFileMapper = Optional.empty();

    static {
        OpenCV.loadLocally();
    }

    public DeepObjectPoseEstimationApp(CommandOptions commandOptions) {
        this.commandOptions = commandOptions;
    }

    private static void usage() throws IOException {
        new ResourceUtils().readResourceAsStream("README-dope4j.md").forEach(System.out::println);
    }

    public void run() throws Exception {
        XLogger.load("logging-dope4j.properties");
        var modelUrl = commandOptions.getRequiredOption("modelUrl");
        LOGGER.info("Model URL: {}", modelUrl);
        var imagePath = Paths.get(commandOptions.getRequiredOption("imagePath"));
        LOGGER.info("Image path: {}", imagePath);
        if (commandOptions.isOptionTrue("cache")) {
            cacheFileMapper =
                    commandOptions
                            .getOption("cacheFolder")
                            .map(Paths::get)
                            .or(() -> XFiles.TEMP_FOLDER.map(p -> p.resolve(CACHE_FOLDER_NAME)))
                            .or(() -> Optional.of(Paths.get(CACHE_FOLDER_NAME)))
                            .map(CacheFileMapper::new);
            LOGGER.info("Cache folder: {}", cacheFileMapper.get().getCacheHome());
        }
        var cameraInfoPath = commandOptions.getRequiredOption("cameraInfo");
        LOGGER.info("Reading camera info from: {}", cameraInfoPath);
        var objectSize = commandOptions.getRequiredOption("objectSize");
        LOGGER.info("Object cuboid size: {}", objectSize);
        objectsDecoder =
                new ObjectsDecoder(
                        commandOptions
                                .getOption("threshold")
                                .map(Double::parseDouble)
                                .orElse(DopeConstants.DEFAULT_PEAK_THRESHOLD),
                        newCuboid(objectSize),
                        readCameraInfo(Paths.get(cameraInfoPath)),
                        this);
        commandOptions
                .getOption("cameraInfo")
                .map(Paths::get)
                .ifPresent(
                        path -> {
                            LOGGER.info("Reading camera info from: {}", path);
                            var cameraInfo = objectsDecoder.cameraInfo = readCameraInfo(path);
                            LOGGER.info("Camera info: {}", cameraInfo);
                            objectsDecoder.cameraInfo = cameraInfo;
                        });
        if (!imagePath.toFile().exists())
            throw new RuntimeException("Path does not exist: " + imagePath);
        var imageFilesList = listImageFiles(imagePath);
        LOGGER.info("Found {} images to run inference on", imageFilesList.size());
        if (imageFilesList.isEmpty())
            throw new RuntimeException("No image files found in " + imagePath);
        try (var service =
                new DeepObjectPoseEstimationService<OutputPoses>(modelUrl, objectsDecoder)) {
            for (var imageFile : imageFilesList) {
                try {
                    var pose = processFromCache(imageFile);
                    if (pose.isEmpty()) {
                        pose = service.analyze(imageFile).stream().findFirst();
                    }
                    pose.ifPresent(System.out::println);
                } catch (Exception e) {
                    LOGGER.error("Failed to decode image " + imageFile + ": ", e);
                }
            }
        }
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

    private CameraInfo readCameraInfo(Path path) {
        try {
            return new YAMLMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .registerModule(new RecordNamingStrategyPatchModule())
                    .registerModule(new ParameterNamesModule())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .reader()
                    .readValue(path.toFile(), CameraInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return Files.walk(imagePath, depth).filter(FilePredicates.match(regexp)).toList();
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
}
