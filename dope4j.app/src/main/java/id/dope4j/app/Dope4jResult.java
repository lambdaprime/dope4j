/*
 * Copyright 2023 dope4j project
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
package id.dope4j.app;

import id.dope4j.io.OutputPoses;
import id.xfunction.XJsonStringBuilder;
import java.nio.file.Path;
import java.util.Optional;

public record Dope4jResult(Optional<Path> imagePath, OutputPoses detectedPoses) {
    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("imagePath", imagePath());
        builder.append("detectedPoses", detectedPoses());
        return builder.toString();
    }
}
