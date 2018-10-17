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
import static org.junit.Assert.assertEquals;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;



/**
 * More Jackson2 JSON Test
 * 
 * Test with clearing mixins
 * 
 * @author gk
 * @version $Id$
 */
public class Jackson2MapperTest extends BaseUnit4Test {
    
    private JsonService sc = null;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
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
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe\"}", filteredBean);

        Rectangle rectangle = new Rectangle(5, 10);
        rectangle.setName("jim");
        String filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals("Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle);
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
        assertEquals("global Ser filtered Bean failed ", "{\"name\":\"joe\"}", filteredBean);

        String filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals("global Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle);
        
        filteredBean  = ((Jackson2MapperService)sc).serializeOnlyFilter(bean, new Class[]{ Bean.class}, true, "age");
        assertEquals("Another Global Ser filtered Bean failed ", "{\"age\":12}", filteredBean);

        filteredRectangle  = ((Jackson2MapperService)sc).serializeOnlyFilter( 
               rectangle, new Class[] { Rectangle.class}, true, "h", "name");
        assertEquals("Local Ser filtered Rectangle failed ",
                "{\"h\":10,\"name\":\"jim\"}", filteredRectangle);
        
        // if refresh would be false, this would fail
        filteredBean  = sc.serializeOnlyFilter(bean, Bean.class, "name");
        assertEquals("global Ser filtered Bean failed ", "{\"name\":\"joe\"}", filteredBean);

        filteredRectangle  = sc.serializeOnlyFilter(rectangle,
                Rectangle.class, "w", "name");
        assertEquals("global Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", filteredRectangle);
    }

}
