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

import ai.djl.ndarray.NDArray;
import id.dope4j.DopeConstants;

/**
 * @author lambdaprime intid@protonmail.com
 */
public record OutputTensor(NDArray tensor, NDArray beliefMaps, AffinityFields affinities) {

    public OutputTensor(NDArray tensor, NDArray beliefMaps, NDArray affinities) {
        this(tensor, beliefMaps, new AffinityFields(affinities));
    }

    /** Raw tensor as returned by the network */
    public NDArray tensor() {
        return tensor;
    }

    /**
     * Tensor which represents Belief Maps for all keypoints.
     *
     * <p>There expected to be {@link DopeConstants#BELIEF_MAPS_COUNT} Belief Maps - one for each 8
     * vertices of object cuboid + 1 for cuboid center point.
     *
     * <p>For example beliefMaps[i][x][y] describes confidence of the network that vertex Vi of
     * object's cuboid is located at (x, y) coordinates of the input image.
     *
     * <p>Because on image there may be multiple of similar objects it means that on one
     * beliefMaps[i] there can be multiple of different Vi vertices detected, which belongs to
     * different objects on the image.
     *
     * <p>For memory purposes this effectively is a reference to subarray inside {@link #tensor}.
     */
    public NDArray beliefMaps() {
        return beliefMaps;
    }

    public AffinityFields affinities() {
        return affinities;
    }
}
