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

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import id.dope4j.exceptions.DopeException;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.io.InputImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves preprocessed input image with the output tensor into file system.
 *
 * <p>Thread safe.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class SaveStateDecoder implements DopeDecoder<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveStateDecoder.class);
    private CacheFileMapper mapper;

    public SaveStateDecoder(CacheFileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Void> decode(InputImage inputImage, NDArray outputTensor) throws DopeException {
        inputImage
                .path()
                .ifPresent(
                        path -> {
                            var outputFile = mapper.getProcessedImageFile(path);
                            try {
                                Files.createDirectories(outputFile.getParent());
                                saveToFile(inputImage.image(), outputFile);
                                outputFile = mapper.getTensorFile(path);
                                Files.createDirectories(outputFile.getParent());
                                saveToFile(outputTensor, outputFile);
                            } catch (IOException e) {
                                LOGGER.error("Could not save some of the resources", e);
                            }
                        });
        return Optional.empty();
    }

    private void saveToFile(NDArray array, Path path) throws IOException {
        LOGGER.debug("Saving output tensor to file {}", path);
        Files.write(path, array.encode());
    }

    private void saveToFile(Image image, Path path) throws IOException {
        LOGGER.debug("Saving image to file {}", path);
        image.save(new FileOutputStream(path.toFile()), "png");
    }
}
