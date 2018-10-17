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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.json.jackson.example.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;

/**
 * Jackson2 JSON Test
 * 
 * Test without type setting 
 * 
 * @author gk
 * @version $Id$
 */
public class JsonPathJacksonTest extends BaseUnit4Test {
    
    private JsonService sc = null;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
        try {
            Configuration conf = Configuration.defaultConfiguration();
            logger.debug("jayway jsonpath conf:"+ conf.jsonProvider());
            assertEquals("Jackson JsonPath JsonProvider match failed ", JacksonJsonProvider.class.getName(), conf.jsonProvider().getClass().getName());
            logger.debug("Jackson2MapperService.mapper:"+ ((Jackson2MapperService)sc).getMapper()  + " confjsonProvider:" + conf.jsonProvider());
            assertTrue("JsonProvider is not a JacksonJsonProvider ",  conf.jsonProvider() instanceof JacksonJsonProvider);
            assertEquals("JacksonJsonProvider mapper is not configured mapper", ((Jackson2MapperService)sc).getMapper(), ((JacksonJsonProvider)conf.jsonProvider()).getObjectMapper());
        } catch (Throwable e) {
            if (e.getCause() != null && e.getCause() instanceof ClassNotFoundException) { 
                logger.error(e.getMessage(), e.getCause());
                fail("Check correct initialization with useJsonPath = true):");
            } else {
                fail(e.getMessage());
            }
        }

    }

    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        logger.debug("serJson:"+ serJson);
        String cf = JsonPath.parse(serJson).read("$.container.cf");// .using(conf)
        assertEquals("Serialization failed ", "Config.xml", cf);   
    }

    @Test
    public void testSerializeDateWithDefaultDateFormat() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        Calendar refDate = Calendar.getInstance();
        map.put("date", refDate.getTime());
        String serJson = sc.ser(map);
        //System.out.println("serJson date: "+ serJson);
        Date date = JsonPath.parse(serJson).read("$.date", Date.class);// .using(conf)
        Calendar parsedDate = Calendar.getInstance();
        parsedDate.setTime(date);
        assertEquals("Serialization failed ", refDate.get(Calendar.DATE), parsedDate.get(Calendar.DATE));
    }

    @Test
    public void testDeserializationCollectionWithFilter() throws Exception {

        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.serializeOnlyFilter(beanList, Bean.class, "name",   "age");
        //System.out.println("bean list: "+ result);
        
        Bean joe2 = JsonPath.parse(result).read("$[2]", Bean.class);
        assertEquals("DeSer failed ", "joe2", joe2.getName());
        // could not map to typed list
        List<Map<String, Object>> beanList2 = JsonPath.parse(result).read("$[-2:]", List.class);
        assertEquals("Expect 2 Elements failed ", 2, beanList2.size());
        //System.out.println("bean list result: "+ beanList2);
        assertEquals("DeSer failed ", "joe9", beanList2.get(1).get("name"));
    }

    @Test
    public void testSerializeExcludeField() throws Exception {
        // could not use as TestClass constructor generates configurationName again
        //String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "configurationName");
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "name"); 
        logger.debug("serJson: "+ serJson);
        TypeRef<TestClass> typeRef = new TypeRef<TestClass>() { };
        // could not use as TestClass constructor generates configurationName again
        TestClass result = JsonPath.parse(serJson).read("$",typeRef); //  TestClass.class
        assertTrue(result.getName() == null);// ! 
    }
    
    
    @Test
    public void testDeserializationCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        logger.debug("serColl: "+ serColl);
        TypeRef<List<Rectangle>> typeRef = new TypeRef<List<Rectangle>>() { };
        List<Rectangle> result = JsonPath.parse(serColl).read("$",typeRef);
        //System.out.println("result: "+ result);
        int idx = 0;
        for (Rectangle rect : result) {
            assertEquals("deser reread size failed", (idx * idx), rect.getSize());
            idx++;
        }
    }

    @Test
    public void testDeSerToORGJSONCollectionObject() throws Exception {
        // test array
         List<Bean> beanResults = new ArrayList<Bean> ( );
         Bean tu = new Bean();
         tu.setName("jim jar");
         beanResults.add(tu);
         Bean tu2 = new Bean();
         tu2.setName("jim2 jar2");
         tu2.setAge(45);
         beanResults.add(tu2);
         
         String[] filterAttr = {"age","name" };   
         String filteredSerList = sc.serializeOnlyFilter(beanResults, Bean.class, filterAttr);
         logger.debug("serList: "+ filteredSerList);
         
         sc.addAdapter(null, null,new JsonOrgModule());

         TypeRef<JSONArray> typeRef = new TypeRef<JSONArray>() { };
         JSONArray jsonOrgResult = JsonPath.parse(filteredSerList).read("$",typeRef);
         
         logger.debug("jsonOrgResult: "+ jsonOrgResult.toString(2));
         assertEquals("DeSer failed ", "jim jar", ((JSONObject)(jsonOrgResult.get(0))).get("name") );
         assertEquals("DeSer failed ", 45, ((JSONObject)(jsonOrgResult.get(1))).get("age") );      
    }
    
    @Test
    public void testDeSerUnQuotedObject() throws Exception {
        String jsonString = "{name:\"joe\"}";
        TypeRef<Bean> typeRef = new TypeRef<Bean>() { };
        Bean result = JsonPath.parse(jsonString).read("$",typeRef); 
        assertTrue("expected bean object!", result instanceof Bean);
    }

}
