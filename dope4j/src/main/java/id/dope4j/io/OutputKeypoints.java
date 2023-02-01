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

import id.deeplearningutils.modality.cv.output.Point2D;
import id.xfunction.XJsonStringBuilder;
import java.util.List;

/**
 * @author lambdaprime intid@protonmail.com
 */
public record OutputKeypoints(List<List<Point2D>> vertices, List<Point2D> centerPoints) {

    public static final OutputKeypoints EMPTY = new OutputKeypoints(List.of(), List.of());

    /**
     * There are 8 lists in total which represent 8 corners of 3D cuboids which surround the object
     */
    public List<List<Point2D>> vertices() {
        return vertices;
    }

    /** Center points of all objects */
    public List<Point2D> centerPoints() {
        return centerPoints;
    }

    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("centerPoints", centerPoints());
        builder.append("vertices", vertices());
        return builder.toString();
    }
}
