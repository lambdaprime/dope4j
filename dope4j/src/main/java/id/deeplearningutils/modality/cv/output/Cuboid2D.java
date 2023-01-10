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
    private List<Point> vertices;
    private Point center;
    private Point v1;
    private Point v2;
    private Point v3;
    private Point v4;
    private Point v5;
    private Point v6;
    private Point v7;
    private Point v8;

    public Cuboid2D(
            Point center,
            Point v1,
            Point v2,
            Point v3,
            Point v4,
            Point v5,
            Point v6,
            Point v7,
            Point v8) {
        this(center, List.of(v1, v2, v3, v4, v5, v6, v7, v8));
    }

    public Cuboid2D(Point center, List<? extends Point> vertices) {
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
    }

    public List<Point> vertices() {
        return vertices;
    }

    public List<Point> getVertices() {
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
