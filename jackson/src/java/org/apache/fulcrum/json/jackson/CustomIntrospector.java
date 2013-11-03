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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

public class CustomIntrospector extends JacksonAnnotationIntrospector {

    public List<String> externalFilterClasses = Collections
            .synchronizedList(new ArrayList<String>());

    @Override
    public Object findFilterId(AnnotatedClass ac) {
        // Let's default to current behavior if annotation is found:
        Object id = super.findFilterId(ac);
        // but use simple class name if not
        if (id == null) {
            String name = ac.getName();
            if (!externalFilterClasses.isEmpty()
                    && externalFilterClasses.contains(name)) {
                id = name;
            }
        }
        return id;
    }

    public List<String> getExternalFilterClasses() {
        return externalFilterClasses;
    }

    public void setExternalFilterClass(Class externalFilterClass) {
        if (!externalFilterClasses.contains(externalFilterClass.getName())) {
            externalFilterClasses.add(externalFilterClass.getName());
        }
    }

    public void setExternalFilterClasses(Class... classes) {

        for (int i = 0; i < classes.length; i++) {
            if (!externalFilterClasses.contains(classes[i].getName())) {

                externalFilterClasses.add(classes[i].getName());
            }
        }
    }

    public void removeExternalFilterClass(Class externalFilterClass) {
        if (externalFilterClasses.contains(externalFilterClass.getName())) {
            externalFilterClasses.remove(externalFilterClass.getName());
        }
    }

}