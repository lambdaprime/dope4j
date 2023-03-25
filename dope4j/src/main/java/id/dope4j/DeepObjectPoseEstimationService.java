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

import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.translate.TranslateException;
import ai.djl.util.cuda.CudaUtils;
import id.dope4j.decoders.DopeDecoder;
import id.dope4j.exceptions.DopeException;
import id.dope4j.impl.DopeTranslator;
import id.dope4j.impl.Utils;
import id.dope4j.io.InputImage;
import id.xfunction.util.LazyService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to run inference.
 *
 * <p>To perform inference this service requires path to DOPE network and {@link DopeDecoder}
 * implementation. Each input image is sent to the DOPE network and network output results are
 * decoded with {@link DopeDecoder}.
 *
 * <p>Network knows how to detect only one class of objects. It means that all detected poses will
 * be for different objects present on the image where all these objects belong to the same class.
 *
 * @param <R> type of the service output inference results
 * @author lambdaprime intid@protonmail.com
 */
public class DeepObjectPoseEstimationService<R> extends LazyService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeepObjectPoseEstimationService.class);

    private final Meter METER =
            GlobalOpenTelemetry.getMeter(DeepObjectPoseEstimationService.class.getSimpleName());
    private final LongCounter ANALYZE_COUNTER =
            METER.counterBuilder("analyze").setDescription("Number of method calls").build();
    private final LongCounter IMAGES_COUNTER =
            METER.counterBuilder("input_images")
                    .setDescription("Total number of input images")
                    .build();
    private final LongCounter ANALYZED_IMAGES_COUNTER =
            METER.counterBuilder("analyzed_images")
                    .setDescription("Total number of images analyzed")
                    .build();
    private String networkUrl;
    private Model model;
    private DopeTranslator<R> translator;

    public DeepObjectPoseEstimationService(String networkUrl, DopeDecoder<R> decoder) {
        this.networkUrl = networkUrl;
        translator = new DopeTranslator<>(decoder);
    }

    /**
     * Perform batch inference.
     *
     * @return poses of all detected objects which are of the same class
     */
    public List<R> analyze(Path... images) throws DopeException {
        startLazy();
        ANALYZE_COUNTER.add(1);
        IMAGES_COUNTER.add(images.length);
        if (images.length == 0) {
            LOGGER.warn("Received empty list of images, nothing to analyze");
            return List.of();
        }
        try {
            var batch =
                    Arrays.stream(images)
                            .map(
                                    imagePath -> {
                                        try {
                                            return new InputImage(imagePath);
                                        } catch (IOException e) {
                                            LOGGER.warn(
                                                    "Ignoring file {} due to an error: {}: {}",
                                                    imagePath,
                                                    e.getClass(),
                                                    e);
                                            return null;
                                        }
                                    })
                            .filter(o -> o != null)
                            .toList();
            if (batch.isEmpty()) {
                LOGGER.warn("There is no images to analyze (possibly due to errors above)");
                return List.of();
            }
            LOGGER.info("Starting inference for batch of size {}", batch.size());
            var output =
                    model.newPredictor(translator).batchPredict(batch).stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();
            LOGGER.info("Inference completed");
            ANALYZED_IMAGES_COUNTER.add(output.size());
            return output;
        } catch (TranslateException e) {
            throw new DopeException(e);
        }
    }

    /**
     * @hidden exclude from javadoc
     */
    @Override
    protected void onStart() {
        LOGGER.info("Engine name: {}", Engine.getDefaultEngineName());
        LOGGER.info("Engine: {}", Engine.getInstance());
        LOGGER.info("GPU count: {}", Engine.getInstance().getGpuCount());
        LOGGER.info("CUDA version: {}", CudaUtils.getCudaVersion());
        try {
            model = Utils.loadModel(networkUrl);
        } catch (Exception e) {
            throw new DopeException("Could not load model " + networkUrl, e);
        }
    }

    /**
     * @hidden exclude from javadoc
     */
    @Override
    protected void onClose() {
        LOGGER.info("Closing model {}", networkUrl);
        model.close();
    }
}
