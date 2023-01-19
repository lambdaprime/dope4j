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
import id.dope4j.impl.Utils;
import id.dope4j.io.InputImage;
import id.dope4j.io.OutputKeypoints;
import id.dope4j.io.OutputObjects;
import id.dope4j.io.OutputTensor;
import id.matcv.RgbColors;
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Dope4jInspector implements Inspector {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dope4jInspector.class);
    private final Mat mat;
    private final InputImage inputImage;
    private boolean showImage;
    private boolean showVerticesBeliefs;
    private boolean showCenterPointBeliefs;
    private boolean showAffinityFields;
    private boolean showMatchedVertices;
    private Optional<SaveStateToCacheDecoder> saveStateOpt;

    Dope4jInspector(
            Mat mat,
            InputImage inputImage,
            Optional<CacheFileMapper> cacheFileMapper,
            boolean showVerticesBeliefs,
            boolean showCenterPointBeliefs,
            boolean showAffinityFields,
            boolean showMatchedVertices) {
        this.mat = mat;
        this.inputImage = inputImage;
        this.showVerticesBeliefs = showVerticesBeliefs;
        this.showCenterPointBeliefs = showCenterPointBeliefs;
        this.showAffinityFields = showAffinityFields;
        this.showMatchedVertices = showMatchedVertices;
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
    public void inspectOjects(OutputObjects objects) {
        if (showMatchedVertices) {
            objects.objects()
                    .forEach(
                            bb -> {
                                var centerPoint =
                                        new org.opencv.core.Point(
                                                bb.getCenter().getX() * DopeConstants.SCALE_FACTOR,
                                                bb.getCenter().getY() * DopeConstants.SCALE_FACTOR);
                                bb.getVertices()
                                        .forEach(
                                                vertex -> {
                                                    var v =
                                                            new org.opencv.core.Point(
                                                                    vertex.getX()
                                                                            * DopeConstants
                                                                                    .SCALE_FACTOR,
                                                                    vertex.getY()
                                                                            * DopeConstants
                                                                                    .SCALE_FACTOR);
                                                    Imgproc.line(
                                                            mat, centerPoint, v, RgbColors.GREEN);
                                                });
                            });
            showImage = true;
        }
    }

    @Override
    public void inspectKeypoints(OutputKeypoints keypoints) {
        if (showVerticesBeliefs) {
            keypoints.vertices().forEach(l -> Utils.drawKeypoints(mat, l));
            showImage = true;
        }
        if (showCenterPointBeliefs) {
            Utils.drawKeypoints(mat, keypoints.centerPoints());
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
