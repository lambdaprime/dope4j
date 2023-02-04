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
package id.dope4j.decoders;

import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Pose;
import id.dope4j.impl.DjlOpenCvConverters;
import id.matcv.MatConverters;
import id.matcv.MatUtils;
import id.matcv.camera.CameraInfo;
import id.xfunction.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates pose of the cuboids and adds them to the output results.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class CuboidPoseCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuboidPoseCalculator.class);
    private static final Map<Integer, String> PNP_METHODS =
            Map.of(
                    Calib3d.SOLVEPNP_P3P, "SOLVEPNP_P3P",
                    Calib3d.SOLVEPNP_ITERATIVE, "SOLVEPNP_ITERATIVE");
    private static final MatConverters matConverters = new MatConverters();
    private static final DjlOpenCvConverters converters = new DjlOpenCvConverters();
    private static final MatUtils utils = new MatUtils();
    private final Cuboid3D cuboidModel3d;
    private final Mat cameraMat;
    private final MatOfDouble distortionMat;
    private final MatOfPoint3f pointsModel3d;
    private float scale;
    private final List<Pose> poses = new ArrayList<>();
    private final List<Cuboid2D> objects = new ArrayList<>();

    /**
     * @param scale allows to scale all input cuboids if needed, before performing calculations
     */
    public CuboidPoseCalculator(Cuboid3D cuboidModel3d, CameraInfo cameraInfo, float scale) {
        this.cuboidModel3d = cuboidModel3d;
        this.scale = scale;

        cameraMat = cameraInfo.cameraMatrix().toMat64F();
        utils.debugMat("cameraMat", cameraMat);
        distortionMat = cameraInfo.distortionCoefficients().toMatOfDouble();
        utils.debugMat("distortionMat", distortionMat);
        pointsModel3d = converters.copyToMatOfPoint3f(cuboidModel3d);
    }

    /**
     * If pose not found (due to not enough vertices or an error) nothing is added to output.
     *
     * @return true if pose was calculated and added to the output results
     */
    public boolean calculateAndAddPose(Cuboid2D cuboid2d) {
        var points2d = converters.copyToMatOfPoint2f(cuboid2d, scale);
        utils.debugMat("points2d", points2d);
        var cuboid3d = createCuboid3dModel(cuboid2d);
        var points3d = converters.copyToMatOfPoint3f(cuboid3d);
        utils.debugMat("points3d", points3d);
        return findPose(points2d, points3d);
    }

    /**
     * Return output results.
     *
     * @return list of all successfully calculated poses, one pose per each input cuboid
     */
    public List<Pose> getPoses() {
        return poses;
    }

    /**
     * Return output results.
     *
     * @return list of all input cuboids with their vertices projected according to this cuboid's
     *     calculated pose
     */
    public List<Cuboid2D> getObjects() {
        return objects;
    }

    private boolean findPose(MatOfPoint2f points2d, MatOfPoint3f points3d) {
        var vertexCount = points2d.rows();
        var rvec = new Mat();
        var tvec = new Mat();
        LOGGER.debug("Number of available vertices: {}", points2d.rows());
        if (vertexCount < 4) return false;
        var method = Calib3d.SOLVEPNP_ITERATIVE;
        if (points2d.rows() < 6) {
            if (points2d.rows() != 4) {
                points2d = new MatOfPoint2f(points2d.rowRange(0, 4));
                points3d = new MatOfPoint3f(points3d.rowRange(0, 4));
            }
            method = Calib3d.SOLVEPNP_P3P;
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Using PnP method: {}", PNP_METHODS.get(method));
        Preconditions.equals(points2d.rows(), points3d.rows(), "Vertex count mismatch");
        Calib3d.solvePnP(points3d, points2d, cameraMat, distortionMat, rvec, tvec, false, method);
        utils.debugMat("rvec", rvec);
        utils.debugMat("tvec", tvec);
        Calib3d.projectPoints(pointsModel3d, rvec, tvec, cameraMat, distortionMat, points2d);
        var position = converters.copyToPoint(tvec);
        if (position.getZ() < 0) position = position.scaled(-1);
        var orientation = matConverters.copyToVector3d(rvec);
        poses.add(new Pose(position, orientation));
        objects.add(converters.copyToCuboid2D(points2d));
        return true;
    }

    /**
     * Uses {@link #cuboidModel3d} to create a 3D cuboid for its 2D version.
     *
     * <p>There is not always 1:1 matching between cuboid 3D model and its detected2D versions. This
     * happens when not all vertices of 2D cuboid was detected. If certain vertices are missing in
     * the 2D cuboid they will be removed from its 3D cuboid as well.
     */
    private Cuboid3D createCuboid3dModel(Cuboid2D cuboid2d) {
        if (cuboid2d.getMissingVertexCount() == 0) return cuboidModel3d;
        var vertices =
                IntStream.range(0, Cuboid2D.VERTEX_COUNT)
                        .mapToObj(
                                i ->
                                        cuboid2d.getVertices().get(i) != null
                                                ? cuboidModel3d.getVertices().get(i)
                                                : null)
                        .toList();
        return new Cuboid3D(cuboidModel3d.getCenter(), vertices);
    }
}
