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
package id.dope4j;

import id.dope4j.impl.CacheFileMapper;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CacheFileMapperTest {

    @Test
    public void test() {
        Assertions.assertEquals(
                "/tmp/_cache/4/5.tensor",
                new CacheFileMapper(Paths.get("/1/2/3"), Paths.get("/tmp/_cache"))
                        .getTensorFile(Paths.get("4/5"))
                        .toString());
        Assertions.assertEquals(
                "/tmp/_cache/4/5.tensor",
                new CacheFileMapper(Paths.get("/1/2/3"), Paths.get("/tmp/_cache"))
                        .getTensorFile(Paths.get("/1/2/3/4/5"))
                        .toString());
        Assertions.assertEquals(
                "/tmp/_cache/1/2/3/4/5.tensor",
                new CacheFileMapper(Paths.get("/1/2/3"), Paths.get("/tmp/_cache"))
                        .getTensorFile(Paths.get("/wrong/1/2/3/4/5"))
                        .toString());
    }
}
