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
package id.dope4j.io;

import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.deeplearningutils.modality.cv.output.Pose;
import id.xfunction.Preconditions;
import id.xfunction.XJsonStringBuilder;
import java.util.List;

/**
 * Detected objects with their poses.
 *
 * <p>It contains results for all detected object instances which belong to the same object class
 * (Cookies, ChocolatePudding etc).
 *
 * @author lambdaprime intid@protonmail.com
 */
public record OutputPoses(Cuboid3D object3d, List<? extends Cuboid2D> objects2d, List<Pose> poses) {

    public OutputPoses {
        Preconditions.equals(
                objects2d.size(), poses.size(), "Mismatch between number of 2D objects and poses");
    }

    /**
     * List of cuboids for all detected objects in the image
     *
     * <p>Each cuboid represents its 3D model {@link #object3d} projected on the object detected on
     * the input 2D image.
     */
    public List<? extends Cuboid2D> objects2d() {
        return objects2d;
    }

    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("object3d", object3d());
        builder.append("objects2d", objects2d());
        builder.append("poses", poses());
        return builder.toString();
    }
}
