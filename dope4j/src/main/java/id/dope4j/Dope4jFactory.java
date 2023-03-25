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
package id.dope4j;

import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.dope4j.decoders.ObjectsDecoder;
import id.dope4j.io.OutputPoses;
import id.matcv.camera.CameraInfo;

/**
 * Factory methods for <b>dope4j</b>.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class Dope4jFactory {

    /**
     * Create {@link DeepObjectPoseEstimationService} which will use network output results to
     * decode all poses detected on the image.
     *
     * @param objectCuboidModel cuboid 3d model of the object which network is trained to detect
     * @param cameraInfo Camera intrinsics which is used for taking images
     */
    public DeepObjectPoseEstimationService<OutputPoses> createPoseEstimationService(
            String networkUrl, Cuboid3D objectCuboidModel, CameraInfo cameraInfo) {
        return createPoseEstimationService(
                networkUrl, objectCuboidModel, DopeConstants.DEFAULT_PEAK_THRESHOLD, cameraInfo);
    }

    /**
     * @param threshold keypoints threshold value (see {@link DopeConstants#DEFAULT_PEAK_THRESHOLD}
     *     for more details)
     */
    public DeepObjectPoseEstimationService<OutputPoses> createPoseEstimationService(
            String networkUrl,
            Cuboid3D objectCuboidModel,
            double threshold,
            CameraInfo cameraInfo) {
        return new DeepObjectPoseEstimationService<>(
                networkUrl, new ObjectsDecoder(threshold, objectCuboidModel, cameraInfo));
    }
}
