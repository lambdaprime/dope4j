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
package id.dope4j;

import id.dope4j.io.OutputTensor;

/**
 * @author lambdaprime intid@protonmail.com
 */
public interface DopeConstants {
    int IMAGE_WIDTH = 640;
    int IMAGE_HEIGHT = 480;

    /**
     * @see OutputTensor#beliefMaps
     */
    int BELIEF_MAPS_COUNT = 9;

    /**
     * @see OutputTensor#affinities
     */
    long AFFINITIES_COUNT = 16;

    long TENSOR_LENGTH = BELIEF_MAPS_COUNT + AFFINITIES_COUNT;
    int TENSOR_ROWS = 60;
    int TENSOR_COLS = 80;
    int[] BELIEF_SHAPE = {TENSOR_ROWS, TENSOR_COLS};
    double GAUSSIAN_SIGMA = 3.0;

    /**
     * Minimum confidence value from {@link OutputTensor#beliefMaps()} for point to be considered as
     * a keypoint.
     *
     * <p>Analog to config_detect.thresh_points from original DOPE decoder.
     */
    double DEFAULT_PEAK_THRESHOLD = 0.1;

    /**
     * Minimum confidence value from blurred {@link OutputTensor#beliefMaps()} for point to be
     * considered as a keypoint.
     *
     * <p>Analog to config_detect.thresh_map from original DOPE decoder.
     */
    double DEFAULT_BLURRED_PEAK_THRESHOLD = 0.01;

    float SCALE_FACTOR = 8;
    int PEAKS_WEIGHTED_AVERAGE_WINDOW = 5;
    float OFFSET_DUE_TO_UPSAMPLING = 0.4395F;
}
