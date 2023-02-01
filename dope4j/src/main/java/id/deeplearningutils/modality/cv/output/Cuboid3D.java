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

import id.xfunction.Preconditions;
import id.xfunction.XJsonStringBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Describes <a href="https://en.wikipedia.org/wiki/Cuboid">cuboid</a> in 3D.
 *
 * <p>It differs from Bounding Box in 2D with number of vertices. Cuboid has 8 vertices instead of
 * 4.
 *
 * <p>Cuboid may have some of the vertices missing. In this case values of such vertices is null.
 *
 * <p>Vertex naming is the following:
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
 *
 * @author lambdaprime intid@protonmail.com
 */
public class Cuboid3D {
    public static final int VERTEX_COUNT = 8;
    private List<Point3D> vertices;
    private Point3D center;
    private Point3D v1;
    private Point3D v2;
    private Point3D v3;
    private Point3D v4;
    private Point3D v5;
    private Point3D v6;
    private Point3D v7;
    private Point3D v8;
    private int availableVertexCount;
    private int missingVertexCount;

    public Cuboid3D(
            Point3D center,
            Point3D v1,
            Point3D v2,
            Point3D v3,
            Point3D v4,
            Point3D v5,
            Point3D v6,
            Point3D v7,
            Point3D v8) {
        this(center, List.of(v1, v2, v3, v4, v5, v6, v7, v8));
    }

    public Cuboid3D(Point3D center, List<? extends Point3D> vertices) {
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
        this.vertices = Collections.unmodifiableList(vertices);
        availableVertexCount =
                VERTEX_COUNT - (int) vertices.stream().filter(Predicate.isEqual(null)).count();
        missingVertexCount = VERTEX_COUNT - availableVertexCount;
    }

    public Cuboid3D(Point3D center, double width, double height, double length) {
        this(
                center,
                new Point3D(
                        center.getX() + width / 2,
                        center.getY() - height / 2,
                        center.getZ() + length / 2),
                new Point3D(
                        center.getX() - width / 2,
                        center.getY() - height / 2,
                        center.getZ() + length / 2),
                new Point3D(
                        center.getX() - width / 2,
                        center.getY() + height / 2,
                        center.getZ() + length / 2),
                new Point3D(
                        center.getX() + width / 2,
                        center.getY() + height / 2,
                        center.getZ() + length / 2),
                new Point3D(
                        center.getX() + width / 2,
                        center.getY() - height / 2,
                        center.getZ() - length / 2),
                new Point3D(
                        center.getX() - width / 2,
                        center.getY() - height / 2,
                        center.getZ() - length / 2),
                new Point3D(
                        center.getX() - width / 2,
                        center.getY() + height / 2,
                        center.getZ() - length / 2),
                new Point3D(
                        center.getX() + width / 2,
                        center.getY() + height / 2,
                        center.getZ() - length / 2));
    }

    public List<Point3D> vertices() {
        return vertices;
    }

    public List<Point3D> getVertices() {
        return vertices;
    }

    public Point3D getCenter() {
        return center;
    }

    public Point3D v1() {
        return v1;
    }

    public Point3D v2() {
        return v2;
    }

    public Point3D v3() {
        return v3;
    }

    public Point3D v4() {
        return v4;
    }

    public Point3D v5() {
        return v5;
    }

    public Point3D v6() {
        return v6;
    }

    public Point3D v7() {
        return v7;
    }

    public Point3D v8() {
        return v8;
    }

    public int getAvailableVertexCount() {
        return availableVertexCount;
    }

    public int getMissingVertexCount() {
        return missingVertexCount;
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
        Cuboid3D other = (Cuboid3D) obj;
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
