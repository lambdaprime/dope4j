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
import id.xfunction.XJsonStringBuilder;
import java.util.List;

/**
 * @author lambdaprime intid@protonmail.com
 */
public record OutputObjects2D(List<? extends Cuboid2D> cuboids2d) {

    public static final OutputObjects2D EMPTY = new OutputObjects2D(List.of());

    public List<? extends Cuboid2D> cuboids2d() {
        return cuboids2d;
    }

    @Override
    public String toString() {
        var builder = new XJsonStringBuilder();
        builder.append("cuboids2d", cuboids2d());
        return builder.toString();
    }

    public Object size() {
        return cuboids2d.size();
    }
}
