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
package id.dope4j.impl;

import static id.dope4j.impl.Utils.debugAsInt;
import static id.dope4j.impl.Utils.debugNDArray;
import static id.dope4j.impl.Utils.normalize;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import id.dope4j.decoders.DopeDecoder;
import id.dope4j.io.InputImage;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whatever exception is thrown by {@link Translator} will be thrown by {@link Predictor} as {@link
 * TranslateException} and the prediction will stop. To avoid this and keep prediction going we use
 * {@link Optional} and return {@link Optional#empty()} in case of errors.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DopeTranslator<T> implements Translator<InputImage, Optional<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DopeTranslator.class);
    private static final String IMAGE_KEY = "imageKey";
    private DopeDecoder<T> decoder;

    public DopeTranslator(DopeDecoder<T> decoder) {
        this.decoder = decoder;
    }

    @Override
    public NDList processInput(TranslatorContext ctx, InputImage inputImage) {
        LOGGER.trace("processInput {}", inputImage);
        ctx.setAttachment(IMAGE_KEY, inputImage);
        NDArray rgbArray = inputImage.image().toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
        debugAsInt("Input rgbArray", rgbArray, "0:3, 0:3, 0:3");
        var tensor = normalize(NDImageUtils.toTensor(rgbArray), .5F, .5F);
        debugNDArray("Input tensor", tensor, "0:3, 0:3, 0:3");
        return new NDList(tensor);
    }

    @Override
    public Optional<T> processOutput(TranslatorContext ctx, NDList list) {
        LOGGER.trace("processOutput {}", list);
        if (list.isEmpty()) {
            LOGGER.warn("Received empty output");
            return Optional.empty();
        }
        var tensor = list.get(0);
        debugNDArray("Output tensor", tensor, "0:3, 0:3, 0:3");
        if (ctx.getAttachment(IMAGE_KEY) instanceof InputImage image) {
            return decoder.decode(image, tensor);
        } else {
            LOGGER.warn(
                    "Lost input attachment for the received inference output. Output will be"
                            + " ignored.");
            return Optional.empty();
        }
    }

    @Override
    public Batchifier getBatchifier() {
        // The Batchifier describes how to combine a batch together
        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a
        // single [N, X1, X2, ...] array
        return Batchifier.STACK;
    }
}
