package org.apache.fulcrum.json.jackson.mixins;

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


import org.apache.fulcrum.json.jackson.example.Bean;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(allowGetters = true)
public class BeanMixin3 extends Bean {
    
    public BeanMixin3() {
    }

    @Override
    @JsonProperty("name")
    @JsonGetter("bean.name[0]")
    @JsonSetter("name")
    public String getName() {
        return super.getName();
    }
    
    @JsonProperty("profession")
    @JsonGetter("bean.profession[0]")
    public String getProfession() {
        return super.profession;
    }
    
    @Override
    public String toString()
    {
        return "BeanMixin3 { name: "+ getName() + ", age: " + getAge()+ ", " + super.toString() + " }";
    }
}

