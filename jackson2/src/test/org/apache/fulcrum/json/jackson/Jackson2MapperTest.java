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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.apache.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;


/**
 * More Jackson2 JSON Test
 * 
 * Test with clearing mixins

 * @author gk
 * @version $Id$
 */
@RunWith(JUnitPlatform.class)
public class Jackson2MapperTest extends BaseUnit5Test {
    
    private JsonService sc = null;
    Logger logger;

    @BeforeEach
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new Log4JLogger(LogManager.getLogger(getClass().getName()) );
        // clear 
        ((Jackson2MapperService)sc).setMixins(null,null);
        logger.debug( "cleared mixins");
    }

    @Test
    public void testSerializeWithCustomFilter() throws Exception {
        Bean bean = new Bean();
        bean.setName("joe");
        bean.setAge(12);
        String filteredBean  = sc.serializeOnlyFilter(bean, Bean.class, "name");
        assertEquals( "{\"name\":\"joe\"}", filteredBean, "Ser filtered Bean failed ");

        Rectangle rectangle = new Rectangle(5, 10);
        rectangle.setName("jim");
        String filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals(
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle,
                "Ser filtered Rectangle failed ");
    }
    
    // analog seralizeAllExcept 
    @Test
    public void testSerializeOnlyFilterMultipleFilterChanges() throws Exception {
        Bean bean = new Bean();
        bean.setName("joe");
        bean.setAge(12);
        
        Rectangle rectangle = new Rectangle(5, 10);
        rectangle.setName("jim");
        
        String filteredBean  = sc.serializeOnlyFilter(bean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", filteredBean, "global Ser filtered Bean failed ");

        String filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals(
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle,
                "global Ser filtered Rectangle failed ");
        
        filteredBean  = ((Jackson2MapperService)sc).serializeOnlyFilter(bean, new Class[]{ Bean.class}, true, "age");
        assertEquals("{\"age\":12}", filteredBean, "Another Global Ser filtered Bean failed ");

        filteredRectangle  = ((Jackson2MapperService)sc).serializeOnlyFilter( 
               rectangle, new Class[] { Rectangle.class}, true, "h", "name");
        assertEquals(
                "{\"h\":10,\"name\":\"jim\"}", filteredRectangle,
                "Local Ser filtered Rectangle failed ");
        
        // if refresh would be false, this would fail
        filteredBean  = sc.serializeOnlyFilter(bean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", filteredBean,
                     "global Ser filtered Bean failed ");

        filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals(
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle, "global Ser filtered Rectangle failed ");
    }

}
