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
package id.dope4j.impl;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.modality.cv.output.Point;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import id.deeplearningutils.modality.cv.output.Point2D;
import id.dope4j.DopeConstants;
import id.dope4j.io.AffinityFields;
import id.matcv.OpenCvKit;
import id.matcv.RgbColors;
import id.matcv.accessors.Vector2f2DAccessor;
import id.xfunction.Preconditions;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final DjlOpenCvConverters converters = new DjlOpenCvConverters();
    private static final OpenCvKit openCvKit = new OpenCvKit();

    public static void debugNDArray(String description, NDArray array, String slice) {
        if (!LOGGER.isDebugEnabled()) return;
        LOGGER.debug(
                description + " shape {} (slice {}: {})",
                array.getShape(),
                slice,
                array.get(slice));
    }

    public static void debugAsInt(String description, NDArray array, String slice) {
        if (!LOGGER.isDebugEnabled()) return;
        try {
            array = array.toType(DataType.INT32, false);
        } catch (UnsupportedOperationException e) {
            // fall back to original debug
        }
        debugNDArray(description, array, slice);
    }

    public static void debug2DNDArray(NDArray array) {
        if (!LOGGER.isDebugEnabled()) return;
        var buf = new StringBuilder("\n");
        for (int i = 0; i < array.size(0); i++) {
            for (int j = 0; j < array.size(1); j++) {
                buf.append(String.format("%14f", array.get(i, j).getFloat()));
            }
            buf.append("\n");
        }
        LOGGER.debug(buf.toString());
    }

    /**
     * This transform will normalize each channel of the input array as follows: input[channel] -
     * mean / std
     *
     * @see <a
     *     href="http://pytorch.org/vision/main/generated/torchvision.transforms.Normalize.html">torchvision.transforms.Normalize</a>
     */
    public static NDArray normalize(NDArray array, float mean, float std) {
        return array.sub(mean).div(mean);
    }

    //    public static void listModels() throws Exception {
    //        Consumer<List<Artifact>> print =
    //                artifacts -> {
    //                    artifacts.stream().forEach(artifact ->
    // System.out.println(artifact.getName()));
    //                };
    //        System.out.println("\nOrtModelZoo:");
    //        OrtModelZoo.listModels().values().forEach(print);
    //        System.out.println("\nPtModelZoo:");
    //        PtModelZoo.listModels().values().forEach(print);
    //        System.out.println("\nDefaultModelZoo:");
    //        DefaultModelZoo.listModels().values().forEach(print);
    //    }

    public static Model loadModel(String modelUrl)
            throws ModelNotFoundException, MalformedModelException, IOException {
        LOGGER.info("Loading model...");
        var criteria =
                Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelUrls(modelUrl)
                        .optEngine("OnnxRuntime") // use OnnxRuntime engine by default
                        .optOption("ortDevice", "TensorRT")
                        .build();
        var model = criteria.loadModel();
        LOGGER.info("Model loaded");
        LOGGER.info("Model name: {}", model.getName());
        LOGGER.info("Model data type: {}", model.getDataType());
        LOGGER.info("Model path: {}", model.getModelPath());
        try {
            LOGGER.info("Model artifacts: {}", Arrays.toString(model.getArtifactNames()));
        } catch (UnsupportedOperationException e) {
            // if model does not support this operation, we ignore
        }
        LOGGER.info("Model input: ");
        model.describeInput().stream()
                .map(s -> String.format("%s = %s", s.getKey(), s.getValue()))
                .forEach(LOGGER::info);
        return model;
    }

    public static List<Point2D> scalePoints(Stream<? extends Point> points, int scale) {
        return points.map(p -> new Point2D(p.getX() * scale, p.getY() * scale)).toList();
    }

    //    public static Landmark scaleLandmark(Landmark landmark, int scale) {
    //        return new Landmark(
    //                landmark.getX() * scale,
    //                landmark.getY() * scale,
    //                landmark.getWidth() * scale,
    //                landmark.getHeight() * scale,
    //                scalePoints(StreamSupport.stream(landmark.getPath().spliterator(), false),
    // scale));
    //    }

    /** Draws markers at the given points */
    public static void drawKeypoints(Mat image, Collection<? extends Point> points) {
        Preconditions.equals(DopeConstants.IMAGE_HEIGHT, image.rows(), "Image height is wrong");
        Preconditions.equals(DopeConstants.IMAGE_WIDTH, image.cols(), "Image width is wrong");
        points.forEach(
                p ->
                        Imgproc.drawMarker(
                                image,
                                converters.copyToPoint(p, DopeConstants.SCALE_FACTOR),
                                RgbColors.GREEN));
    }

    public static void drawAffinityFields(Mat image, AffinityFields fields) {
        Preconditions.equals(DopeConstants.IMAGE_HEIGHT, image.rows(), "Image height is wrong");
        Preconditions.equals(DopeConstants.IMAGE_WIDTH, image.cols(), "Image width is wrong");
        for (int i = 0; i < fields.size(); i++) {
            // make i enclose final
            var j = i;
            openCvKit.drawVectorField(
                    image,
                    (int) DopeConstants.SCALE_FACTOR,
                    (int) DopeConstants.SCALE_FACTOR,
                    RgbColors.RED,
                    Vector2f2DAccessor.fromGetter(
                            image.rows(),
                            image.cols(),
                            (x, y) ->
                                    fields.getValue(
                                                    j,
                                                    y / DopeConstants.SCALE_FACTOR,
                                                    x / DopeConstants.SCALE_FACTOR)
                                            .mul(DopeConstants.SCALE_FACTOR)));
        }
    }
}
