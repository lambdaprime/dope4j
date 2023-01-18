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
import id.dope4j.DeepObjectPoseEstimationService;
import id.dope4j.DopeConstants;
import id.dope4j.decoders.ObjectsDecoder;
import id.dope4j.decoders.ObjectsDecoder.Inspector;
import id.dope4j.decoders.SaveStateDecoder;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.impl.Utils;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputKeypoints;
import id.dope4j.io.OutputObjects;
import id.dope4j.io.OutputTensor;
import id.matcv.RgbColors;
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
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
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
    private CacheFileMapper mapper;
    private CommandOptions commandOptions;
    private SaveStateDecoder saveState;
    private ObjectsDecoder objectsDecoder;
    private boolean isCacheEnabled;

    static {
        OpenCV.loadLocally();
    }

    public DeepObjectPoseEstimationApp(CommandOptions commandOptions) {
        this.commandOptions = commandOptions;
    }

    private static void usage() throws IOException {
        new ResourceUtils().readResourceAsStream("README-dope4j.md").forEach(System.out::println);
    }

    public Optional<Void> process(InputImage inputImage, NDArray outputTensor) {
        objectsDecoder.decode(inputImage, outputTensor);
        return Optional.empty();
    }

    public void run() throws Exception {
        XLogger.load("logging-dope4j.properties");
        var modelUrl = commandOptions.getRequiredOption("modelUrl");
        LOGGER.info("Model URL: {}", modelUrl);
        var imagePath = Paths.get(commandOptions.getRequiredOption("imagePath"));
        LOGGER.info("Image path: {}", imagePath);
        isCacheEnabled = commandOptions.isOptionTrue("cache");
        var cacheFolder =
                commandOptions
                        .getOption("cacheFolder")
                        .map(Paths::get)
                        .or(() -> XFiles.TEMP_FOLDER.map(p -> p.resolve(CACHE_FOLDER_NAME)))
                        .orElse(Paths.get(CACHE_FOLDER_NAME));
        LOGGER.info("Cache folder: {}", cacheFolder);
        mapper = new CacheFileMapper(cacheFolder);
        saveState = new SaveStateDecoder(mapper);
        var cameraInfoPath = commandOptions.getRequiredOption("cameraInfo");
        LOGGER.info("Reading camera info from: {}", cameraInfoPath);
        objectsDecoder =
                new ObjectsDecoder(
                        commandOptions
                                .getOption("threshold")
                                .map(Double::parseDouble)
                                .orElse(DopeConstants.DEFAULT_PEAK_THRESHOLD),
                        readCameraInfo(Paths.get(cameraInfoPath)),
                        this);
        if (!imagePath.toFile().exists())
            throw new RuntimeException("Path does not exist: " + imagePath);
        var imageFilesList = listImageFiles(imagePath);
        LOGGER.info("Found {} images to run inference on", imageFilesList.size());
        if (imageFilesList.isEmpty())
            throw new RuntimeException("No image files found in " + imagePath);
        try (var service = new DeepObjectPoseEstimationService<Void>(modelUrl, this::process)) {
            for (var imageFile : imageFilesList) {
                try {
                    if (isCacheEnabled && processFromCache(imageFile)) continue;
                    service.analyze(imageFile);
                } catch (Exception e) {
                    LOGGER.error("Failed to decode image " + imageFile + ": ", e);
                }
            }
        }
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

    private boolean processFromCache(Path imageFile) throws IOException {
        var tensorFile = mapper.getTensorFile(imageFile);
        if (!tensorFile.toFile().exists()) return false;
        LOGGER.debug(
                "Image data found in cache, do not run inference and use it instead: image {}",
                imageFile);
        var tensor =
                NDArray.decode(
                        Engine.getInstance().newBaseManager(), Files.readAllBytes(tensorFile));
        process(new InputImage(imageFile), tensor);
        return true;
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
            var options = CommandOptions.collectOptions(args);
            var startAt = Instant.now();
            try {
                new DeepObjectPoseEstimationApp(options).run();
                code = 0;
            } catch (ArgumentParsingException e) {
                System.err.println(e.getMessage());
                usage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (options.isOptionTrue("totalRunTime"))
                LOGGER.debug("Total execution time: {}", Duration.between(startAt, Instant.now()));
        }
        System.exit(code);
    }

    @Override
    public Inspector build(InputImage inputImage) {
        Mat mat = (Mat) inputImage.image().getWrappedImage();
        return new Inspector() {
            private boolean showImage;

            @Override
            public void inspectTensor(OutputTensor outputTensor) {
                inputImage
                        .path()
                        .ifPresent(
                                imageFile -> {
                                    if (!isCacheEnabled) return;
                                    var tensorFile = mapper.getTensorFile(imageFile);
                                    if (tensorFile.toFile().exists()) return;
                                    LOGGER.debug(
                                            "Adding image data for {} into the cache", imageFile);
                                    saveState.decode(inputImage, outputTensor.tensor());
                                });

                if (commandOptions.isOptionTrue("showAffinityFields")) {
                    Utils.drawAffinityFields(mat, outputTensor.affinities());
                    showImage = true;
                }
            }

            @Override
            public void inspectOjects(OutputObjects objects) {
                if (commandOptions.isOptionTrue("showMatchedVertices")) {
                    objects.objects()
                            .forEach(
                                    bb -> {
                                        var centerPoint =
                                                new org.opencv.core.Point(
                                                        bb.getCenter().getX()
                                                                * DopeConstants.SCALE_FACTOR,
                                                        bb.getCenter().getY()
                                                                * DopeConstants.SCALE_FACTOR);
                                        bb.vertices()
                                                .forEach(
                                                        vertex -> {
                                                            var v =
                                                                    new org.opencv.core.Point(
                                                                            vertex.getX()
                                                                                    * DopeConstants
                                                                                            .SCALE_FACTOR,
                                                                            vertex.getY()
                                                                                    * DopeConstants
                                                                                            .SCALE_FACTOR);
                                                            Imgproc.line(
                                                                    mat,
                                                                    centerPoint,
                                                                    v,
                                                                    RgbColors.GREEN);
                                                        });
                                    });
                    showImage = true;
                }
            }

            @Override
            public void inspectKeypoints(OutputKeypoints keypoints) {
                if (commandOptions.isOptionTrue("showVerticesBeliefs")) {
                    keypoints.vertices().forEach(l -> Utils.drawKeypoints(mat, l));
                    showImage = true;
                }
                if (commandOptions.isOptionTrue("showCenterPointBeliefs")) {
                    Utils.drawKeypoints(mat, keypoints.centerPoints());
                    showImage = true;
                }
            }

            @Override
            public void close() {
                if (showImage) {
                    HighGui.imshow(inputImage.toString(), mat);
                    HighGui.waitKey();
                }
            }
        };
    }
}
