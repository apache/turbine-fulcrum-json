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

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.fulcrum.json.JsonService;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * 
 * By default multiple serialization of the same object in a single thread is
 * not support (e.g adapter + default for the same bean / object).
 * 
 * 
 * @author gk
 * @version $Id$
 * 
 */
public class GSONBuilderService extends AbstractLogEnabled implements
        JsonService, Initializable, Configurable {

    private static final String GLOBAL_ADAPTERS = "globalAdapters";

    private static final String DATE_FORMAT = "dateFormat";
    
    private static final String USEJSONPATH = "useJsonPath";

    private String dateFormat;

    private Hashtable<String, String> adapters = null;

    private boolean useJsonPath = false;
    
    GsonBuilder gson;

    @Override
    public String ser(Object src) throws Exception {
        getLogger().debug("ser" + src);
        return gson.create().toJson(src);
    }

    @Override
    public <T> String ser(Object src, Class<T> type) throws Exception {
        getLogger().debug("ser::" + src + " with type" + type);

        Type collectionType = new TypeToken<T>() {
        }.getType();
        return gson.create().toJson(src, collectionType);
    }

    @Override
    public <T> T deSer(String json, Class<T> type) throws Exception {
        // TODO Auto-generated method stub
        getLogger().debug("deser:" + json);
        return gson.create().fromJson(json, type);
    }
    
    @Override
    public <T> Collection<T> deSerCollection(String json, Object collectionType,
            Class<T> elementType) throws Exception {
        getLogger().debug("deser:" + json);
        getLogger().debug("collectionType:" + collectionType);
        return  gson.create().fromJson(json, (Type)collectionType);
    }

    @Override
    public String serializeOnlyFilter(Object src, String... filterAttr)
            throws Exception {
        return  gson
                .addSerializationExclusionStrategy(
                        include(null,filterAttr)).create().toJson(src);
    }

    @Override
    public String serializeOnlyFilter(Object src, Boolean notused,
            String... filterAttr) throws Exception {
        return  gson
                .addSerializationExclusionStrategy(
                        include(null,filterAttr)).create().toJson(src);
    }

    @Override
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass,
            String... filterAttr) throws Exception {
        return  gson
        .addSerializationExclusionStrategy(
                include(filterClass, filterAttr)).create().toJson(src);
    }
    
    @Override
    public <T> String serializeOnlyFilter(Object arg0, Class<T> arg1,
            Boolean arg2, String... arg3) throws Exception {
        throw new Exception("Not yet implemented!");
    }

    /**
     * registering an adapter 
     * 
     * @see GsonBuilder#registerTypeAdapter(Type, Object)
     */
    @Override
    public JsonService addAdapter(String name, Class target, Object adapter)
            throws Exception {
        gson.registerTypeAdapter(target, adapter);
        return this;
    }

    /**
     * registering an adapter. Unregistering could be only done by reinitialize {@link GsonBuilder} 
     * using @link {@link GSONBuilderService#initialize()}, although a new Adapter with the same target overwrites the previously defined.
     * 
     * @see GsonBuilder#registerTypeAdapter(Type, Object)
     */
    @Override
    public JsonService addAdapter(String name, Class target, Class adapter)
            throws Exception {
        gson.registerTypeAdapter(target, adapter.getConstructor().newInstance());
        return null;
    }

    @Override
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception {
        return gson
                .addSerializationExclusionStrategy(
                        exclude(filterClass, filterAttr)).create().toJson(src);
    }
    
    @Override
    public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass,
            Boolean clearCache, String... filterAttr) throws Exception {
        throw new Exception("Not yet implemented!");
    }
    
    @Override
    public String serializeAllExceptFilter(Object src, String... filterAttr)
            throws Exception {
        return gson
                .addSerializationExclusionStrategy(
                        exclude(null, filterAttr)).create().toJson(src);
    }

    @Override
    public String serializeAllExceptFilter(Object src, Boolean notused,
            String... filterAttr) throws Exception {
        return gson
                .addSerializationExclusionStrategy(
                        exclude(null, filterAttr)).create().toJson(src);
    }
    
    @Override
    public String ser(Object src, Boolean refreshCache) throws Exception {
        throw new Exception("Not implemented!");
    }

    @Override
    public <T> String ser(Object src, Class<T> type, Boolean refreshCache)
            throws Exception {
        throw new Exception("Not implemented!");
    }

    public JsonService registerTypeAdapter(Object serdeser, Type type) {
        gson.registerTypeAdapter(type, serdeser);
        return this;
    }
    
    /**
     * Alternative method to calling {@link #registerTypeAdapter(Object, Type)}
     * Note: Always use either this direct format call or the other adapter register call,
     * otherwise inconsistencies may occur!
     * 
     * @param dfStr date format string
     */
    public void setDateFormat(final String dfStr) {
        gson.setDateFormat(dfStr);
    }

    /* (non-Javadoc)
     * @see org.apache.fulcrum.json.JsonService#setDateFormat(java.text.DateFormat)
     */
    @Override
    public void setDateFormat(final DateFormat df) {
        DateTypeAdapter dateTypeAdapter = new DateTypeAdapter();
        dateTypeAdapter.setCustomDateFormat(df);
        gson.registerTypeAdapter(Date.class,dateTypeAdapter);
    }

    public void getJsonService() throws InstantiationException {
        // gson.registerTypeAdapter(Date.class, ser).
        // addSerializationExclusionStrategy( exclude(ObjectKey.class) ).
        // addSerializationExclusionStrategy( exclude(ComboKey.class) );
        // return gson.create().toJson( src );
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    @Override
    public void configure(Configuration conf) throws ConfigurationException {

        getLogger().debug("conf.getName()" + conf.getName());
        final Configuration configuredDateFormat = conf.getChild(DATE_FORMAT,
                false);
        if (configuredDateFormat != null) {
            this.dateFormat = configuredDateFormat.getValue();// DEFAULTDATEFORMAT);
        }
        final Configuration configuredAdapters = conf.getChild(GLOBAL_ADAPTERS,
                true);
        if (configuredAdapters != null) {
            Configuration[] nameVal = configuredAdapters.getChildren();
            for (int i = 0; i < nameVal.length; i++) {
                String key = nameVal[i].getName();
                getLogger().debug("configured key: " + key);
                if (key.equals("adapter")) {
                    String forClass = nameVal[i].getAttribute("forClass");
                    this.adapters = new Hashtable<String, String>();
                    this.adapters.put(forClass, nameVal[i].getValue());
                }
            }
        }
        // TODO provide configurable Type Adapters
        final Configuration configuredjsonPath = conf.getChild(
                USEJSONPATH, false);
        if (configuredjsonPath != null) {
            this.useJsonPath  = configuredjsonPath.getValueAsBoolean();
        }
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    @Override
    public void initialize() throws Exception {
        gson = new GsonBuilder();
        getLogger().debug("initialized: gson:" + gson);
        if (dateFormat != null) {
            getLogger().info("setting date format to: " + dateFormat);
            setDateFormat(new SimpleDateFormat(dateFormat));
            //setDateFormat(dateFormat);
        }

        if (adapters != null) {
            Enumeration<String> enumKey = adapters.keys();
            while (enumKey.hasMoreElements()) {
                String forClass = enumKey.nextElement();
                String avClass = adapters.get(forClass);
                if (avClass != null) {
                    try {
                        getLogger().debug(
                                "initializing: adapters " + avClass
                                        + " forClass:" + forClass);
                        Class adapterForClass = Class.forName(forClass);
                        Class adapterClass = Class.forName(avClass);
                        addAdapter("Test Adapter", adapterForClass,
                                adapterClass);

                    } catch (Exception e) {
                        throw new InstantiationException(
                                "JsonMapperService: Error instantiating one of the adapters: "
                                        + avClass + " for " + forClass);
                    }
                }
            }
        }
        
        if (useJsonPath) {
            // set it before runtime
            com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {
                
                private Callable<Gson> gsonFuture = new Callable<Gson>() {
                    @Override
                    public Gson call() {
                        return GSONBuilderService.this.gson.create();
                    }
                };

                private final JsonProvider jsonProvider = new GsonJsonProvider(GSONBuilderService.this.gson.create());
                private final MappingProvider mappingProvider = new GsonMappingProvider(gsonFuture);

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
            });
        }
    }

    /**
     * Simple Exclusion strategy to filter class or fields used by this service
     * for serialization (not yet deserialization).
     * 
     * @param clazz
     *            The class to be filtered out.
     * @param filterAttrs
     *            The fieldnames to be filtered as string
     * @return the strategy applied by GSON
     */
    private ExclusionStrategy exclude(Class clazz, String... filterAttrs) {
        return new ExclusionStrategy() {

            public Class<?> excludedThisClass;
            public HashSet<String> excludedAttributes;

            private ExclusionStrategy init(Class<?> excludedThisClass,
                    String... filterAttrs) {
                this.excludedThisClass = excludedThisClass;
                if (filterAttrs != null) {
                    this.excludedAttributes = new HashSet<String>(
                            filterAttrs.length);
                    Collections.addAll(this.excludedAttributes, filterAttrs);
                } else
                    this.excludedAttributes = new HashSet<String>();

                return this;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return (excludedThisClass != null) ? excludedThisClass
                        .equals(clazz) : false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes paramFieldAttributes) {
                // return paramFieldAttributes.getDeclaringClass() ==
                // excludedThisClass &&
                // excludesAttributes.contains(paramFieldAttributes.getName());
                return !excludedAttributes.isEmpty() ? this.excludedAttributes
                        .contains(paramFieldAttributes.getName()) : false;
            }
        }.init(clazz, filterAttrs);
    }
    
    /**
     * @param clazz the class to exclude
     * @param filterAttrs bean elements not to be serialized
     * @return
     */
    private ExclusionStrategy include(Class clazz, String... filterAttrs) {
        return new ExclusionStrategy() {

            private Class<?> includeThisClass;
            private HashSet<String> includedAttributes;

            private ExclusionStrategy init(Class<?> includeThisClass,
                    String... filterAttrs) {
                this.includeThisClass = includeThisClass;
                if (filterAttrs != null) {
                    this.includedAttributes = new HashSet<String>(
                            filterAttrs.length);
                    getLogger().debug(" ... adding includedAttributes:" + filterAttrs.length);
                    Collections.addAll(this.includedAttributes, filterAttrs);
                    for (String includedAttribute : includedAttributes) {
                        getLogger().debug("includedAttribute:" +includedAttribute);
                    }
                } else
                    this.includedAttributes = new HashSet<String>();

                return this;
            }

            /**
             * skip is current class is not equal provided class
             */
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                getLogger().debug(includeThisClass+ ": comparing include class:" + clazz);
                return includeThisClass != null ? !includeThisClass
                        .equals(clazz) : false;
            }

            /**
             * skip if current field attribute is not included are skip else
             */
            @Override
            public boolean shouldSkipField(FieldAttributes paramFieldAttributes) { 
                return !includedAttributes.isEmpty() ? !this.includedAttributes
                        .contains(paramFieldAttributes.getName()) : true;        

            }
        }.init(clazz, filterAttrs);
    }

}
