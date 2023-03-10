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
package id.dope4j.jackson.mixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import id.deeplearningutils.modality.cv.output.Point3D;
import java.util.List;

public class Cuboid3DJson {

    // ctor declaration need to match one in original class
    @JsonCreator
    public Cuboid3DJson(
            @JsonProperty("center") Point3D center,
            @JsonProperty("vertices") List<Point3D> vertices) {}
}
