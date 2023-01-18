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
package com.fasterxml.jackson.databind.impl;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Remove when the following issue is resolved: <a
 * href="https://github.com/FasterXML/jackson-databind/issues/2992">Properties naming strategy do
 * not work with Record #2992</a>
 */
public class RecordNamingStrategyPatchModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.addValueInstantiators(new ValueInstantiatorsModifier());
        super.setupModule(context);
    }

    private static class ValueInstantiatorsModifier extends ValueInstantiators.Base {
        @Override
        public ValueInstantiator findValueInstantiator(
                DeserializationConfig config,
                BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator) {
            if (!beanDesc.getBeanClass().isRecord()
                    || !(defaultInstantiator instanceof StdValueInstantiator)
                    || !defaultInstantiator.canCreateFromObjectWith()) {
                return defaultInstantiator;
            }
            Map<String, BeanPropertyDefinition> map =
                    beanDesc.findProperties().stream()
                            .collect(
                                    Collectors.toMap(
                                            p -> p.getInternalName(), Function.identity()));
            SettableBeanProperty[] renamedConstructorArgs =
                    Arrays.stream(defaultInstantiator.getFromObjectArguments(config))
                            .map(
                                    p -> {
                                        BeanPropertyDefinition prop = map.get(p.getName());
                                        return prop != null ? p.withName(prop.getFullName()) : p;
                                    })
                            .toArray(SettableBeanProperty[]::new);

            return new PatchedValueInstantiator(
                    (StdValueInstantiator) defaultInstantiator, renamedConstructorArgs);
        }
    }

    private static class PatchedValueInstantiator extends StdValueInstantiator {

        protected PatchedValueInstantiator(
                StdValueInstantiator src, SettableBeanProperty[] constructorArguments) {
            super(src);
            _constructorArguments = constructorArguments;
        }
    }
}
