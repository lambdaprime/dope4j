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
package id.deeplearningutils.modality.cv.output;

import ai.djl.modality.cv.output.Point;
import id.xfunction.Preconditions;
import id.xfunction.XJsonStringBuilder;
import java.util.List;
import java.util.Objects;

/**
 * Describes <a href="https://en.wikipedia.org/wiki/Cuboid">cuboid</a> in 2D.
 *
 * <p>It differs from Bounding Box in 2D with number of vertices. Cuboid has 8 vertices instead of
 * 4.
 *
 * <p>Vertex naming is the following:
 *
 * <pre>{@code
 * y
 * ^
 * |    5--------6
 * |   /|       /|
 * |  / |      / |
 * | 1--|-----2  |
 * | |  8---- |--7
 * | | /      | /
 * | |/       |/
 * | 4--------3
 * |-----------------> x
 * }</pre>
 *
 * @author lambdaprime intid@protonmail.com
 */
public class Cuboid2D {
    private List<Point2D> vertices;
    private Point2D center;
    private Point2D v1;
    private Point2D v2;
    private Point2D v3;
    private Point2D v4;
    private Point2D v5;
    private Point2D v6;
    private Point2D v7;
    private Point2D v8;
    private List<Edge2D> edges;

    public Cuboid2D(
            Point2D center,
            Point2D v1,
            Point2D v2,
            Point2D v3,
            Point2D v4,
            Point2D v5,
            Point2D v6,
            Point2D v7,
            Point2D v8) {
        this(center, List.of(v1, v2, v3, v4, v5, v6, v7, v8));
    }

    public Cuboid2D(Point2D center, List<Point2D> vertices) {
        Preconditions.equals(8, vertices.size(), "Cuboid requires 8 vertices");
        this.center = center;
        this.v1 = vertices.get(0);
        this.v2 = vertices.get(1);
        this.v3 = vertices.get(2);
        this.v4 = vertices.get(3);
        this.v5 = vertices.get(4);
        this.v6 = vertices.get(5);
        this.v7 = vertices.get(6);
        this.v8 = vertices.get(7);
        this.vertices = List.copyOf(vertices);
        this.edges =
                List.of(
                        new Edge2D(v1, v2),
                        new Edge2D(v2, v3),
                        new Edge2D(v3, v4),
                        new Edge2D(v4, v1),
                        new Edge2D(v5, v6),
                        new Edge2D(v6, v7),
                        new Edge2D(v7, v8),
                        new Edge2D(v8, v5),
                        new Edge2D(v1, v5),
                        new Edge2D(v2, v6),
                        new Edge2D(v3, v7),
                        new Edge2D(v4, v8));
    }

    public List<Edge2D> getEdges() {
        return edges;
    }

    public List<Point2D> getVertices() {
        return vertices;
    }

    public Point getCenter() {
        return center;
    }

    public Point v1() {
        return v1;
    }

    public Point v2() {
        return v2;
    }

    public Point v3() {
        return v3;
    }

    public Point v4() {
        return v4;
    }

    public Point v5() {
        return v5;
    }

    public Point v6() {
        return v6;
    }

    public Point v7() {
        return v7;
    }

    public Point v8() {
        return v8;
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, vertices);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Cuboid2D other = (Cuboid2D) obj;
        return Objects.equals(center, other.center) && Objects.equals(vertices, other.vertices);
    }

    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("center", center);
        builder.append("vertices", vertices);
        return builder.toString();
    }
}
