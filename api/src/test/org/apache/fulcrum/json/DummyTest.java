package org.apache.fulcrum.json;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.fulcrum.yaafi.container.DefaultContainerSetup;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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

public class DummyTest  {

    DefaultContainerSetup container;
    
    @BeforeEach
    public void setup() {
    }
    
    @Test
    public void testDummyJSON() throws Exception {
        container = new DefaultContainerSetup();
        container.setConfigurationFileName( "/DefaultJSONComponentConfig.xml" );
        container.setRoleFileName( "/DefaultJSONRoleConfig.xml" );
        Object service = container.lookup( JsonService.ROLE );
        System.out.println( "service:" + service );
        assertNotNull( service );
        assertTrue( service instanceof JsonService );
        JsonService jsonService = (JsonService) service;
        String test = jsonService.ser( "test" );
        assertNotNull( test );
        assertEquals( "test", test );
    }
    
    @AfterEach
    public void dispose() {
       container.tearDown(); 
    }
}
