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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class CacheService implements LogEnabled {

    AnnotationIntrospector primary;
    Map<String, FilterProvider> filters =  new ConcurrentHashMap<String, FilterProvider>();;
    
    private static Logger logger;
    
    public CacheService(AnnotationIntrospector primary) {
        this.primary = primary;
    }

    <T> void removeFilter(Class<T> filterClass, Boolean excludeType) {
        if (filterClass == null)
            return;
        if (filters.containsKey(filterClass.getName())) {
            logger.debug("removing filter: " + filterClass.getName());
            removeCustomIntrospectorWithExternalFilterId(filterClass, excludeType);
            SimpleFilterProvider smpfilter = (SimpleFilterProvider) filters
                    .get(filterClass.getName());
            smpfilter.removeFilter(filterClass.getName());
            filters.remove(filterClass.getName());
        }
    }
    
    <T> void removeCustomIntrospectorWithExternalFilterId(
            Class<T> externalFilterId, Boolean excludeType) {
        if (primary instanceof SimpleNameIntrospector) {
            if (externalFilterId != null) {
                ((SimpleNameIntrospector) primary)
                        .removeFilteredClass(externalFilterId);
                if (excludeType) {
                    ((SimpleNameIntrospector) primary)
                    .removeExternalFilterExcludeClass(externalFilterId);
                }
            }
        }
    }

    void cleanSerializerCache(ObjectMapper mapper) {
        if (mapper.getSerializerProvider() instanceof DefaultSerializerProvider) {
            int cachedSerProvs = ((DefaultSerializerProvider) mapper
                    .getSerializerProvider()).cachedSerializersCount();
            if (cachedSerProvs > 0) {
//                getLogger()
//                        .debug("flushing cachedSerializersCount:"
//                                + cachedSerProvs);
                ((DefaultSerializerProvider) mapper.getSerializerProvider())
                        .flushCachedSerializers();
            }
        }
    }

    public Map<String, FilterProvider> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, FilterProvider> filters) {
        this.filters = filters;
    }

    @Override
    public void enableLogging(Logger logger) {
        this.logger = logger;        
    }

}
