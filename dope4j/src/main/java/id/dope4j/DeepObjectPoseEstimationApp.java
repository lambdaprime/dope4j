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
package id.dope4j;

import static id.dope4j.impl.Utils.debugNDArray;

import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import id.dope4j.decoders.DopeDecoderUtils;
import id.dope4j.decoders.SaveStateDecoder;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.impl.Utils;
import id.dope4j.io.InputImage;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements access to dope4j functionality from command-line.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DeepObjectPoseEstimationApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepObjectPoseEstimationApp.class);
    private static final DopeDecoderUtils decoderUtils = new DopeDecoderUtils();
    private static final String CACHE_FOLDER_NAME = "_cache_dope4j";
    private CacheFileMapper mapper;
    private CommandOptions commandOptions;
    private SaveStateDecoder saveState;
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
        LOGGER.debug("Input image: {}", inputImage);
        debugNDArray("Input tensor", outputTensor, "0:3, 0:3, 0:3");
        inputImage
                .path()
                .ifPresent(
                        imageFile -> {
                            if (!isCacheEnabled) return;
                            var tensorFile = mapper.getTensorFile(imageFile);
                            if (tensorFile.toFile().exists()) return;
                            LOGGER.debug("Adding image data for {} to cache", imageFile);
                            saveState.decode(inputImage, outputTensor);
                        });
        var output = decoderUtils.readDopeOutput(outputTensor);
        var keypoints = decoderUtils.findKeypoints(output);
        keypoints =
                keypoints.stream()
                        .map(l -> Utils.scalePoints(l.stream(), DopeConstants.SCALE_FACTOR))
                        .toList();
        Mat mat = (Mat) inputImage.image().getWrappedImage();
        var verticesBeliefs = keypoints.subList(0, DopeConstants.BELIEF_MAPS_COUNT - 1);
        if (commandOptions.isOptionTrue("showVerticesBeliefs"))
            verticesBeliefs.forEach(l -> Utils.drawKeypoints(mat, l));
        var centerpointBeliefs = keypoints.get(DopeConstants.BELIEF_MAPS_COUNT - 1);
        if (commandOptions.isOptionTrue("showCenterPointBeliefs"))
            Utils.drawKeypoints(mat, centerpointBeliefs);
        if (commandOptions.isOptionTrue("showAffinityFields"))
            Utils.drawAffinityFields(mat, output);
        HighGui.imshow(inputImage.toString(), mat);
        HighGui.waitKey();
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
                        .map(s -> Paths.get(s))
                        .or(() -> XFiles.TEMP_FOLDER.map(p -> p.resolve(CACHE_FOLDER_NAME)))
                        .orElse(Paths.get(CACHE_FOLDER_NAME));
        LOGGER.info("Cache folder: {}", cacheFolder);
        mapper = new CacheFileMapper(cacheFolder);
        saveState = new SaveStateDecoder(mapper);
        if (!imagePath.toFile().exists())
            throw new RuntimeException("Path does not exist: " + imagePath);
        var imageFilesList = listImageFiles(imagePath);
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

    private boolean processFromCache(Path imageFile) throws IOException {
        var tensorFile = mapper.getTensorFile(imageFile);
        if (!tensorFile.toFile().exists()) return false;
        LOGGER.debug(
                "Image data {} found in cache, do not run inference and use it instead", imageFile);
        var tensor =
                NDArray.decode(
                        Engine.getInstance().newBaseManager(), Files.readAllBytes(tensorFile));
        process(new InputImage(imageFile), tensor);
        return true;
    }

    private List<Path> listImageFiles(Path imagePath) throws IOException {
        return Files.walk(imagePath, 1)
                .filter(FilePredicates.anyExtensionOf("png", "jpg"))
                .toList();
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
                System.err.println(
                        "Total execution time: " + Duration.between(startAt, Instant.now()));
        }
        System.exit(code);
    }
}
