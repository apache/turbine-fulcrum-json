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
import java.util.Collection;

/**
 * This class defines methods needed to serialize and deserialize and helper
 * methos if needed.
 * 
 * Some methods expect a class parameter.
 * 
 * If you want to call theses methods from an environment, where you could only
 * provide strings (e.g. velocity context), just wrap the method and call
 * <code>Class clazz = Class.forName(className);</code> for the parameter.
 * 
 * 
 * @author <a href="mailto:gk@apache.org">Georg Kallidis</a>
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
     * @param src
     *              The java object to be serialized.
     * @param refreshCache 
     *              If <code>true</code>, try to refresh cache after serialization
     * 
     * For other attributes @see {@link #ser(Object)}

     */
    String ser(Object src, Boolean cleanCache) throws Exception;

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
     *             If JSON serialization fails
     */
    <T> String ser(Object src, Class<T> type) throws Exception;
    
    /**
     * 
     * @param src
     * @param type
     * @param refreshCache 
     *          If <code>true</code>, try to clean cache after serialization
     * 
     * For other attributes @see {@link #ser(Object, Class)}
     */
    <T> String ser(Object src, Class<T> type, Boolean cleanCache) throws Exception;

    /**
     * Deserializing a JSON string
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
     * This is to deserialize collections. Depending on the implementation either both collectiontype and elementType is needed or 
     * the elementType will be derived from the typed collectiontype.
     * 
     * @param json
     *          The JSON string to be deserialized
     * @param collectionType
     *          It could be just the collection or the typed collection. It may then be used to get the type for element type too.
     *          Cft. implementation tests for more details (GSON). 
     * @param elementType
     *          The element type. This is need in any case to assure the generic checking.
     * @return
     * @throws Exception
     */
    <T> Collection<T> deSerCollection(String json, Object collectionType, Class<T> elementType) 
            throws Exception;

    /**
     * @see #serializeOnlyFilter(Object, Class, Boolean, String...).
     * 
     * <code>refreshFilter</code> is set to <code>false</code> for this method call.
     */
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass,
            String... filterAttr) throws Exception;
    
    /**
     * Serialize only object properties where filter attributes are provided
     * 
     * @param src
     *            The Java object to serialize
     * @param filterClass
     *            The class to which the filtering should be applied
     *            
     * @param cleanFilter
     *             clean filter (clean cache for this filterClass) if <code>true</code>,  after serialization.
     *  
     * @param filterAttr
     *            The class bean attributes which should be serialized
     * 
     * @return JSON string
     * 
     * @throws Exception
     *             if JSON serialization or filter registration fails
     */
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass, Boolean cleanFilter,
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
     * @param cleanFilter
     *            refresh filter (clean cache for this filterClass) after serialization.      
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
            Class<T> filterClass, Boolean cleanFilter, String... filterAttr) throws Exception;
    
    /** 
     * @see #serializeAllExceptFilter(Object, Class, Boolean, String...)
     * 
     * <code>refreshFilter</code> is <code>false</code>.
     */
    public <T> String serializeAllExceptFilter(Object src,
            Class<T> filterClass, String... filterAttr) throws Exception;

    /**
     * Adds an adapter (mixin, serializer,..) for the target class depending on
     * the JsonService implementation.
     * Cft. to {@link #addAdapter(String, Class, Object)}
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
     * Add an adapter (mixin, serializer,..) for the target class depending on
     * the JsonService implementation. Adapters could by default not deregistered. If you want
     * to get rid of them, you have to (@see {@link #reInitService()} (or overwrite with the same target type, depending on
     * implementation) 
     * 
     * @param name
     *            A name for the adapter
     * @param target
     *            The target class for this adapter
     * @param mixin
     *            The adapter/mixin for the target object
     *            (module/serializer/deserializer)
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
