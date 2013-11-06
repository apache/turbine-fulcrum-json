package org.apache.fulcrum.json;

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

/**
 * JsonService defines methods needed to serialize and deserialize and hepler
 * methos if needed.
 * 
 * Some methods expect a class parameter.
 * 
 * If you want to call theses methods from an environment, where you could only
 * provide strings (e.g. velocity context), just wrap the method and call
 * <code>Class clazz = Class.forName(className);</code> for the parameter.
 * 
 * 
 * @author gk
 * @version $Id$
 */
public interface JsonService {
    /** Avalon Identifier **/
    String ROLE = JsonService.class.getName();

    /**
     * Serializes a Java object
     * 
     * @param src
     *            The java object to be serialized.
     * 
     * @return JSON string
     * 
     * @throws Exception
     *             if JSON serialization fails
     */
    String ser(Object src) throws Exception;

    /**
     * Serializes a Java object
     * 
     * @param src
     *            The java object to be serialized
     * @param type
     *            Type, which should be used for the provided object .
     * 
     * @return JSON string
     * 
     * @throws Exception
     *             if JSON serialization fails
     */
    <T> String ser(Object src, Class<T> type) throws Exception;

    /**
     * Deserialzes a JSON string
     * 
     * @param src
     *            Tthe JSON string to be deserialized
     * @param type
     *            The java type to be used as a class
     * 
     * @return an object
     * 
     * @throws Exception
     *             if JSON deserialization fails
     */
    <T> T deSer(String src, Class<T> type) throws Exception;

    /**
     * @see #serializeOnlyFilter(Object, Class, boolean, String...).
     * 
     * <code>refreshFilter</code> is <code>false</code>.
     */
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass,
            String... filterAttr) throws Exception;
    
    /**
     * Serialize only object properties where filters attributes are provided
     * 
     * @param src
     *            The Java object to serialize
     * @param filterClass
     *            The class to which the filtering should be applied
     *            
     * @param refreshFilter
     *             refresh Filter (clean cache for this filerClass)
     *  
     * @param filterAttr
     *            The class bean attributes which should be serialized
     * 
     * @return JSON string
     * 
     * @throws Exception
     *             if JSON serialization or filter registration fails
     */
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass, boolean refreshFilter,
            String... filterAttr) throws Exception;

    /**
     * Serialize all object properties excluding provided filters attributes
     * 
     * @param src
     *            The Java object to serialize
     * @param filterClass
     *            The class to which the filtering should be applied. If its the
     *            same class, just the filterAttributes get applied. If not the
     *            class is filtered out, if found as a property type.
     * @param refreshFilter
     *            refresh Filter (clean cache for this filerClass)      
     * 
     * @param filterAttr
     *            The bean attributes which should not be serialized
     * 
     * @return JSON string
     * 
     * @throws Exception
     *             if JSON serialization or filter registration fails
     */
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, boolean refreshFilter, String... filterAttr) throws Exception;
    
    /** 
     * @see #serializeAllExceptFilter(Object, Class, boolean, String...)
     * 
     * <code>refreshFilter</code> is <code>false</code>.
     */
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception;

    /**
     * Adds an adapter (mixin, serializer,..) for the target class depending on
     * the JsonService implementation
     * 
     * @param name
     *            A name for the adapter
     * @param target
     *            The target class for this adapter
     * @param mixin
     *            The adapter/mixin for the target class
     * 
     * @return JsonService
     * 
     * @throws Exception
     *             if adapter registration fails
     */
    public JsonService addAdapter(String name, Class target, Class mixin)
            throws Exception;

    /**
     * Adds an adapter (mixin, serializer,..) for the target class depending on
     * the JsonService implementation
     * 
     * @param name
     *            A name for the adapter
     * @param target
     *            The target class for this adapter
     * @param mixin
     *            The adapter/mixin for the target object
     *            (serializer/deserializer)
     * 
     * @return JsonService
     * 
     * @throws Exception
     *             if adapter registration fails
     */
    public JsonService addAdapter(String name, Class target, Object mixin)
            throws Exception;

    /**
     * @param df
     *            The {@link DateFormat} to be used by the JsonService.
     * 
     *            It could be provided by component configuration too.
     * 
     */
    public void setDateFormat(final DateFormat df);

}
