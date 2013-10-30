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

import org.apache.avalon.framework.service.ServiceException;



/**
 * ParserService defines the methods which are needed by the parser objects
 * to get their necessities.
 *
 * @author gk
 * @version $Id$
 */
public interface JsonService 
{
    /** Avalon Identifier **/
    String ROLE = JsonService.class.getName();

    /** Default Encoding for Parameter Parser */
    String ENCODING_DEFAULT = "UTf-8";


    /**
     * Use the JsonService 
     *
     * @return A object of
     *
     * @throws ServiceException if parsing fails or the UploadService
     * is not available
     */
    String ser(Object src) throws Exception;
    
    <T> String ser( Object src , Class<T> type ) throws Exception;

    <T> T deSer(String src, Class<T> type) throws Exception;
    
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass, String ... filterAttr ) throws Exception;
    
    public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass, String ... filterAttr ) throws Exception;
    
    public JsonService addAdapter( String name, Class target, Class mixin ) throws Exception; 
    
    public void setDateFormat(final DateFormat df );

}

