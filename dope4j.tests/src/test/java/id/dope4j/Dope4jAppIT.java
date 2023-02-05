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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.dope4j.app.DeepObjectPoseEstimationApp;
import id.dope4j.app.Dope4jResult;
import id.dope4j.io.OutputPoses;
import id.dope4j.jackson.mixin.Cuboid2DJson;
import id.dope4j.jackson.mixin.Cuboid3DJson;
import id.xfunction.cli.CommandOptions;
import id.xfunction.nio.file.FilePredicates;
import id.xfunctiontests.XAsserts;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Dope4jAppIT {

    private static final double POSE_DELTA = 0.0999;
    private static final Path imagePath = Paths.get("testset");
    private static final List<Dope4jResult> dopeResults = new ArrayList<>();
    private static final List<Dope4jResult> dope4jResults = new ArrayList<>();

    @BeforeAll
    public static void setupAll() throws Exception {
        var reader =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .addMixIn(Cuboid2D.class, Cuboid2DJson.class)
                        .addMixIn(Cuboid3D.class, Cuboid3DJson.class)
                        .readerFor(Dope4jResult.class);
        reader.<Dope4jResult>readValues(new FileReader("testset/results.json"))
                .forEachRemaining(dopeResults::add);
        dopeResults.forEach(System.out::println);
        var out = new ByteArrayOutputStream();
        new DeepObjectPoseEstimationApp(
                        CommandOptions.collectOptions(
                                new String[] {
                                    "-action=runInference",
                                    "-imagePath=" + imagePath,
                                    "-objectSize=4.947199821472168,2.9923000335693359,8.3498001098632812",
                                    "-cache=true",
                                    "-cacheFolder=_cache",
                                    "-cameraInfo=../config/camera_info.yaml",
                                    "-debug=true"
                                }),
                        new PrintStream(out))
                .run();
        reader.<Dope4jResult>readValues(out.toByteArray()).forEachRemaining(dope4jResults::add);
        dope4jResults.forEach(System.out::println);
    }

    static Stream<Path> testDataProvider() throws IOException {
        return Files.list(imagePath).filter(FilePredicates.anyExtensionOf("jpg"));
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    public void test(Path image) throws Exception {
        var expected = findResult(dopeResults, image).detectedPoses();
        var actual = findResult(dope4jResults, image).detectedPoses();
        Assertions.assertEquals(expected.size(), actual.size());
        assertPoses(expected, actual);
    }

    private void assertPoses(OutputPoses expected, OutputPoses actual) {
        var expectedPoses = expected.poses();
        var actualPoses = actual.poses();
        for (int i = 0; i < expectedPoses.size(); i++) {
            var expectedPose = expectedPoses.get(i).position();
            var actualPose = actualPoses.get(i).position();
            XAsserts.assertSimilar(expectedPose.getX(), actualPose.getX(), POSE_DELTA);
            XAsserts.assertSimilar(expectedPose.getY(), actualPose.getY(), POSE_DELTA);
            XAsserts.assertSimilar(expectedPose.getZ(), actualPose.getZ(), POSE_DELTA);
        }
    }

    /** Finds results for current test image */
    private Dope4jResult findResult(List<Dope4jResult> results, Path image) {
        var imageFileName = image.getFileName().toString();
        return results.stream()
                .filter(
                        res ->
                                res.imagePath()
                                        .get()
                                        .getFileName()
                                        .toString()
                                        .startsWith(imageFileName))
                .findFirst()
                .orElseThrow();
    }
}
