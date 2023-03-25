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

import id.dope4j.io.OutputPoses;
import id.xfunctiontests.XAsserts;
import org.junit.jupiter.api.Assertions;

public class TestUtils {
    private static final double POSE_DELTA = 0.0999;

    public static void assertPoses(OutputPoses expected, OutputPoses actual) {
        var expectedPoses = expected.poses();
        var actualPoses = actual.poses();
        Assertions.assertEquals(expectedPoses.size(), actualPoses.size());
        System.out.println("Expected: " + expectedPoses);
        System.out.println("Actual: " + actualPoses);
        for (int i = 0; i < expectedPoses.size(); i++) {
            var expectedPose = expectedPoses.get(i).position();
            var actualPose = actualPoses.get(i).position();
            XAsserts.assertSimilar(expectedPose.getX(), actualPose.getX(), POSE_DELTA);
            XAsserts.assertSimilar(expectedPose.getY(), actualPose.getY(), POSE_DELTA);
            XAsserts.assertSimilar(expectedPose.getZ(), actualPose.getZ(), POSE_DELTA);
        }
    }
}
