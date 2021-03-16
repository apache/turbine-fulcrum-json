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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;


@JsonTypeInfo(include=As.PROPERTY, use=Id.CLASS, property="type")
public final class TypedRectangle {

// This is only need if no DefaultTyping is set; you have then assign this to object if using collections
    @JsonTypeInfo(use = Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
    public static class Mixins {    }
    
    private int w, h;
    private String name;
 
    public TypedRectangle() {
        // may be is needed for deserialization, if not set otherwise
    }
    
    public TypedRectangle(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public TypedRectangle(int w, int h, String name) {
        this.w = w;
        this.h = h;
        this.name = name;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public int getSize() {
        return w * h;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}