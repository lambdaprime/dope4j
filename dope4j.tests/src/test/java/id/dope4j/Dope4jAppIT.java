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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import id.dope4j.app.DeepObjectPoseEstimationApp;
import id.xfunction.XJson;
import id.xfunction.cli.CommandOptions;
import id.xfunction.nio.file.FilePredicates;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Dope4jAppIT {

    private static final Path imagePath = Paths.get("testset");
    private static final List<JsonNode> dopeResults = new ArrayList<>();
    private static final List<JsonNode> dope4jResults = new ArrayList<>();

    @BeforeAll
    public static void setupAll() throws Exception {
        XJson.setLimitDecimalPlaces(3);
        var mapper = new JsonMapper();
        mapper.getFactory()
                .createParser(new FileReader("testset/results.json"))
                .readValuesAs(JsonNode.class)
                .forEachRemaining(dopeResults::add);
        dopeResults.forEach(System.out::println);

        var out = new ByteArrayOutputStream();
        new DeepObjectPoseEstimationApp(
                        CommandOptions.collectOptions(
                                new String[] {
                                    "-imagePath=" + imagePath,
                                    "-objectSize=4.947199821472168,2.9923000335693359,8.3498001098632812",
                                    "-cache=true",
                                    "-cacheFolder=_cache",
                                    "-cameraInfo=../config/camera_info.yaml"
                                }),
                        new PrintStream(out))
                .run();

        mapper.getFactory()
                .createParser(out.toByteArray())
                .readValuesAs(JsonNode.class)
                .forEachRemaining(dope4jResults::add);
        dope4jResults.forEach(System.out::println);
    }

    static Stream<Path> testDataProvider() throws IOException {
        return Files.list(imagePath).filter(FilePredicates.anyExtensionOf("jpg"));
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    public void test(Path image) throws Exception {
        JsonNode expected = findJsonNode(dopeResults, image);
        JsonNode actual = findJsonNode(dope4jResults, image);
        Assertions.assertEquals(expected.size(), actual.size());
        assertPoses(expected, actual);
    }

    private void assertPoses(JsonNode expected, JsonNode actual) {
        var expectedPoses = expected.get("detectedPoses").findValue("poses");
        var actualPoses = actual.get("detectedPoses").findValue("poses");
        for (int i = 0; i < expectedPoses.size(); i++) {
            Assertions.assertEquals(
                    expectedPoses.get(i).get("position").toString(),
                    actualPoses.get(i).get("position").toString());
        }
    }

    /** Finds results for current test image */
    private JsonNode findJsonNode(List<JsonNode> jsonNodes, Path image) {
        Predicate<JsonNode> resultFinder =
                n ->
                        n.findValuesAsText("imagePath").stream()
                                .map(Paths::get)
                                .map(Path::getFileName)
                                .filter(Predicate.isEqual(image.getFileName()))
                                .findFirst()
                                .isPresent();
        return StreamSupport.stream(jsonNodes.spliterator(), false)
                .filter(resultFinder)
                .findFirst()
                .orElseThrow();
    }
}
