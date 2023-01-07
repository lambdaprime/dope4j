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
package id.dope4j.decoders;

import ai.djl.ndarray.NDArray;
import ai.djl.translate.Translator;
import id.dope4j.exceptions.DopeException;
import id.dope4j.io.InputImage;
import java.util.Optional;

/**
 * Decoder for DOPE network output.
 *
 * <p>Output of DOPE network can be decoded differently based on usecase. Users may like to decode:
 *
 * <ul>
 *   <li>poses
 *   <li>only vertices
 *   <li>only center points
 *   <li>vector fields
 *   <li>everything above
 * </ul>
 *
 * <p>Must be thread safe.
 *
 * @param <R> output type of the decoder
 * @author lambdaprime intid@protonmail.com
 */
@FunctionalInterface
public interface DopeDecoder<R> {

    /**
     * This method is called inside of {@link
     * Translator#processOutput(ai.djl.translate.TranslatorContext, ai.djl.ndarray.NDList)}. The
     * lifetime of tensor is limited to the execution time of this method. It means data from the
     * tensor should not be referenced after this method completes because it may be deleted.
     *
     * @throws DopeException
     */
    Optional<R> decode(InputImage inputImage, NDArray outputTensor) throws DopeException;
}
