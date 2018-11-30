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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.json.jackson.example.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.apache.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

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
@RunWith(JUnitPlatform.class)
public class JsonPathJacksonTest extends BaseUnit5Test {
    
    private JsonService sc = null;
    Logger logger;

    @BeforeEach
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new Log4JLogger(LogManager.getLogger(getClass().getName()) );
        try {
            Configuration conf = Configuration.defaultConfiguration();
            logger.debug("jayway jsonpath conf:"+ conf.jsonProvider());
            assertEquals( JacksonJsonProvider.class.getName(), conf.jsonProvider().getClass().getName(),
                          "Jackson JsonPath JsonProvider match failed ");
            
            logger.debug("Jackson2MapperService.mapper:"+ ((Jackson2MapperService)sc).getMapper()  + " confjsonProvider:" + conf.jsonProvider());
            
            assertTrue(conf.jsonProvider() instanceof JacksonJsonProvider, 
                       "JsonProvider is not a JacksonJsonProvider ");
            
            assertEquals(((Jackson2MapperService)sc).getMapper(), ((JacksonJsonProvider)conf.jsonProvider()).getObjectMapper(), 
                         "JacksonJsonProvider mapper is not configured mapper");
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
        assertEquals("Config.xml", cf, "Serialization failed ");   
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
        assertEquals(refDate.get(Calendar.DATE), parsedDate.get(Calendar.DATE), "Serialization failed ");
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
        assertEquals("joe2", joe2.getName(), 
                     "DeSer failed ");
        // could not map to typed list
        List<Map<String, Object>> beanList2 = JsonPath.parse(result).read("$[-2:]", List.class);
        assertEquals(2, beanList2.size(), 
                     "Expect 2 Elements failed ");
        //System.out.println("bean list result: "+ beanList2);
        assertEquals("joe9", beanList2.get(1).get("name"), 
                     "DeSer failed ");
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
            assertEquals((idx * idx), rect.getSize(), 
                         "deser reread size failed");
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
         assertEquals("jim jar", ((JSONObject)(jsonOrgResult.get(0))).get("name"), 
                         "DeSer failed name" );
         assertEquals(45, ((JSONObject)(jsonOrgResult.get(1))).get("age"),
                      "DeSer failed age" );
    }
    
    @Test
    public void testDeSerUnQuotedObject() throws Exception {
        String jsonString = "{name:\"joe\"}";
        TypeRef<Bean> typeRef = new TypeRef<Bean>() { };
        Bean result = JsonPath.parse(jsonString).read("$",typeRef); 
        assertTrue(result instanceof Bean, 
                   "expected bean object!");
    }

}
