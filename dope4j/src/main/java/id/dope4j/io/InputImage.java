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
package id.dope4j.io;

import ai.djl.modality.cv.Image;
import ai.djl.opencv.OpenCVImageFactory;
import id.dope4j.DopeConstants;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author lambdaprime intid@protonmail.com
 */
public record InputImage(Image image, Optional<Path> path) {

    public InputImage(Image image) {
        this(image, Optional.empty());
    }

    public InputImage(Image image, Path path) {
        this(image, Optional.of(path));
    }

    public InputImage(Path path) throws IOException {
        this(
                OpenCVImageFactory.getInstance()
                        .fromFile(path.toAbsolutePath())
                        .resize(DopeConstants.IMAGE_WIDTH, DopeConstants.IMAGE_HEIGHT, false),
                path);
    }

    /** Preprocessed image */
    public Image image() {
        return image;
    }

    /** Path to the image if it comes from file system. */
    public Optional<Path> path() {
        return path;
    }
}
