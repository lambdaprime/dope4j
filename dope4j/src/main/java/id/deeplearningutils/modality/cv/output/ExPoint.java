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
import java.util.Objects;

/**
 * Extension for {@link Point}
 *
 * @author lambdaprime intid@protonmail.com
 */
public class ExPoint extends Point {

    private static final long serialVersionUID = 1L;

    public ExPoint(double x, double y) {
        super(x, y);
    }

    public double distance(Point p) {
        var n1 = getX() - p.getX();
        var n2 = getY() - p.getY();
        return Math.sqrt(n1 * n1 + n2 * n2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        var other = (ExPoint) obj;
        return getX() == other.getX() && getY() == other.getY();
    }

    @Override
    public String toString() {
        return "[" + getX() + ", " + getY() + "]";
    }
}
