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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.fulcrum.json.JsonService;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

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

    private String dateFormat;

    final String DEFAULTDATEFORMAT = "MM/dd/yyyy";

    private Hashtable<String, String> adapters = null;

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
            Class<T> arg2) throws Exception {
        getLogger().debug("deser:" + json);
        getLogger().debug("collectionType:" + collectionType);
        return  gson.create().fromJson(json, (Type)collectionType);
    }
    

    @Override
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass,
            String... filterAttr) throws Exception {
        throw new Exception("Not yet implemented!");
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
        gson.registerTypeAdapter(target, adapter.newInstance());
        return null;
    }

    @Override
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception {
        return serializeAllExceptFilter(src, filterClass, false, filterAttr);
    }
    
    @Override
    public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass,
            Boolean arg2, String... filterAttr) throws Exception {
        return gson
                .addSerializationExclusionStrategy(
                        exclude(filterClass, filterAttr)).create().toJson(src);
    }

    public JsonService registerTypeAdapter(JsonSerializer serdeser, Type type) {
        gson.registerTypeAdapter(type, serdeser);
        return this;
    }

    @Override
    public void setDateFormat(final DateFormat df) {
        JsonSerializer<Date> ser = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc,
                    JsonSerializationContext context) {
                return src == null ? null : new JsonPrimitive(df.format(src));
            }
        };
        registerTypeAdapter(ser, Date.class);
    }

    public void getJsonService() throws InstantiationException {
        // gson.registerTypeAdapter(Date.class, ser).
        // addSerializationExclusionStrategy( exclude(ObjectKey.class) ).
        // addSerializationExclusionStrategy( exclude(ComboKey.class) );
        // return gson.create().toJson( src );
    }

    @Override
    public void configure(Configuration conf) throws ConfigurationException {

        getLogger().debug("conf.getName()" + conf.getName());
        final Configuration configuredDateFormat = conf.getChild(DATE_FORMAT,
                true);
        this.dateFormat = configuredDateFormat.getValue(DEFAULTDATEFORMAT);
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
    }

    @Override
    public void initialize() throws Exception {
        gson = new GsonBuilder();
        getLogger().debug("initialized: gson:" + gson);
        getLogger().info("setting date format to:" + dateFormat);
        setDateFormat(new SimpleDateFormat(dateFormat));

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
                        throw new Exception(
                                "JsonMapperService: Error instantiating "
                                        + avClass + " for " + forClass);
                    }
                }
            }
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
                return (!excludedAttributes.isEmpty()) ? this.excludedAttributes
                        .contains(paramFieldAttributes.getName()) : false;
            }
        }.init(clazz, filterAttrs);
    }
    

}
