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
package id.dope4j.impl;

import ai.djl.modality.cv.output.Point;
import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Point2D;
import id.deeplearningutils.modality.cv.output.Point3D;
import id.xfunction.Preconditions;
import java.util.ArrayList;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.utils.Converters;

public class DjlOpenCvConverters {

    public org.opencv.core.Point copyToPoint(Point p) {
        return new org.opencv.core.Point(p.getX(), p.getY());
    }

    public org.opencv.core.Point copyToPoint(Point p, double scale) {
        return new org.opencv.core.Point(p.getX() * scale, p.getY() * scale);
    }

    public org.opencv.core.Point copyToPoint(Point3D p) {
        return copyToPoint(p, 1);
    }

    public org.opencv.core.Point copyToPoint(Point3D p, double scale) {
        return new org.opencv.core.Point(p.getX() * scale, p.getY() * scale);
    }

    public Point2D copyToPoint(org.opencv.core.Point p) {
        return new Point2D(p.x, p.y);
    }

    /**
     * Points are gathered in strict order defined in {@link Cuboid2D}. Only non null points are
     * copied. Center point is copied the last one.
     */
    public MatOfPoint2f copyToMatOfPoint2f(Cuboid2D cuboid2d) {
        return copyToMatOfPoint2f(cuboid2d, 1);
    }

    /**
     * @see #copyToMatOfPoint2f(Cuboid2D)
     */
    public MatOfPoint2f copyToMatOfPoint2f(Cuboid2D cuboid2d, double scale) {
        var rows = cuboid2d.getAvailableVertexCount() + 1;
        var ar = new float[2 * rows];
        int c = 0;
        for (var p : cuboid2d.getVertices()) {
            if (p == null) continue;
            var j = c * 2;
            ar[j] = (float) (p.getX() * scale);
            ar[j + 1] = (float) (p.getY() * scale);
            c++;
        }
        c *= 2;
        ar[c] = (float) (cuboid2d.getCenter().getX() * scale);
        ar[c + 1] = (float) (cuboid2d.getCenter().getY() * scale);
        return new MatOfPoint2f(new MatOfFloat(ar).reshape(2, rows));
    }

    /**
     * Points are gathered in strict order defined in {@link Cuboid3D}. Only non null points are
     * copied. Center point is copied the last one.
     */
    public MatOfPoint3f copyToMatOfPoint3f(Cuboid3D cuboid3d) {
        return copyToMatOfPoint3f(cuboid3d, 1);
    }

    /**
     * @see #copyToMatOfPoint3f(Cuboid3D)
     */
    public MatOfPoint3f copyToMatOfPoint3f(Cuboid3D cuboid3d, double scale) {
        var rows = cuboid3d.getAvailableVertexCount() + 1;
        var ar = new float[3 * rows];
        int c = 0;
        for (var p : cuboid3d.getVertices()) {
            if (p == null) continue;
            var j = c * 3;
            ar[j] = (float) p.getX();
            ar[j + 1] = (float) p.getY();
            ar[j + 2] = (float) p.getZ();
            c++;
        }
        c *= 3;
        ar[c] = (float) (cuboid3d.getCenter().getX() * scale);
        ar[c + 1] = (float) (cuboid3d.getCenter().getY() * scale);
        ar[c + 2] = (float) (cuboid3d.getCenter().getZ() * scale);
        return new MatOfPoint3f(new MatOfFloat(ar).reshape(3, rows));
    }

    /** Input vertices should be ordered as defined in {@link Cuboid2D} */
    public Cuboid2D copyToCuboid2D(MatOfPoint2f cuboid2d) {
        var cvVertices = new ArrayList<org.opencv.core.Point>(cuboid2d.rows());
        Converters.Mat_to_vector_Point(cuboid2d, cvVertices);
        var vertices = cvVertices.stream().map(this::copyToPoint).toList();
        Preconditions.equals(9, vertices.size(), "Received wrong number of vertices");
        return new Cuboid2D(vertices.get(8), vertices.subList(0, 8));
    }

    public Point3D copyToPoint(Mat matrix) {
        Preconditions.equals(3, matrix.rows(), "Point matrix has wrong number of rows");
        Preconditions.equals(1, matrix.cols(), "Point matrix has wrong number of cols");
        var data = new double[3];
        matrix.get(0, 0, data);
        return new Point3D(data[0], data[1], data[2]);
    }
}
