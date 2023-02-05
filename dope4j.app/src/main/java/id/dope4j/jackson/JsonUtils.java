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
package id.dope4j.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.impl.RecordNamingStrategyPatchModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import id.deeplearningutils.modality.cv.output.Cuboid2D;
import id.deeplearningutils.modality.cv.output.Cuboid3D;
import id.dope4j.app.Dope4jResult;
import id.dope4j.jackson.mixin.Cuboid2DJson;
import id.dope4j.jackson.mixin.Cuboid3DJson;
import id.matcv.camera.CameraInfo;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {

    private ObjectReader reader;
    private ObjectReader cameraReader;

    public JsonUtils() {
        reader =
                new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .addMixIn(Cuboid2D.class, Cuboid2DJson.class)
                        .addMixIn(Cuboid3D.class, Cuboid3DJson.class)
                        .readerFor(Dope4jResult.class);
        cameraReader =
                new YAMLMapper()
                        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .registerModule(new RecordNamingStrategyPatchModule())
                        .registerModule(new ParameterNamesModule())
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .reader();
    }

    public List<Dope4jResult> readDope4jResults(Path path) {
        try {
            var results = new ArrayList<Dope4jResult>();
            reader.<Dope4jResult>readValues(new FileReader(path.toFile()))
                    .forEachRemaining(results::add);
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dope4jResult> readDope4jResults(byte[] data) {
        try {
            var results = new ArrayList<Dope4jResult>();
            reader.<Dope4jResult>readValues(data).forEachRemaining(results::add);
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CameraInfo readCameraInfo(Path path) {
        try {
            return cameraReader.readValue(path.toFile(), CameraInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
