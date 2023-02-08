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
package id.dope4j.decoders;

import id.deeplearningutils.modality.cv.output.Point2D;
import id.mathcalc.Vector2f;
import id.xfunction.Preconditions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Matches cuboids center points with their vertices.
 *
 * <p>Returns map of pairs: [center point C: vertices], where vertices are all vertices from the
 * verticesLists and are part of the cuboid which matches to this center point C. For those cuboid
 * vertices which was not found because they does not exist in verticesLists we return null. This
 * guarantees that for each center point C we always will return list of 8 vertices but some
 * vertices in this list may be null.
 *
 * <p>Matching between cuboid center point and its vertex happens based on follow criteria:
 *
 * <ol>
 *   <li>distance between them
 *   <li>direction of the vertex as given by {@link VectorField} (currently it is ignored)
 * </ol>
 *
 * <p>Output example:
 *
 * <pre>{@code
 *       L1,  L2,  L3,   L4,  L5,  L6,   L7,  L8
 *
 * C1: null, v11, v12, null, v14, v15, null, v17
 * C2:  v20, v21, v22,  v23, v24, v25,  v26, v27
 * ...
 * CN: ...
 * }</pre>
 *
 * <p>Where CN is a number of center points in centerPoints list and L* are cuboid vertices:
 *
 * <pre>{@code
 * y
 * ^
 * |    7--------8
 * |   /|       /|
 * |  / |      / |
 * | 3--|-----4  |
 * | |  6---- |--5
 * | | /      | /
 * | |/       |/
 * | 2--------1
 * |-----------------> x
 * }</pre>
 */
public class CuboidVertexMatcher {

    @FunctionalInterface
    public static interface VectorField {
        Vector2f get(int cuboidVertexId, Point2D vertex);
    }

    private static final int VERTEX_COUNT = 8;
    private List<Point2D> centerPoints;
    private List<List<Point2D>> verticesLists;
    private VectorField vectorField;

    /**
     * @param centerPoints list of center points. Each center point describes single cuboid.
     * @param verticesLists there should be 8 lists where each list describes all candidates of a
     *     given cuboid vertex. Each V = verticesLists[i][j] will be matched with only one center
     *     point centerPoints[c] and its cuboid vertex with index i.
     * @param vectorField direction vectors of all vertices from verticesLists.
     */
    public CuboidVertexMatcher(
            List<Point2D> centerPoints,
            List<List<Point2D>> verticesLists,
            VectorField vectorField) {
        Preconditions.equals(8, verticesLists.size());
        this.centerPoints = centerPoints;
        this.verticesLists = verticesLists;
        this.vectorField = vectorField;
    }

    public Map<Point2D, List<Point2D>> match() {
        // cuboidsMap[i] = [all 8 vertices of a cuboid with center point centerPoints[i]]
        var cuboidsMap =
                Stream.generate(() -> new ArrayList<Point2D>(VERTEX_COUNT))
                        .limit(centerPoints.size())
                        .toList();
        record VertexCandidate(Point2D vertex, double distance) {}
        // candidatesMap[i] = [all candidate vertices of cuboid vertex vertexId with center point
        // centerPoints[i]]
        // candidatesMap is reused and cleaned up for each new vertexId
        var candidatesMap =
                Stream.generate(() -> new ArrayList<VertexCandidate>())
                        .limit(centerPoints.size())
                        .toList();
        for (int cuboidVertexId = 0; cuboidVertexId < VERTEX_COUNT; cuboidVertexId++) {
            var vertices = verticesLists.get(cuboidVertexId);
            for (var vertex : vertices) {
                double minDistance = Integer.MAX_VALUE;
                // point with the smallest distance to the current vertex
                int candidateCenterPointId = -1;
                for (int i = 0; i < centerPoints.size(); i++) {
                    var center = centerPoints.get(i);
                    var affinityVec = vectorField.get(cuboidVertexId, vertex).normalize();
                    var vec =
                            new Vector2f(
                                            (float) (center.getX() - vertex.getX()),
                                            (float) (center.getY() - vertex.getY()))
                                    .normalize();
                    var angle = affinityVec.sub(vec).norm();
                    var distance = vertex.distance(center);
                    if (distance < minDistance) {
                        candidateCenterPointId = i;
                        minDistance = distance;
                    }
                }
                if (candidateCenterPointId != -1)
                    candidatesMap
                            .get(candidateCenterPointId)
                            .add(new VertexCandidate(vertex, minDistance));
            }
            for (int i = 0; i < centerPoints.size(); i++) {
                var candidates = candidatesMap.get(i);
                if (candidates.isEmpty()) {
                    // for missing vertices of a cuboid we add nulls
                    cuboidsMap.get(i).add(null);
                } else {
                    // from all found candidates of current cuboidVertexId we chose the one with
                    // smallest distance to the center point
                    var best = candidates.get(0);
                    for (var c : candidates) {
                        if (c.distance < best.distance) best = c;
                    }
                    cuboidsMap.get(i).add(best.vertex);
                }
                candidatesMap.get(i).clear();
            }
        }
        return IntStream.range(0, cuboidsMap.size())
                .boxed()
                .collect(
                        Collectors.toMap(
                                i -> centerPoints.get(i),
                                i -> cuboidsMap.get(i),
                                (m1, m2) -> {
                                    throw new RuntimeException(
                                            "Collision between center point indices");
                                },
                                () -> new LinkedHashMap<>(centerPoints.size())));
    }
}
