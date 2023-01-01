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
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to run inference.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DeepObjectPoseEstimationService<T> extends LazyService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DeepObjectPoseEstimationService.class);

    private String modelUrl;
    private Model model;
    private DopeTranslator<T> translator;

    public DeepObjectPoseEstimationService(String modelUrl, DopeDecoder<T> decoder) {
        this.modelUrl = modelUrl;
        translator = new DopeTranslator<>(decoder);
    }

    /** Perform inference */
    public Optional<T> analyze(Path imageFile) throws DopeException {
        return analyze(List.of(imageFile)).stream().findFirst();
    }

    /** Perform batch inference */
    public List<T> analyze(List<Path> images) throws DopeException {
        startLazy();
        try {
            var batch =
                    images.stream()
                            .map(
                                    imagePath -> {
                                        try {
                                            return new InputImage(imagePath);
                                        } catch (IOException e) {
                                            LOGGER.warn(
                                                    "Ignoring file {} due to error: {}: {},"
                                                            + " ignoring it",
                                                    imagePath,
                                                    e.getClass(),
                                                    e);
                                            return null;
                                        }
                                    })
                            .filter(o -> o != null)
                            .toList();
            if (images.isEmpty()) {
                LOGGER.warn("There is no images to analyze (possibly due to errors above)");
                return List.of();
            }
            LOGGER.info("Starting inference for batch of size {}", images.size());
            var output =
                    model.newPredictor(translator).batchPredict(batch).stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();
            LOGGER.info("Inference completed");
            return output;
        } catch (TranslateException e) {
            throw new DopeException(e);
        }
    }

    @Override
    protected void onStart() {
        LOGGER.info("Engine name: {}", Engine.getDefaultEngineName());
        LOGGER.info("Engine: {}", Engine.getInstance());
        LOGGER.info("GPU count: {}", Engine.getInstance().getGpuCount());
        LOGGER.info("CUDA version: {}", CudaUtils.getCudaVersion());
        try {
            model = Utils.loadModel(modelUrl);
        } catch (Exception e) {
            throw new DopeException("Could not load model " + modelUrl, e);
        }
    }

    @Override
    protected void onClose() {
        LOGGER.info("Closing model {}", modelUrl);
        model.close();
    }
}
