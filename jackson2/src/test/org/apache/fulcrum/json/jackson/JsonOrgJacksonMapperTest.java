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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

/**
 * Jackson2 JSON Test
 * 
 * Test without type setting 
 * 
 * @author gk
 * @version $Id: JacksonMapperTest.java 1800753 2017-07-04 11:00:03Z gk $
 */
public class JsonOrgJacksonMapperTest extends BaseUnit4Test {
    
    private JsonService sc = null;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        sc.addAdapter(null, null,new JsonOrgModule());
        //((Jackson2MapperService)sc).registerModule(new JsonOrgModule());
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
    }
    
    // support for org.json mapping 
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
         String[] filterAttr = {"name", "age" };   
         String filteredSerList = sc.serializeOnlyFilter(beanResults, Bean.class, filterAttr);
         logger.debug("serList: "+ filteredSerList);
         JSONArray jsonOrgResult = sc.deSer(filteredSerList, JSONArray.class);//readValue(serList, JSONArray.class);
         logger.debug("jsonOrgResult: "+ jsonOrgResult.toString(2));
         assertEquals("DeSer failed ", "jim jar", ((JSONObject)(jsonOrgResult.get(0))).get("name") );
         assertEquals("DeSer failed ", 45, ((JSONObject)(jsonOrgResult.get(1))).get("age") );      
    }
    
    // support for org.json mapping 
    @Test
    public void testSerToORGJSONCollectionObject() throws Exception {
        // test array
         List<Bean> beanList = new ArrayList<Bean> ( );
         Bean tu = new Bean();
         tu.setName("jim jar");
         beanList.add(tu);
         Bean tu2 = new Bean();
         tu2.setName("jim2 jar2");
         tu2.setAge(45);
         beanList.add(tu2);
         String[] filterAttr = {"name", "age" };
         JSONArray jsonArray =((Jackson2MapperService)sc).getMapper().convertValue( beanList, JSONArray .class );
         logger.debug("jsonArray: "+ jsonArray.toString(2));
         assertEquals("Get JSONObject from jsonArray failed ", "jim jar", ((JSONObject)jsonArray.get( 0 )).get( "name" ));
    }
    
    // support for org.json mapping 
    @Test
    public void testSerToORGJSONObject() throws Exception {
        Bean bean = new Bean();
        bean.setName("joe");
        bean.setAge(12);
        JSONObject jsonObject =((Jackson2MapperService)sc).getMapper().convertValue( bean, JSONObject.class );
        logger.debug("jsonObject: "+ jsonObject.toString(2));
        assertEquals("Get name from jsonObject failed ", "joe", jsonObject.get( "name" ));
    }
    
    @Test
    public void testFilteredBeanAfterDeSerToORGJSONObject() throws Exception {
        Bean bean = new Bean();
        bean.setName("joe2");
        bean.setAge(12);
        //same as ((Jackson2MapperService)sc).getMapper().convertValue( bean, JSONObject.class );
        JSONObject jsonObject =((Jackson2MapperService)sc).deSer( bean, JSONObject.class );
        logger.debug("jsonObject: "+ jsonObject.toString(2));
        //  without no filter is applied, filter on original bean requires new mapper, bug ?
        ((Jackson2MapperService)sc).initialize();// need to get fresh mapper
        String[] filterAttr = {"name" };
        String filteredBean = sc.serializeOnlyFilter(bean, Bean.class, filterAttr);
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe2\"}", filteredBean);
        logger.debug("filteredBean: "+ filteredBean.toString());
    }
    
    @Test
    public void testFilteredJSONObjectAfterDeSerToORGJSONObject() throws Exception {
        Bean bean = new Bean();
        bean.setName("joe3");
        bean.setAge(22);
        // alternatively two step instead of calling convertValue, which does not affect filtering
        String serBean = sc.ser( bean, Bean.class );
        JSONObject jsonObject = sc.deSer( serBean, JSONObject.class );
        logger.debug("jsonObject: "+ jsonObject.toString(2));
        // retrieve hashmap to allow filtering by SimpleBeanPropertyFilter 
        String serJsonObject = sc.ser( jsonObject, JSONObject.class );
        Map mapOfJsonObject = sc.deSer( serJsonObject, HashMap.class );
        String[] filterAttr = {"name" };
        String filteredJsonObject = sc.serializeOnlyFilter(mapOfJsonObject, filterAttr);
        assertEquals("Ser filtered JSONObject failed ", "{\"name\":\"joe3\"}", filteredJsonObject);
        logger.debug("filteredJsonObject: "+ filteredJsonObject.toString());
        // works without init
        String filteredBean = sc.serializeOnlyFilter(bean, Bean.class, filterAttr);
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe3\"}", filteredBean);
        logger.debug("filteredBean: "+ filteredBean.toString());
    }

}
