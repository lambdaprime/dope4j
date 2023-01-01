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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Maps file names to their preprocessed versions which are inside subfolder {@link #SUBFOLDER}
 *
 * @author lambdaprime intid@protonmail.com
 */
public class FileMapper {

    private static final String SUBFOLDER = "_cached";

    public Path getTensorFile(Path imageFile) {
        return imageFile.resolveSibling(Paths.get(SUBFOLDER, imageFile.getFileName() + ".tensor"));
    }

    public Path getProcessedImageFile(Path imageFile) {
        return imageFile.resolveSibling(Paths.get(SUBFOLDER, imageFile.getFileName() + ".png"));
    }
}
