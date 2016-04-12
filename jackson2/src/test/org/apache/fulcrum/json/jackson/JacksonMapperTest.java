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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

/**
 * Jackson2 JSON Test
 * 
 * Test without type setting 
 * 
 * @author gk
 * @version $Id$
 */
public class JacksonMapperTest extends BaseUnit4Test {
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    private JsonService sc = null;
    Logger logger;

    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
    }

    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals("Serialization failed ", preDefinedOutput, serJson);
    }

    @Ignore
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals("DeSer failed ", TestClass.class, deson.getClass());
    }
    @Test
    public void testSerializeDateWithDefaultDateFormat() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        assertTrue("Serialize with Adapater failed ",
                serJson.matches("\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}"));
    }
    @Test
    public void testDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = ((Jackson2MapperService) sc).ser(map, Map.class);
        Map serDate = sc.deSer(serJson, Map.class);
        assertEquals("Date DeSer failed ", String.class, serDate.get("date")
                .getClass());
    }
    @Test
    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe\"}", bean);

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
                Rectangle.class, "w", "name");
        assertEquals("Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", rectangle);

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
                "Serialization of beans failed ",
                "[{'name':'joe0','age':0},{'name':'joe1','age':1},{'name':'joe2','age':2},{'name':'joe3','age':3},{'name':'joe4','age':4},{'name':'joe5','age':5},{'name':'joe6','age':6},{'name':'joe7','age':7},{'name':'joe8','age':8},{'name':'joe9','age':9}]",
                result.replace('"', '\''));
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
        assertTrue("DeSer failed ", beanList2.size() == 10);
        for (Bean bean : beanList2) {
            assertEquals("DeSer failed ", Bean.class, bean.getClass());
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
        assertTrue("DeSer failed ", beanList2 instanceof List);
        assertTrue("DeSer failed ", ((List) beanList2).size() == 10);
        for (int i = 0; i < ((List) beanList2).size(); i++) {
            assertTrue("DeSer failed ",
                    ((List) beanList2).get(i) instanceof Map);
            assertTrue(
                    "DeSer failed ",
                    ((Map) ((List) beanList2).get(i)).get("name").equals(
                            "joe" + i));
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
         assertEquals("DeSer failed ", "jim jar", ((JSONObject)(jsonOrgResult.get(0))).get("name") );
         assertEquals("DeSer failed ", 45, ((JSONObject)(jsonOrgResult.get(1))).get("age") );      
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
                .addAdapter("M4RMixin", Rectangle.class, Mixin.class).ser(
                        filteredRectangle);
        assertEquals("Ser failed ", "{\"width\":5}", serRect);
    }
    @Test
    public void testSerializeWith2Mixins() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");

        String serRect = sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class).ser(filteredRectangle);
        assertEquals("Ser failed ", "{\"name\":\"jim\",\"width\":5}", serRect);

        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe\"}", bean);
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
                "Serialization of beans failed ",
                "[{'name':'joe0'},{'name':'joe1'},{'name':'joe2'},{'name':'joe3'},{'name':'joe4'},{'name':'joe5'},{'name':'joe6'},{'name':'joe7'},{'name':'joe8'},{'name':'joe9'}]",
                result.replace('"', '\''));
    }
    @Test
    public void testDeSerUnQuotedObject() throws Exception {
        String jsonString = "{name:\"joe\"}";
        Bean result = sc.deSer(jsonString, Bean.class);
        assertTrue("expected bean object!", result instanceof Bean);
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
            assertEquals("deser reread size failed", (i * i), ((List<Rectangle>)resultList0)
                    .get(i).getSize());
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
        assertTrue("DeSer failed ", beanList2 instanceof List);
        assertTrue("DeSer failed ", ((List) beanList2).size() == 10);
        for (int i = 0; i < ((List) beanList2).size(); i++) {
            assertTrue("DeSer failed ",
                    ((List) beanList2).get(i) instanceof Map);
            assertTrue(
                    "DeSer failed ",
                    ((Map) ((List) beanList2).get(i)).get("name").equals(
                            "joe" + i));
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

        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "DeSer failed ",
                "[{'width':25},{'width':250},{'name':'joe0'},{'name':'joe1'},{'name':'joe2'}]",
                serRect.replace('"', '\''));
        
        // adding h and name for first two items, adding width for beans
        String deSerTest = "[{\"width\":25,\"age\":99, \"h\":50,\"name\":\"rect1\"},{\"width\":250,\"name\":\"rect2\"},{\"name\":\"joe0\"},{\"name\":\"joe1\"},{\"name\":\"joe2\"}]";
        
        List typeRectList = new ArrayList(); //empty
        // could not use Mixins here, but Adapters are still set
        Collection<Rectangle> resultList0 =  sc.deSerCollection(deSerTest, typeRectList, Rectangle.class);
        System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 5; i++) {
            // name and h should be null as it is ignored,  cft. Mixin
            assertTrue(((List<Rectangle>)resultList0).get(i).getName()==null);
            assertTrue(((List<Rectangle>)resultList0).get(i).getH()==0);
        }
        // could not use Mixins here, but Adapters are still set
        Collection<Bean> resultList1 =  sc.deSerCollection(deSerTest, typeRectList, Bean.class);
        System.out.println("resultList1 class:" +resultList1.getClass());
        for (int i = 0; i < 5; i++) {
            System.out.println("resultList1 "+i+ " name:"+((List<Bean>)resultList1).get(i).getName());
            // name should NOT be null, age should be ignored, cft. BeanMixin
            assertTrue(((List<Bean>)resultList1).get(i).getName()!=null);
            assertTrue(((List<Bean>)resultList1).get(i).getAge()==0);
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

    // @JsonFilter("myFilter")
    static class Bean {
        private String name;
        private int age;
        public String profession;

        public Bean() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static abstract class Mixin2 {
        void MixIn2(int w, int h) {
        }

        @JsonProperty("width")
        abstract int getW(); // rename property

        @JsonIgnore
        abstract int getH();

        @JsonIgnore
        abstract int getSize(); // exclude

        abstract String getName();
    }

    public static abstract class BeanMixin {
        void BeanMixin() {
        }

        @JsonIgnore
        abstract int getAge();

        @JsonIgnore
        String profession; // exclude

        @JsonProperty
        abstract String getName();//
    }

}
