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
package id.dope4j.decoders;

import static id.dope4j.DopeConstants.*;
import static id.dope4j.impl.Utils.*;

import ai.djl.modality.cv.output.Point;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import id.deeplearningutils.modality.cv.output.ExPoint;
import id.dope4j.DopeConstants;
import id.dope4j.exceptions.NoKeypointsFoundException;
import id.dope4j.io.OutputTensor;
import id.matcv.MatUtils;
import id.xfunction.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Following resources were used as a references for this implementation: <a
 * href="https://github.com/NVlabs/Deep_Object_Pose/blob/e6161e651ec19850f3f20e7c2657c8adfe01cc07/src/dope/inference/detector.py">detector.py</a>
 * <a
 * href="https://github.com/NVIDIA-ISAAC-ROS/isaac_ros_pose_estimation/blob/a6ad6e5eae07bc176a918da89e0d4088102f06ee/isaac_ros_dope/src/dope_decoder_node.cpp">dope_decoder_node.cpp</a>
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DopeDecoderUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DopeDecoderUtils.class);
    private MatUtils utils = new MatUtils();

    public OutputTensor readDopeOutput(NDArray tensor) {
        Shape tensorShape = tensor.getShape();
        Preconditions.equals(3, tensorShape.dimension(), "Tensor shape dimensions is wrong");
        var tensorSize = tensorShape.get(0);
        Preconditions.equals(TENSOR_LENGTH, tensorSize, "Total tensor size is wrong");
        int rows = (int) tensorShape.get(1);
        Preconditions.equals(TENSOR_ROWS, rows, "Number of rows is wrong");
        int cols = (int) tensorShape.get(2);
        Preconditions.equals(TENSOR_COLS, cols, "Number of cols is wrong");

        var beliefMaps = tensor.get(":" + BELIEF_MAPS_COUNT);
        var affinities = tensor.get(BELIEF_MAPS_COUNT + ":");

        debugNDArray("Belief maps", beliefMaps, "0:3, 0:5, 0:5");
        debugNDArray("Affinities", affinities, "0:3, 0:3, 0:3");

        return new OutputTensor(tensor, beliefMaps, affinities);
    }

    /**
     * Keypoints are object vertices + center points. They all part of Belief Maps. There is one
     * Belief Map per vertex. Center point beliefs are on the last Belief Map.
     *
     * <p>{@link DopeConstants#PEAK_THRESHOLD} allows to configure what predictions are ignored and
     * what not.
     *
     * @throws NoKeypointsFoundException
     */
    public List<List<Point>> findKeypoints(OutputTensor output) throws NoKeypointsFoundException {
        var beliefMaps = output.beliefMaps();
        var allPeaks = new ArrayList<List<Point>>();
        for (int i = 0; i < BELIEF_MAPS_COUNT; i++) {
            LOGGER.debug("Belief map shape: {}", beliefMaps.get(i).getShape());
            Mat belief = new MatOfFloat(beliefMaps.get(i).toFloatArray());
            belief = belief.reshape(1, BELIEF_SHAPE);
            if (i == 0) utils.debugMat("Belief map", belief, new Rect(0, 0, 3, 3));

            var blurred = new Mat();
            Imgproc.GaussianBlur(
                    belief,
                    blurred,
                    new Size(0, 0),
                    GAUSSIAN_SIGMA,
                    GAUSSIAN_SIGMA,
                    Core.BORDER_REFLECT);
            if (i == 0) utils.debugMat("Blurred belief map {}", blurred, new Rect(0, 0, 3, 3));
            var peaks = utils.findPeaks(blurred, PEAK_THRESHOLD);
            if (peaks.isEmpty())
                throw new NoKeypointsFoundException(
                        String.format(
                                "Belief map number %s, peak threshold %s", i, PEAK_THRESHOLD));
            allPeaks.add(peaks.stream().<Point>map(p -> new ExPoint(p.x, p.y)).toList());
        }
        LOGGER.debug("Detected keypoints: {}", allPeaks);
        return allPeaks;
    }
}
