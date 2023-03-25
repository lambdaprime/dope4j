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
package id.dope4j.app;

import id.dope4j.DopeConstants;
import id.dope4j.decoders.ObjectsDecoder.Inspector;
import id.dope4j.decoders.SaveStateToCacheDecoder;
import id.dope4j.impl.CacheFileMapper;
import id.dope4j.impl.DjlOpenCvConverters;
import id.dope4j.impl.Utils;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputKeypoints;
import id.dope4j.io.OutputObjects2D;
import id.dope4j.io.OutputPoses;
import id.dope4j.io.OutputTensor;
import id.matcv.RgbColors;
import java.io.PrintStream;
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Dope4jInspector implements Inspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dope4jInspector.class);
    private static final DjlOpenCvConverters converters = new DjlOpenCvConverters();
    private final Mat mat;
    private final InputImage inputImage;
    private boolean showImage;
    private boolean showVerticesBeliefs;
    private boolean showCenterPointBeliefs;
    private boolean showAffinityFields;
    private boolean showMatchedVertices;
    private boolean showCuboid2D;
    private boolean showProjectedCuboids2D;
    private Optional<SaveStateToCacheDecoder> saveStateOpt;
    private PrintStream out;
    private int lineThickness;

    Dope4jInspector(
            PrintStream out,
            Mat mat,
            InputImage inputImage,
            Optional<CacheFileMapper> cacheFileMapper,
            boolean showVerticesBeliefs,
            boolean showCenterPointBeliefs,
            boolean showAffinityFields,
            boolean showMatchedVertices,
            boolean showCuboid2D,
            boolean showProjectedCuboids2D,
            int lineThickness) {
        this.out = out;
        this.mat = mat;
        this.inputImage = inputImage;
        this.showVerticesBeliefs = showVerticesBeliefs;
        this.showCenterPointBeliefs = showCenterPointBeliefs;
        this.showAffinityFields = showAffinityFields;
        this.showMatchedVertices = showMatchedVertices;
        this.showCuboid2D = showCuboid2D;
        this.showProjectedCuboids2D = showProjectedCuboids2D;
        this.lineThickness = lineThickness;
        saveStateOpt = cacheFileMapper.map(SaveStateToCacheDecoder::new);
    }

    @Override
    public void inspectTensor(OutputTensor outputTensor) {
        inputImage
                .path()
                .ifPresent(
                        imageFile -> {
                            saveStateOpt.ifPresent(
                                    saveState -> {
                                        var tensorFile =
                                                saveState.getCacheMapper().getTensorFile(imageFile);
                                        if (tensorFile.toFile().exists()) return;
                                        LOGGER.debug(
                                                "Adding image data for {} into the cache",
                                                imageFile);
                                        saveState.decode(inputImage, outputTensor.tensor());
                                    });
                        });

        if (showAffinityFields) {
            Utils.drawAffinityFields(mat, outputTensor.affinities());
            showImage = true;
        }
    }

    @Override
    public void inspectOjects2D(OutputObjects2D objects) {
        if (showMatchedVertices) {
            for (var cuboid : objects.cuboids2d()) {
                var centerPoint =
                        converters.copyToPoint(cuboid.getCenter(), DopeConstants.SCALE_FACTOR);
                cuboid.getVertices().stream()
                        .filter(v -> v != null)
                        .map(v -> converters.copyToPoint(v, DopeConstants.SCALE_FACTOR))
                        .forEach(
                                v ->
                                        Imgproc.line(
                                                mat,
                                                centerPoint,
                                                v,
                                                RgbColors.GREEN,
                                                lineThickness));
            }
            showImage = true;
        }
        if (showCuboid2D) {
            objects.cuboids2d()
                    .forEach(
                            cuboid ->
                                    Utils.drawCuboid2D(
                                            mat,
                                            cuboid,
                                            DopeConstants.SCALE_FACTOR,
                                            RgbColors.GREEN,
                                            lineThickness));
            showImage = true;
        }
    }

    @Override
    public void inspectKeypoints(OutputKeypoints keypoints) {
        if (showVerticesBeliefs) {
            keypoints.vertices().forEach(l -> Utils.drawKeypoints(mat, l, lineThickness));
            showImage = true;
        }
        if (showCenterPointBeliefs) {
            Utils.drawKeypoints(mat, keypoints.centerPoints(), lineThickness);
            showImage = true;
        }
    }

    @Override
    public void inspectPoses(OutputPoses poses) {
        out.println(new Dope4jResult(inputImage.path(), poses).toString());
        if (showProjectedCuboids2D) {
            poses.objects2d()
                    .forEach(
                            cuboid ->
                                    Utils.drawCuboid2D(
                                            mat, cuboid, 1, RgbColors.RED, lineThickness));
            showImage = true;
        }
    }

    @Override
    public void close() {
        if (showImage) {
            HighGui.imshow(inputImage.toString(), mat);
            HighGui.waitKey();
        }
    }
}
