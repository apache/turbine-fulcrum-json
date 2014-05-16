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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnitTest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;


/**
 * Jackson2 JSON Test with EnabledDefaultTyping
 * 
 * cft. http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 * 
 * @author gk
 * @version $Id: JacksonMapperEnabledDefaultTypingTest.java 1577518 2014-03-14 13:21:39Z gk $
 */
public class JacksonMapperEnabledDefaultTyping_OBJECT_AND_NON_CONCRETE_Test extends BaseUnitTest {
    private JsonService sc = null;
    Logger logger;

    /**
     * Constructor for test.
     * 
     * @param testName
     *            name of the test being executed
     */
    public JacksonMapperEnabledDefaultTyping_OBJECT_AND_NON_CONCRETE_Test(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
        ((Jackson2MapperService) sc).getMapper().enableDefaultTypingAsProperty(
                DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
    }

    public void testSerialize() throws Exception {
        String serJson = sc.ser(new JacksonMapperEnabledDefaultTyping_OBJECT_AND_NON_CONCRETE_Test(
                "mytest"));
        assertEquals(
                "Set failed ",
                "{\"name\":\"mytest\"}",
                serJson);
    }

    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals("DeSer failed ", TestClass.class, deson.getClass());
    }

    public void testSerializeDateWithDefaultDateFormat() throws Exception {

        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        //System.out.println("serJson:" +serJson);
        assertTrue(
                "Serialize with Adapater failed ",
                serJson.matches(".*\"java.util.Date\",\"\\d\\d/\\d\\d/\\d{4}\".*"));
    }

    public void testDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("mydate",Calendar.getInstance().getTime());
        Calendar mydate2 = Calendar.getInstance();
        mydate2.set(1999, 3, 10);
        map.put("mydate2",mydate2.getTime());
        String serJson0 =  sc.ser(map, false);
        String serJson =  sc.ser(map, Map.class, false);
        System.out.println("serJson:"+ serJson0);
        assertEquals(serJson0, serJson);
        sc.addAdapter("Collection Adapter", Object.class, DateKeyMixin.class);
        DateKeyMixin serObject =sc.deSer(serJson0, DateKeyMixin.class);
//        System.out.println("serObject:"+ serObject);
//        System.out.println("serObject:"+ serObject.mydate.getClass());
        assertTrue(serObject.mydate instanceof Date);
        assertTrue(serObject.mydate2 instanceof Date);
        assertTrue(serObject.mydate2.toString().equals("Sat Apr 10 00:00:00 CEST 1999"));
//        System.out.println("serObject:"+ ((Date)serObject.mydate));
//        System.out.println("serObject:"+ ((Date)serObject.mydate2));
    }

    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"name\":\"joe\"}",
                bean);

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
                Rectangle.class, "w", "name");
        assertEquals("Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", rectangle);
    }

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
                "[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0','age':0},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1','age':1},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2','age':2},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3','age':3},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4','age':4},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5','age':5},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6','age':6},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7','age':7},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8','age':8},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9','age':9}]",
                result.replace('"', '\''));
    }

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
        //System.out.println("res:"+result);
        // could not use TypeReference as JSON string has no type set for array:
        // com.fasterxml.jackson.databind.JsonMappingException: Unexpected token (START_OBJECT), expected VALUE_STRING:
        // need JSON String that contains type id (for subtype of java.util.Collection)
        // 
        // -> need to use constructCollectionType        
        Class clazz = Class.forName("org.apache.fulcrum.json.jackson.Bean");
        List<Bean> beanList2 = (List<Bean>)sc.deSerCollection(result, new ArrayList(),clazz);
        assertTrue("DeSer failed ", beanList2.size() == 10);
        for (Bean bean : beanList2) {
            assertEquals("DeSer failed ", Bean.class, bean.getClass());
        }
    }

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
        // could not use TypeReference as JSON string has no type set for array:
        // Exception: need JSON String that contains type id (for subtype of java.util.List)
        // -> need to use constructCollectionType
        Class clazz = Class.forName("org.apache.fulcrum.json.jackson.Bean");
        List<Bean> beanList2 = (List<Bean>)sc.deSerCollection(result, new ArrayList(),clazz);
        //Object beanList2 = sc.deSer(result, List.class);
        assertTrue("DeSer failed ", beanList2 instanceof List);
        assertTrue("DeSer failed ", ((List) beanList2).size() == 10);
        for (int i = 0; i < ((List) beanList2).size(); i++) {
            assertTrue("DeSer failed ",
                    ((List) beanList2).get(i) instanceof Bean);
            assertTrue("DeSer failed ", ((Bean) ((List) beanList2).get(i))
                    .getName().equals("joe" + i));

        }
    }

    public void testSerializeWithMixin() throws Exception {
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String serRect = sc
                .addAdapter("M4RMixin", Rectangle.class, Mixin.class).ser(
                        filteredRectangle);
        assertEquals("Ser failed ", "{\"width\":5}", serRect);
    }

    public void testSerializeWith2Mixins() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");

       String serRect =  sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class).ser(filteredRectangle);
        assertEquals("Ser failed ", "{\"name\":\"jim\",\"width\":5}", serRect);

        //
        String bean = sc.addAdapter("M4RBeanMixin", Bean.class,
                BeanMixin.class).ser(filteredBean);;
        
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"name\":\"joe\"}",
                bean);
    }
    
    public void testSerializeWithMixinAndFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        //
        sc.addAdapter("M4RBeanMixin", Bean.class,
                BeanMixin.class);
        // profession was already set to ignore, does not change
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "profession");
        assertEquals(
                "Ser filtered Bean failed ",
                "{}",
                bean);
    }
    
    public void testSerializeWithUnregisteredMixinAndFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        //
        sc.addAdapter("M4RBeanMixin", Bean.class,
                BeanMixin.class)
        .addAdapter("M4RBeanMixin", Bean.class,
                null);
        // now profession is used after cleaning adapter
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "profession");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"profession\":\"\"}",
                bean);
    }

    public void testMultipleSerializingWithMixinAndFilter() throws Exception {
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        //
        sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class);
        
        // if serialization is done Jackson clean cache
        String rectangle0 = sc.ser(filteredRectangle,Rectangle.class,true);
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"name\":\"jim\",\"width\":5}",
                rectangle0);
        // filtering out name, using width from mixin2 as a second filter
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, Rectangle.class, true, "width");
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"width\":5}",
                rectangle);
        // default for mixin
       String rectangle1 = sc.ser(filteredRectangle);
       assertEquals(
              "Ser filtered Rectangle failed ",
              "{\"name\":\"jim\",\"width\":5}",
              rectangle1);
    }

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
                "[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9'}]",
                result.replace('"', '\''));
    }

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
        System.out.println("result:::"+ result);
        // Type List.class / TypeReference -> Exception: need JSON String that contains type id (for subtype of java.util.List)
        // Type: Bean.class -> Exception: Can not deserialize instance of org.apache.fulcrum.json.jackson.Bean out of START_ARRAY token
        
        // -> need to use constructCollectionType
        List<Bean> beanList2 = (List<Bean>)sc.deSerCollection(result, new ArrayList(),Bean.class);
        assertTrue("DeSer failed ", beanList2.size() == 10);
        for (Bean bean : beanList2) {
            assertEquals("DeSer failed ", Bean.class, bean.getClass());
        }
    }

    public void testSerializationCollectionWithMixins() throws Exception {
        List components = new ArrayList<Object>();
        components.add(new Rectangle(25, 3));
        components.add(new Rectangle(250, 30));
        for (int i = 0; i < 3; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            components.add(filteredBean);
        }

        // property w->width, BeanMixin:  name ignore other properties
        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "Serialization failed ",
                "[{'type':'org.apache.fulcrum.json.Rectangle','width':25},{'type':'org.apache.fulcrum.json.Rectangle','width':250},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'}]",
                serRect.replace('"', '\''));
    }
    
    public void testSerializeCollectionWithOnlyFilter() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        assertEquals("[{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":0},{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":1}]",
                sc.serializeOnlyFilter(rectList, TypedRectangle.class, true, "w"));
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
    
    public static class DateKeyMixin  {
//        @JsonCreator
//        static Object create(Map<String, Object> map) {
//            return map; //map.get("date");
//            //return map.get("date");
//        }
        public Object mydate;
        public Object mydate2;
    }
    
    

}
