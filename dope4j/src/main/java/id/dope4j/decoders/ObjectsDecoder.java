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

import static id.dope4j.impl.Utils.debugNDArray;

import ai.djl.ndarray.NDArray;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.dope4j.exceptions.DopeException;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputKeypoints;
import id.dope4j.io.OutputObjects2D;
import id.dope4j.io.OutputPoses;
import id.dope4j.io.OutputTensor;
import id.matcv.camera.CameraInfo;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes DOPE output tensor to the list of detected objects.
 *
 * <p>Thread safe.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class ObjectsDecoder implements DopeDecoder<OutputPoses> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectsDecoder.class);
    private static final DopeDecoderUtils decoderUtils = new DopeDecoderUtils();

    /**
     * Decoding DOPE output is multistep process. Inspector allows to introspect every step of this
     * process.
     *
     * @author lambdaprime intid@protonmail.com
     */
    public static interface Inspector {
        void inspectTensor(OutputTensor outputTensor);

        void inspectKeypoints(OutputKeypoints keypoints);

        void inspectOjects2D(OutputObjects2D objects);

        void inspectPoses(OutputPoses poses);

        void close();

        /**
         * {@link Builder} is used by {@link ObjectsDecoder} creates a new instance of {@link
         * Inspector} for each output it is going to decode.
         *
         * @author lambdaprime intid@protonmail.com
         */
        public static interface Builder {
            Inspector build(InputImage inputImage);
        }
    }

    private double threshold;
    private Optional<Inspector.Builder> inspectorBuilder = Optional.empty();
    public CameraInfo cameraInfo;
    private Cuboid3D cuboid3DModel;

    public ObjectsDecoder(double threshold, Cuboid3D cuboid3DModel, CameraInfo cameraInfo) {
        this(threshold, cuboid3DModel, cameraInfo, null);
    }

    public ObjectsDecoder(
            double threshold,
            Cuboid3D cuboid3DModel,
            CameraInfo cameraInfo,
            Inspector.Builder inspectorBuilder) {
        this.threshold = threshold;
        this.cuboid3DModel = cuboid3DModel;
        this.cameraInfo = cameraInfo;
        this.inspectorBuilder = Optional.ofNullable(inspectorBuilder);
    }

    @Override
    public Optional<OutputPoses> decode(InputImage inputImage, NDArray outputTensor)
            throws DopeException {
        LOGGER.debug("Input image: {}", inputImage);
        debugNDArray("Input tensor", outputTensor, "0:3, 0:3, 0:3");
        var inspectorOpt = inspectorBuilder.map(builder -> builder.build(inputImage));
        try {
            var output = decoderUtils.readDopeOutput(outputTensor);
            inspectorOpt.ifPresent(inspector -> inspector.inspectTensor(output));
            var keypoints = decoderUtils.findKeypoints(output, threshold);
            inspectorOpt.ifPresent(inspector -> inspector.inspectKeypoints(keypoints));
            var objects2d = decoderUtils.findObjects(keypoints, output.affinities());
            inspectorOpt.ifPresent(inspector -> inspector.inspectOjects2D(objects2d));
            var poses = decoderUtils.findPoses(objects2d, cuboid3DModel, cameraInfo);
            inspectorOpt.ifPresent(inspector -> inspector.inspectPoses(poses));
            return Optional.of(poses);
        } finally {
            inspectorOpt.ifPresent(Inspector::close);
        }
    }
}
