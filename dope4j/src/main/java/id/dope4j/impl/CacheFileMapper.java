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

/**
 * Maps file names to their preprocessed versions inside cache folder.
 *
 * <p>Thread safe.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class CacheFileMapper {

    private Path cacheHome;

    public CacheFileMapper(Path cacheHome) {
        this.cacheHome = cacheHome;
    }

    public Path getTensorFile(Path imageFile) {
        return imageFile.resolveSibling(cacheHome.resolve(imageFile.getFileName() + ".tensor"));
    }

    public Path getProcessedImageFile(Path imageFile) {
        return imageFile.resolveSibling(cacheHome.resolve(imageFile.getFileName() + ".png"));
    }
}
