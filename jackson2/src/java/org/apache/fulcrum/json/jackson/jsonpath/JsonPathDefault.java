package org.apache.fulcrum.json.jackson.jsonpath;

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

import java.util.EnumSet;
import java.util.Set;

import org.apache.fulcrum.json.jackson.Jackson2MapperService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration.Defaults;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * Intermediary class implementing {@link Defaults} by glueing {@link Jackson2MapperService#getMapper()} with internal 
 * objects {@link JacksonJsonProvider} and {@link JacksonMappingProvider}.
 * 
 * @author gkallidis
 *
 */
public class JsonPathDefault implements
        com.jayway.jsonpath.Configuration.Defaults {

    private final JsonProvider jsonProvider;
    private final MappingProvider mappingProvider;

    // Jackson2MapperService.this.mapper
    public JsonPathDefault(ObjectMapper mapper) {
        jsonProvider = new JacksonJsonProvider(mapper);
        mappingProvider = new JacksonMappingProvider(  mapper);
    }


    @Override
    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    @Override
    public MappingProvider mappingProvider() {
        return mappingProvider;
    }

    @Override
    public Set<Option> options() {
        return EnumSet.noneOf(Option.class);
    }
}
