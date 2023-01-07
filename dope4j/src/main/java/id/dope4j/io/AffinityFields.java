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
package id.dope4j.io;

import ai.djl.modality.cv.output.Point;
import ai.djl.ndarray.NDArray;
import id.mathcalc.Vector2f;

/**
 * @author lambdaprime intid@protonmail.com
 */
public record AffinityFields(NDArray affinities) {

    public Vector2f getValue(int fieldId, Point vertex) {
        return getValue(fieldId, vertex.getX(), vertex.getY());
    }

    public Vector2f getValue(int fieldId, double x, double y) {
        return new Vector2f(
                affinities.get(fieldId * 2).getFloat((long) y, (long) x),
                affinities.get(fieldId * 2 + 1).getFloat((long) y, (long) x));
    }

    public int size() {
        return (int) affinities.size(0);
    }
}
