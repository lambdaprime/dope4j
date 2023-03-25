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
    private Path imagePath;

    /**
     * @param imagePath used to resolve path of incoming image files to the path inside cache.
     *     Example for imagePath /1/2/3 and cacheHome /tmp/_cache the image file /1/2/3/4/5 will be
     *     resolved to /tmp/_cache/4/5
     */
    public CacheFileMapper(Path imagePath, Path cacheHome) {
        this.imagePath = imagePath;
        this.cacheHome = cacheHome;
    }

    public Path getTensorFile(Path imageFile) {
        return appendToFullFileName(map(imageFile), ".tensor");
    }

    public Path getProcessedImageFile(Path imageFile) {
        return appendToFullFileName(map(imageFile), ".png");
    }

    public Path getCacheHome() {
        return cacheHome;
    }

    private Path map(Path path) {
        if (path.startsWith(imagePath)) path = imagePath.relativize(path).normalize();
        // if still absolute - remove root
        if (path.isAbsolute()) path = path.subpath(1, path.getNameCount());
        return cacheHome.resolve(path);
    }

    /** TODO migrate to XPaths */
    private static Path appendToFullFileName(Path path, String postfix) {
        var folder = path.getParent();
        return folder.resolve(path.getFileName() + postfix);
    }
}
