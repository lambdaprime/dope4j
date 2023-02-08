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

import id.deeplearningutils.modality.cv.output.Point2D;
import id.dope4j.decoders.CuboidVertexMatcher;
import id.dope4j.decoders.CuboidVertexMatcher.VectorField;
import id.mathcalc.Vector2f;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CuboidVertexMatcherTest {

    @Test
    public void test_match() {
        var centerPoints = List.of(new Point2D(25, 25), new Point2D(65, 65));
        var verticesLists =
                List.<List<Point2D>>of(
                        List.of(),
                        List.of(new Point2D(21, 21), new Point2D(22, 22), new Point2D(65, 61)),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new Point2D(30, 30), new Point2D(60, 60)));
        var vectorField =
                new VectorField() {
                    @Override
                    public Vector2f get(int cuboidVertexId, Point2D vertex) {
                        return new Vector2f(1, 1);
                    }
                };
        var actual = new CuboidVertexMatcher(centerPoints, verticesLists, vectorField).match();
        Assertions.assertEquals(
                """
                {{ "x": 25, "y": 25 }=[null, { "x": 22, "y": 22 }, null, null, null, null, null, { "x": 30, "y": 30 }], { "x": 65, "y": 65 }=[null, { "x": 65, "y": 61 }, null, null, null, null, null, { "x": 60, "y": 60 }]}
                """
                        .stripTrailing(),
                actual.toString());
    }
}
