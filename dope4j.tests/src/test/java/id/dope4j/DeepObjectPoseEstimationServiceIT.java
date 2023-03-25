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
import id.deeplearningutils.modality.cv.output.Point3D;
import id.dope4j.jackson.JsonUtils;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class DeepObjectPoseEstimationServiceIT {

    @Test
    public void test() throws Exception {
        var jsonUtils = new JsonUtils();
        var path =
                Optional.ofNullable(System.getenv("CHOCOLATE_PUDDING_ONNX_MODEL_PATH"))
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Env variable CHOCOLATE_PUDDING_ONNX_MODEL_PATH is"
                                                        + " missing"));
        var objectCuboidModel =
                new Cuboid3D(
                        new Point3D(), 4.947199821472168, 2.9923000335693359, 8.3498001098632812);
        var cameraInfo = jsonUtils.readCameraInfo(Paths.get("../config/camera_info.yaml"));
        var dopeService =
                new Dope4jFactory()
                        .createPoseEstimationService(path, objectCuboidModel, cameraInfo);
        var imageFileName = "scene_0001_0003_rgb_resized.jpg";
        var actual = dopeService.analyze(Paths.get("testset/" + imageFileName)).get(0);

        var expected =
                jsonUtils.readDope4jResults(Paths.get("testset/results.json")).stream()
                        .peek(r -> System.out.println(r.imagePath().get()))
                        .filter(
                                r ->
                                        r.imagePath()
                                                .get()
                                                .getFileName()
                                                .toString()
                                                .startsWith(imageFileName))
                        .findFirst()
                        .orElseThrow()
                        .detectedPoses();
        TestUtils.assertPoses(expected, actual);
    }
}
