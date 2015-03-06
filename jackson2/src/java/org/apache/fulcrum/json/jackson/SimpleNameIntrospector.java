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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * The intent of this custom introspector is to provide filtering capabilities
 * by using String parameters (properties and class types), which could be
 * adjusted e.g. from a scriptable context (velocity template).
 * 
 * 
 * @author gk
 * @version $Id$
 * 
 */
public class SimpleNameIntrospector extends NopAnnotationIntrospector {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public List<String> externalFilterClasses = new CopyOnWriteArrayList<String>();
    public List<String> externalFilterExcludeClasses = new CopyOnWriteArrayList<String>();

    /**
     * Filtering on method types
     * 
     */
    @Override
    public Boolean isIgnorableType(AnnotatedClass ac) {
        Boolean isIgnorable = super.isIgnorableType(ac);
        if (isIgnorable == null || !isIgnorable) {
            if (!externalFilterExcludeClasses.isEmpty()
                    && externalFilterExcludeClasses.contains(ac.getName())) {
                isIgnorable = true;
            }
        }
        return isIgnorable;
    }
    /**
     * @return Object Filtering on properties returns an object, if
     *         {@link #externalFilterClasses} contains the class provided. The
     *         filter itself currently is {@link SimpleFilterProvider}.
     */
    @Override
    public Object findFilterId(Annotated ac) {
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
    
    public void setExternalFilterExcludeClasses(Class... classes) {

        for (int i = 0; i < classes.length; i++) {
            if (!externalFilterExcludeClasses.contains(classes[i].getName())) {

                externalFilterExcludeClasses.add(classes[i].getName());
            }
        }
    }
    
    public void removeExternalFilterExcludeClass(Class externalFilterClass) {
        if (externalFilterExcludeClasses.contains(externalFilterClass.getName())) {
            externalFilterExcludeClasses.remove(externalFilterClass.getName());
        }
}

}