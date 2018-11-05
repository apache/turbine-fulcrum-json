package org.apache.fulcrum.json.jackson;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class CustomModule<T> extends SimpleModule {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiate a custom module
     * 
     * @param name Name of module
     * @param targetClazz The target class
     * @param stdSer Standard serializer
     * @param stdDeser  Standard de-serializer
     */
    public CustomModule(String name, Class<T> targetClazz,
            StdSerializer<T> stdSer, StdDeserializer<T> stdDeser) {
        super(name, Version.unknownVersion());
        addSerializer(targetClazz, stdSer);
        addDeserializer(targetClazz, stdDeser);
    }
}
