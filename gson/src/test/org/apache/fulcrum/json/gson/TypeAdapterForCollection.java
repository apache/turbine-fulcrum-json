package org.apache.fulcrum.json.gson;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.fulcrum.json.Rectangle;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class TypeAdapterForCollection extends TypeAdapter<List<Rectangle>> {
    @Override
    public void write(JsonWriter out, List<Rectangle> data) throws IOException {
        out.beginObject();
        for (int i = 0; i < data.size(); i++) {
            out.name(data.get(i).getName());
            out.value(data.get(i).getSize());
        }
        out.endObject();
    }

    @Override
    public List<Rectangle> read(JsonReader in) throws IOException {
        ArrayList<Rectangle> list = new ArrayList<Rectangle>();
        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            int size = in.nextInt();// this is the size! as expected it is just
                                    // the square -> extracts the square root
            int value = (int) Math.sqrt(size);
            list.add(new Rectangle(value, value, name));
        }
        in.endObject();
        return list;
    }

}
