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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.json.jackson.example.TestClass;
import org.apache.fulcrum.json.jackson.mixins.BeanMixin;
import org.apache.fulcrum.json.jackson.mixins.RectangleMixin;
import org.apache.fulcrum.json.jackson.mixins.RectangleMixin2;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.apache.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

/**
 * Jackson2 JSON Test
 * 
 * Test without type setting 
 * 
 * @author gk
 * @version $Id$
 */
@RunWith(JUnitPlatform.class)
public class JacksonMapperTest extends BaseUnit5Test {
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    private JsonService sc = null;
    Logger logger;

    @BeforeEach
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new Log4JLogger(LogManager.getLogger(getClass().getName()) );
    }

    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals(preDefinedOutput, serJson, "Serialization failed ");
    }

    @Disabled
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals(TestClass.class, deson.getClass(), "DeSer failed ");
    }
    @Test
    public void testSerializeDateWithDefaultDateFormat() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        assertTrue(serJson.matches("\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}"), "Serialize with Adapater failed ");
    }
    @Test
    public void testDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = ((Jackson2MapperService) sc).ser(map, Map.class);
        Map serDate = sc.deSer(serJson, Map.class);
        assertEquals(String.class, serDate.get("date")
                .getClass(), "Date DeSer failed ");
    }
    @Test
    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", bean, "Serialization of bean failed ");

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
                Rectangle.class, "w", "name");
        assertEquals("{\"w\":5,\"name\":\"jim\"}", 
                     rectangle,
                     "Ser filtered Rectangle failed ");

    }
    @Test
    public void testSerializationCollectionWithFilter() throws Exception {

        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.serializeOnlyFilter(beanList, Bean.class, "name",
                "age");
        assertEquals(
                "[{'name':'joe0','age':0},{'name':'joe1','age':1},{'name':'joe2','age':2},{'name':'joe3','age':3},{'name':'joe4','age':4},{'name':'joe5','age':5},{'name':'joe6','age':6},{'name':'joe7','age':7},{'name':'joe8','age':8},{'name':'joe9','age':9}]",
                result.replace('"', '\''),
                "Serialization of beans failed ");
    }
    
    @Test
    public void serializeMapWithListandString() throws Exception {
        Map<String,Object> wrapper = new HashMap();
        List myList = new ArrayList();
        myList.add(new TestClass() );
        wrapper.put( "list",myList );
        if (wrapper != null) {
            wrapper.put( "testkey", "xxxxx" );
            logger.info( String.format("list has size: %s", wrapper.size()));
        }
        String serialized =  sc.ser( wrapper ); // sc.ser( wrapper, TestClass.class );
        // {"testkey":"xxxxx","list":[{"container":"","configurationName":"Config.xml","name":""}]}
        // {"testkey":"xxxxx","list":[{"container":"","configurationName":"Config.xml","name":""}]}
        logger.info( String.format("serialized results: %s",serialized) );

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
        String result = sc.serializeOnlyFilter(beanList, Bean.class, "name",
                "age");
        List<Bean> beanList2 = (List<Bean>) ((Jackson2MapperService) sc)
                .deSerCollectionWithType(result, List.class, Bean.class);
        assertTrue(beanList2.size() == 10, "DeSer failed ");
        for (Bean bean : beanList2) {
            assertEquals(Bean.class, bean.getClass(), "DeSer failed ");
        }
    }
    @Test
    public void testDeserializationUnTypedCollectionWithFilter()
            throws Exception {

        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.serializeOnlyFilter(beanList, Bean.class, "name",
                "age");
        Object beanList2 = sc.deSer(result, List.class);
        assertTrue(beanList2 instanceof List,"DeSer failed, no List ");
        assertTrue(((List) beanList2).size() == 10, "DeSer failed size not 10");
        for (int i = 0; i < ((List) beanList2).size(); i++) {
            assertTrue(
                    ((List) beanList2).get(i) instanceof Map, "DeSer failed: no map");
            assertTrue(
                    
                    ((Map) ((List) beanList2).get(i)).get("name").equals(
                            "joe" + i), "DeSer failed: name not joe");
        }
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
   
         sc.addAdapter(null, null,new JsonOrgModule());
         //((Jackson2MapperService)sc).registerModule(new JsonOrgModule());
         
         JSONArray jsonOrgResult = sc.deSer(filteredSerList, JSONArray.class);//readValue(serList, JSONArray.class);
         logger.debug("jsonOrgResult: "+ jsonOrgResult.toString(2));
         assertEquals("jim jar", ((JSONObject)(jsonOrgResult.get(0))).get("name"), "DeSer failed: name not jim jar" );
         assertEquals( 45, ((JSONObject)(jsonOrgResult.get(1))).get("age"), "DeSer failed: age != 45" ); 
    }
    
    // support for org.json mapping 
    @Test
    public void testSerToORGJSONCollectionObject() throws Exception {
  
        // test array
         List<Bean> userResults = new ArrayList<Bean> ( );
         Bean tu = new Bean();
         tu.setName("jim jar");
         userResults.add(tu);
         Bean tu2 = new Bean();
         tu2.setName("jim2 jar2");
         tu2.setAge(45);
         userResults.add(tu2);
         
         String[] filterAttr = {"name", "age" };
         
         sc.addAdapter(null, null,new JsonOrgModule());
         //((Jackson2MapperService)sc).registerModule(new JsonOrgModule());
         String filteredSerList = sc.serializeOnlyFilter(userResults, Bean.class, filterAttr);
         logger.debug("serList: "+ filteredSerList);
         
    }
    
    @Test
    public void testSerializeWithMixin() throws Exception {
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String serRect = sc
                .addAdapter("M4RMixin", Rectangle.class, RectangleMixin.class).ser(
                        filteredRectangle);
        assertEquals("{\"width\":5}", serRect, "Ser failed ");
    }
    @Test
    public void testSerializeWith2Mixins() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");

        String serRect = sc.addAdapter("M4RMixin2", Rectangle.class,
                RectangleMixin2.class).ser(filteredRectangle);
        assertEquals("{\"name\":\"jim\",\"width\":5}", serRect,"Ser failed ");

        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", bean, "Ser filtered Bean failed ");
    }
    @Test
    public void testSerializationCollectionWithMixin() throws Exception {

        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.addAdapter("M4RMixin", Bean.class, BeanMixin.class)
                .ser(beanList);
        assertEquals(
                "[{'name':'joe0'},{'name':'joe1'},{'name':'joe2'},{'name':'joe3'},{'name':'joe4'},{'name':'joe5'},{'name':'joe6'},{'name':'joe7'},{'name':'joe8'},{'name':'joe9'}]",
                result.replace('"', '\''),
                "Serialization of beans failed ");
    }
    @Test
    public void testDeSerUnQuotedObject() throws Exception {
        String jsonString = "{name:\"joe\"}";
        Bean result = sc.deSer(jsonString, Bean.class);
        assertTrue(result instanceof Bean, "expected bean object!");
    }
    
    public void testDeserializationCollection2() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        Collection<Rectangle> resultList0 =  ((Jackson2MapperService) sc) .deSerCollectionWithType(serColl, ArrayList.class, Rectangle.class);
        
        for (int i = 0; i < 10; i++) {
            assertEquals((i * i), ((List<Rectangle>)resultList0)
                    .get(i).getSize(), "deser reread size failed");
        }
    }
    @Test
    public void testDeSerializationCollectionWithMixin() throws Exception {

        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.addAdapter("M4RMixin", Bean.class, BeanMixin.class)
                .ser(beanList);
        Object beanList2 = sc.deSer(result,
                List.class);
        assertTrue( beanList2 instanceof List, "DeSer failed" );
        assertTrue( ((List) beanList2).size() == 10, "DeSer failed ");
        for (int i = 0; i < ((List) beanList2).size(); i++) {
            assertTrue(
                    ((List) beanList2).get(i) instanceof Map, "DeSer failed ");
            assertTrue(
                    ((Map) ((List) beanList2).get(i)).get("name").equals(
                            "joe" + i), "DeSer failed ");
        }
    }
    @Test
    public void testCollectionWithMixins() throws Exception {
        List<Object> components = new ArrayList<Object>();
        components.add(new Rectangle(25, 3));
        components.add(new Rectangle(250, 30));
        for (int i = 0; i < 3; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            components.add(filteredBean);
        }

        sc.addAdapter("M4RMixin", Rectangle.class, RectangleMixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "[{'width':25},{'width':250},{'name':'joe0'},{'name':'joe1'},{'name':'joe2'}]",
                serRect.replace('"', '\''),  "DeSer failed ");
        
        // adding h and name for first two items, adding width for beans
        String deSerTest = "[{\"width\":25,\"age\":99, \"h\":50,\"name\":\"rect1\"},{\"width\":250,\"name\":\"rect2\"},{\"name\":\"joe0\"},{\"name\":\"joe1\"},{\"name\":\"joe2\"}]";
        
        List<Rectangle> typeRectList = new ArrayList<>(); //empty
        // could not use Mixins here, but Adapters are still set
        Collection<Rectangle> resultList0 =  sc.deSerCollection(deSerTest, typeRectList, Rectangle.class);
        logger.debug("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 5; i++) {
            // name and h should be null as it is ignored,  cft. Mixin
            assertTrue(((List<Rectangle>)resultList0).get(i).getName()==null);
            assertTrue(((List<Rectangle>)resultList0).get(i).getH()==0);
        }
        // could not use Mixins here, but Adapters are still set
        Collection<Bean> resultList1 =  sc.deSerCollection(deSerTest, typeRectList, Bean.class);
        logger.debug("resultList1 class:" +resultList1.getClass());
        for (int i = 0; i < 5; i++) {
            logger.debug("resultList1 "+i+ " name:"+((List<Bean>)resultList1).get(i).getName());
            // name should NOT be null, age should be ignored, cft. BeanMixin
            assertTrue(((List<Bean>)resultList1).get(i).getName()!=null);
            assertTrue(((List<Bean>)resultList1).get(i).getAge()==-1);
        }
        ((Initializable)sc).initialize();// reinit to default settings
        Collection<Rectangle> resultList3 =  sc.deSerCollection(deSerTest, typeRectList, Rectangle.class);
        // h should be set again without Mixin
        assertTrue(((List<Rectangle>)resultList3).get(0).getH()!=0);
        for (int i = 0; i < 5; i++) {
            // name should be set without Mixin
            assertTrue(((List<Rectangle>)resultList3).get(i).getName()!=null);
        }
    }

}
