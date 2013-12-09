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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.fulcrum.json.JsonService;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.ser.impl.SimpleBeanPropertyFilter;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;

/**
 * Jackson 1 Impl of @link {@link JsonService}.
 * 
 * By default multiple serialization of the same object in a single thread is
 * not support (e.g filter + mixin or default + filter for the same bean /
 * object).
 * 
 * Note: Filters could not easily unregistered. Try setting @link
 * {@link #cacheFilters} to <code>false</code>.
 * 
 * 
 * @author gk
 * @version
 * 
 */
public class JacksonMapperService extends AbstractLogEnabled implements
        JsonService, Initializable, Configurable {

    private static final String DEFAULT_TYPING = "defaultTyping";
    private static final String CACHE_FILTERS = "cacheFilters";
    private static final String DATE_FORMAT = "dateFormat";
    ObjectMapper mapper;
    AnnotationIntrospector primary; // support default
    AnnotationIntrospector secondary;

    public String ANNOTATIONINSPECTOR = "annotationInspectors";

    private Hashtable<String, String> annotationInspectors = null;
    private Hashtable<String, Boolean> features = null;

    private Map<String, FilterProvider> filters;
    private String dateFormat;

    final String DEFAULTDATEFORMAT = "MM/dd/yyyy";

    final boolean defaultType = false;
    public boolean cacheFilters = true; // more efficient if not using multiple
                                        // serialization in one thread
    String[] defaultTypeDefs = null;

    @Override
    public synchronized String ser(Object src) throws Exception {
        if (filters.containsKey(src.getClass().getName())) {
            getLogger().warn(
                    "Found registered filter - using instead of default view filter for class:"
                            + src.getClass().getName());
            // throw new
            // Exception("Found registered filter - could not use custom view and custom filter for class:"+
            // src.getClass().getName());
        }
        return mapper.writer().writeValueAsString(src);
    }

    @Override
    public <T> String ser(Object src, Class<T> type) throws Exception {
        getLogger().debug("ser::" + src + " with type" + type);
        if (filters.containsKey(src.getClass().getName())) {
            getLogger()
                    .warn("Found registered filter - could not use custom view and custom filter for class:"
                            + src.getClass().getName());
            // throw new
            // Exception("Found registered filter - could not use custom view and custom filter for class:"+
            // src.getClass().getName());
        }
        return mapper.writerWithView(type).writeValueAsString(src);
    }

    public synchronized <T> String ser(Object src, FilterProvider filters)
            throws Exception {
        getLogger().debug("ser::" + src + " with filters " + filters);
        String serResult = mapper.writer(filters).writeValueAsString(src);
        return serResult;
    }

    @Override
    public <T> T deSer(String src, Class<T> type) throws Exception {
        ObjectReader reader = mapper.reader(type);
        return reader.readValue(src);
    }
    
    @Override
    public <T> Collection<T> deSerCollection(String json,
            Object collectionType, Class<T> elementType) throws Exception {
        return mapper.readValue(json, mapper.getTypeFactory()
                .constructCollectionType(((Collection<T>)collectionType).getClass(), elementType));
    }

    public <T> T deSer(String json, Class<? extends Collection> collectionType,
            Class<T> type) throws Exception {
        return mapper.readValue(json, mapper.getTypeFactory()
                .constructCollectionType(collectionType, type));
    }

    @Override
    public synchronized <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception {
        return serializeAllExceptFilter(src, filterClass, false, filterAttr);
    }
    
    @Override
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, Boolean refreshFilter, String... filterAttr)
            throws Exception {
        setCustomIntrospectorWithExternalFilterId(filterClass);
        FilterProvider filter;
        if (!this.filters.containsKey(filterClass.getName())) {
            filter = new SimpleFilterProvider().addFilter(
                    filterClass.getName(),
                    SimpleBeanPropertyFilter.serializeAllExcept(filterAttr));
            this.filters.put(filterClass.getName(), filter);
        } else {
            filter = this.filters.get(filterClass.getName());
        }
        String serialized = ser(src, filter);
        if (!cacheFilters)
            removeFilterClass(filterClass);
        return serialized;
    }

    @Override
    public synchronized <T> String serializeOnlyFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception {
        return serializeOnlyFilter(src, filterClass, false, filterAttr);
    }
    
    @Override
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass,
            Boolean refreshFilter, String... filterAttr) throws Exception {
        setCustomIntrospectorWithExternalFilterId(filterClass);
        FilterProvider filter;
        if (!this.filters.containsKey(filterClass.getName())) {
            filter = new SimpleFilterProvider().addFilter(
                    filterClass.getName(),
                    SimpleBeanPropertyFilter.filterOutAllExcept(filterAttr));
            this.filters.put(filterClass.getName(), filter);
        } else {
            filter = this.filters.get(filterClass.getName());
        }
        String serialized = ser(src, filter);
        getLogger().debug("serialized " + serialized);
        if (!cacheFilters || refreshFilter)
            removeFilterClass(filterClass);
        return serialized;
    }

    private <T> void removeFilterClass(Class<T> filterClass) {
        if (this.filters.containsKey(filterClass.getName())) {
            removeCustomIntrospectorWithExternalFilterId(filterClass);
            this.filters.remove(filterClass.getName());
            mapper.getSerializerProvider().flushCachedSerializers();
            getLogger().debug(
                    "removed from  SimpleFilterProvider filters "
                            + filterClass.getName());
        }
    }

    private <T> void setCustomIntrospectorWithExternalFilterId(
            Class<T> externalFilterId) {
        if (primary instanceof CustomIntrospector) {
            if (externalFilterId != null) {
                ((CustomIntrospector) primary)
                        .setExternalFilterClasses(externalFilterId);
                getLogger().debug(
                        "added class from filters "
                                + externalFilterId.getName());
            }
        }
    }

    private <T> void removeCustomIntrospectorWithExternalFilterId(
            Class<T> externalFilterId) {
        if (primary instanceof CustomIntrospector) {
            if (externalFilterId != null) {
                ((CustomIntrospector) primary)
                        .removeExternalFilterClass(externalFilterId);
                getLogger().debug(
                        "removed from introspector filter id  "
                                + externalFilterId.getName());
            }
        }
    }

    public JacksonMapperService registerModule(Module module) {
        mapper.withModule(module);
        return this;
    }

    public <T> void addSimpleModule(SimpleModule module, Class<T> type,
            JsonSerializer<T> ser) {
        module.addSerializer(type, ser);
    }

    public <T> void addSimpleModule(SimpleModule module, Class<T> type,
            JsonDeserializer<T> deSer) {
        module.addDeserializer(type, deSer);
    }

    @Override
    public void setDateFormat(final DateFormat df) {
        mapper.setDateFormat(df);
    }

    @Override
    public JsonService addAdapter(String name, Class target, Object mixin)
            throws Exception {
        return addAdapter(name, target, mixin.getClass());
    }

    @Override
    public JsonService addAdapter(String name, Class target, Class mixin)
            throws Exception {
        Module mx = new MixinModule(name, target, mixin);
        getLogger().debug("registering module " + mx + "  for: " + mixin);
        mapper.withModule(mx);
        return this;
    }

    /**
     * Avalon component lifecycle method
     */
    @Override
    public void configure(Configuration conf) throws ConfigurationException {
        getLogger().debug("conf.getName()" + conf.getName());
        this.annotationInspectors = new Hashtable<String, String>();

        final Configuration configuredAnnotationInspectors = conf.getChild(
                ANNOTATIONINSPECTOR, false);
        if (configuredAnnotationInspectors != null) {
            Configuration[] nameVal = configuredAnnotationInspectors
                    .getChildren();
            for (int i = 0; i < nameVal.length; i++) {
                String key = nameVal[i].getName();
                getLogger().debug("configured key: " + key);
                if (key.equals("features")) {
                    this.features = new Hashtable<String, Boolean>();
                    Configuration[] features = nameVal[i].getChildren();
                    for (int j = 0; j < features.length; j++) {
                        boolean featureValue = features[j]
                                .getAttributeAsBoolean("value", false);
                        String feature = features[j].getValue();
                        getLogger().debug(
                                "configuredAnnotationInspectors " + feature
                                        + ":" + featureValue);
                        this.features.put(feature, featureValue);
                    }
                } else {
                    String val = nameVal[i].getValue();
                    getLogger()
                            .debug("configuredAnnotationInspectors " + key
                                    + ":" + val);
                    this.annotationInspectors.put(key, val);
                }
            }
        }
        final Configuration configuredDateFormat = conf.getChild(DATE_FORMAT,
                true);
        this.dateFormat = configuredDateFormat.getValue(DEFAULTDATEFORMAT);

        final Configuration configuredKeepFilter = conf.getChild(CACHE_FILTERS,
                false);
        if (configuredKeepFilter != null) {
            this.cacheFilters = configuredKeepFilter.getValueAsBoolean();
        }
        final Configuration configuredDefaultType = conf.getChild(
                DEFAULT_TYPING, false);
        if (configuredDefaultType != null) {
            defaultTypeDefs = new String[] {
                    configuredDefaultType.getAttribute("type"),
                    configuredDefaultType.getAttribute("key") };
        }
    }

    @Override
    public void initialize() throws Exception {
        mapper = new ObjectMapper();

        Enumeration<String> enumKey = annotationInspectors.keys();
        while (enumKey.hasMoreElements()) {
            String key = enumKey.nextElement();
            String avClass = annotationInspectors.get(key);
            if (key.equals("primary") && avClass != null) {
                try {
                    primary = (AnnotationIntrospector) Class.forName(avClass)
                            .newInstance();
                } catch (Exception e) {
                    throw new Exception(
                            "JsonMapperService: Error instantiating " + avClass
                                    + " for " + key);
                }
            } else if (key.equals("secondary") && avClass != null) {
                try {
                    secondary = (AnnotationIntrospector) Class.forName(avClass)
                            .newInstance();
                } catch (Exception e) {
                    throw new Exception(
                            "JsonMapperService: Error instantiating " + avClass
                                    + " for " + key);
                }
            }
        }
        if (primary == null) {
            primary = new JacksonAnnotationIntrospector(); // support default
            getLogger().info(
                    "using default introspector:"
                            + primary.getClass().getName());
            mapper.setAnnotationIntrospector(primary);
        } else {
            AnnotationIntrospector pair = new AnnotationIntrospector.Pair(
                    primary, secondary);
            mapper.setAnnotationIntrospector(pair);
        }

        // mapper.enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE,
        // "type");
        if (features != null) {
            Enumeration<String> enumFeatureKey = features.keys();
            while (enumFeatureKey.hasMoreElements()) {
                String featureKey = enumFeatureKey.nextElement();
                Boolean featureValue = features.get(featureKey);
                Feature feature;
                if (featureKey != null && featureValue != null) {
                    try {
                        String[] featureParts = featureKey.split("\\.");
                        getLogger().info(
                                "initializing mapper feature: "
                                        + featureParts[featureParts.length - 1]
                                        + " with " + featureValue);
                        feature = Feature
                                .valueOf(featureParts[featureParts.length - 1]);
                        mapper.configure(feature, featureValue);

                        assert mapper.getSerializationConfig().isEnabled(
                                feature) == featureValue;
                    } catch (Exception e) {
                        throw new Exception(
                                "JsonMapperService: Error instantiating feature "
                                        + featureKey + " with  " + featureValue,
                                e);
                    }
                }
            }
        }

        if (defaultTypeDefs != null && defaultTypeDefs.length == 2) {
            DefaultTyping defaultTyping = DefaultTyping
                    .valueOf(defaultTypeDefs[0]);
            mapper.enableDefaultTypingAsProperty(defaultTyping,
                    defaultTypeDefs[1]);
            getLogger().info(
                    "default typing is " + defaultTypeDefs[0] + " with key:"
                            + defaultTypeDefs[1]);
        }

        getLogger().info("setting date format to:" + dateFormat);
        getLogger().info("keepFilters is:" + cacheFilters);

        mapper.setDateFormat(new SimpleDateFormat(dateFormat));

        filters = Collections
                .synchronizedMap(new HashMap<String, FilterProvider>());
        getLogger().info("initialized: mapper:" + mapper);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public class MixinModule extends SimpleModule {
        public final Class<?> clazz;
        public final Class<?> mixin;

        public <T, U> MixinModule(String name, Class<T> clazz, Class<U> mixin) {
            super(name, new Version(1, 0, 0, null));
            this.clazz = clazz;
            this.mixin = mixin;

        }

        @Override
        public void setupModule(SetupContext context) {
            context.setMixInAnnotations(this.clazz, this.mixin);
        }
    }


}
