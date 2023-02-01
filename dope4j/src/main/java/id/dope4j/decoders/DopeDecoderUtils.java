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

import static id.dope4j.DopeConstants.BELIEF_MAPS_COUNT;
import static id.dope4j.DopeConstants.BELIEF_SHAPE;
import static id.dope4j.DopeConstants.GAUSSIAN_SIGMA;
import static id.dope4j.DopeConstants.TENSOR_COLS;
import static id.dope4j.DopeConstants.TENSOR_LENGTH;
import static id.dope4j.DopeConstants.TENSOR_ROWS;
import static id.dope4j.impl.Utils.debugNDArray;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.types.Shape;
import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Point2D;
import id.dope4j.DopeConstants;
import id.dope4j.io.AffinityFields;
import id.dope4j.io.OutputKeypoints;
import id.dope4j.io.OutputObjects2D;
import id.dope4j.io.OutputPoses;
import id.dope4j.io.OutputTensor;
import id.matcv.MatUtils;
import id.matcv.OpenCvKit;
import id.matcv.accessors.Float2DAccessor;
import id.matcv.camera.CameraInfo;
import id.mathcalc.Vector2f;
import id.xfunction.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point;
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
    private OpenCvKit openCvKit = new OpenCvKit();

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
     * Keypoints are cuboid vertices surrounding the object + cuboid center point. They all part of
     * Belief Maps. There is one Belief Map per each cuboid vertex (total 8). Center point beliefs
     * are on the last Belief Map.
     *
     * <p>{@link DopeConstants#PEAK_THRESHOLD} allows to configure which predictions are ignored and
     * which not.
     */
    public OutputKeypoints findKeypoints(OutputTensor output, double threshold) {
        var beliefMaps = output.beliefMaps();
        List<List<Point>> allPeaks = new ArrayList<>();
        List<List<Point2D>> keypoints = new ArrayList<>();
        int keypointsCount = 0;
        for (int i = 0; i < BELIEF_MAPS_COUNT; i++) {
            LOGGER.debug("Belief map shape: {}", beliefMaps.get(i).getShape());
            var belief = beliefMaps.get(i).toFloatArray();
            Mat beliefMat = new MatOfFloat(belief);
            beliefMat = beliefMat.reshape(1, BELIEF_SHAPE);
            if (i == 0) utils.debugMat("Belief map", beliefMat, new Rect(0, 0, 3, 3));

            var blurred = new Mat();
            Imgproc.GaussianBlur(
                    beliefMat,
                    blurred,
                    new Size(0, 0),
                    GAUSSIAN_SIGMA,
                    GAUSSIAN_SIGMA,
                    Core.BORDER_REFLECT);
            if (i == 0) utils.debugMat("Blurred belief map {}", blurred, new Rect(0, 0, 3, 3));
            var beliefAcc = Float2DAccessor.fromArray(belief, BELIEF_SHAPE);
            var peaks =
                    utils.findPeaks(blurred, Double.MIN_VALUE).stream()
                            .filter(
                                    p ->
                                            beliefAcc.get(p.y, p.x)
                                                    > DopeConstants.DEFAULT_PEAK_THRESHOLD)
                            .toList();

            allPeaks.add(peaks);

            // recalculating peak coordinates with respect to weighted average
            peaks =
                    openCvKit.applyWeightedAverage(
                            Float2DAccessor.fromArray(belief, BELIEF_SHAPE),
                            DopeConstants.PEAKS_WEIGHTED_AVERAGE_WINDOW,
                            peaks);
            keypointsCount += peaks.size();
            keypoints.add(
                    peaks.stream()
                            .map(
                                    p ->
                                            new Point2D(
                                                    p.x + DopeConstants.OFFSET_DUE_TO_UPSAMPLING,
                                                    p.y + DopeConstants.OFFSET_DUE_TO_UPSAMPLING))
                            .toList());
        }

        if (keypointsCount == 0) {
            LOGGER.warn("No keypoints found, peaks threshold {}", threshold);
            return OutputKeypoints.EMPTY;
        }

        LOGGER.debug("Detected peaks: {}", allPeaks);
        LOGGER.debug("Detected keypoints: {}", keypoints);

        var verticesBeliefs = keypoints.subList(0, DopeConstants.BELIEF_MAPS_COUNT - 1);
        LOGGER.debug("Detected vertices: {}", verticesBeliefs);
        var centerPointBeliefs = keypoints.get(DopeConstants.BELIEF_MAPS_COUNT - 1);
        LOGGER.debug("Detected center points: {}", centerPointBeliefs);
        return new OutputKeypoints(verticesBeliefs, centerPointBeliefs);
    }

    /**
     * Returns map of pairs: [center point: vertices], where vertices are all vertices of the cuboid
     * which matches to this center point. For those cuboid vertices which was not found because
     * they does not exist in verticesLists we return null. This guarantees that for each center
     * point we always will return list of 8 vertices but some vertices in this list may be null.
     */
    private Map<Point2D, List<Point2D>> matchCenterPointsWithVertices(
            List<Point2D> centerPoints,
            List<List<Point2D>> verticesLists,
            AffinityFields affinityFields) {
        Map<Point2D, List<Point2D>> objectsMap =
                centerPoints.stream().collect(Collectors.toMap(i -> i, i -> new ArrayList<>()));
        for (int beliefMapId = 0;
                beliefMapId < DopeConstants.BELIEF_MAPS_COUNT - 1;
                beliefMapId++) {
            var vertexId = beliefMapId;
            var vertices = verticesLists.get(beliefMapId);
            for (var vertex : vertices) {
                double minDistance = Integer.MAX_VALUE;
                Point2D candidateCenterPoint = null;
                for (var center : centerPoints) {
                    var affinityVec = affinityFields.getValue(beliefMapId, vertex).normalize();
                    var vec =
                            new Vector2f(
                                            (float) (center.getX() - vertex.getX()),
                                            (float) (center.getY() - vertex.getY()))
                                    .normalize();
                    var angle = affinityVec.sub(vec).norm();
                    var distance = vertex.distance(center);
                    if (distance < minDistance) {
                        candidateCenterPoint = center;
                        minDistance = distance;
                    }
                }
                if (candidateCenterPoint != null) objectsMap.get(candidateCenterPoint).add(vertex);
            }
            objectsMap.forEach(
                    (c, l) -> {
                        // For those center points which does not have matched vertex with current
                        // id we add null
                        if (l.size() < vertexId + 1) l.add(null);
                    });
        }
        return objectsMap;
    }

    public OutputObjects2D findObjects(OutputKeypoints keypoints, AffinityFields affinityFields) {
        if (keypoints == OutputKeypoints.EMPTY) return OutputObjects2D.EMPTY;
        var objectsMap =
                matchCenterPointsWithVertices(
                        keypoints.centerPoints(), keypoints.vertices(), affinityFields);
        var objects =
                new OutputObjects2D(
                        objectsMap.entrySet().stream()
                                .map(e -> newCuboid2D(e.getKey(), e.getValue()))
                                .toList());
        LOGGER.debug("Detected objects: {}", objects);
        return objects;
    }

    private Cuboid2D newCuboid2D(Point2D center, List<Point2D> vertices) {
        return new Cuboid2D(center, vertices);
    }

    public OutputPoses findPoses(
            OutputObjects2D objects, Cuboid3D cuboid3d, CameraInfo cameraInfo) {
        var calc = new CuboidPoseCalculator(cuboid3d, cameraInfo, DopeConstants.SCALE_FACTOR);
        objects.cuboids2d().forEach(calc::calculateAndAddPose);
        return new OutputPoses(cuboid3d, calc.getObjects(), calc.getPoses());
    }
}
