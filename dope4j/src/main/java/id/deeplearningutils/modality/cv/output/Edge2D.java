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
package id.deeplearningutils.modality.cv.output;

import ai.djl.modality.cv.output.Point;
import id.xfunction.XJsonStringBuilder;
import java.util.Objects;

/**
 * Extension for {@link Point}
 *
 * @author lambdaprime intid@protonmail.com
 */
public class Edge2D {

    private Point2D a;
    private Point2D b;

    public Edge2D(Point2D a, Point2D b) {
        boolean swap = Point2D.COMPARATOR.compare(a, b) > 0;
        this.a = swap ? b : a;
        this.b = swap ? a : b;
    }

    public Point2D getPointA() {
        return a;
    }

    public Point2D getPointB() {
        return b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        var other = (Edge2D) obj;
        return Objects.equals(a, other.a) && Objects.equals(b, other.b);
    }

    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("a", a);
        builder.append("b", b);
        return builder.toString();
    }
}
