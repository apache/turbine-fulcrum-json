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

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

/**
 * Jackson2 JSON Test with EnabledDefaultTyping {@link DefaultTyping#NON_FINAL} the most verbose type information.  
 * 
 * cft. http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 * 
 * @author gk
 * @version $Id$
 */
public class JacksonMapperEnabledDefaultTypingTest extends BaseUnit4Test {
    private final String preDefinedOutput = "{\"type\":\"org.apache.fulcrum.json.TestClass\",\"container\":{\"type\":\"java.util.HashMap\",\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    private JsonService sc = null;
    Logger logger;


    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
        ((Jackson2MapperService) sc).getMapper().enableDefaultTypingAsProperty(
                DefaultTyping.NON_FINAL, "type");
    }
    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals("Serialization failed ", preDefinedOutput, serJson);
    }
    @Test
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
        System.out.println("serJson:" +serJson);
        assertEquals(
                "Serialize with Adapater failed ",true,
                serJson.matches(".*\"java.util.Date\",\"\\d\\d/\\d\\d/\\d{4}\".*"));
    }
    @Test
    public void testDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson0 =  sc.ser(map);
        System.out.println("serJson0:"+ serJson0);
        String serJson =  sc.ser(map, Map.class);
        System.out.println("serJsonwithmap:"+ serJson);
        Map<String, Date> serDate = sc.deSer(serJson, Map.class);
        assertEquals("Date DeSer failed ", Date.class, serDate.get("date")
                .getClass());   
    }
    @Test
    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"type\":\"org.apache.fulcrum.json.jackson.Bean\",\"name\":\"joe\"}",
                bean);

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
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0','age':0},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1','age':1},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2','age':2},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3','age':3},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4','age':4},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5','age':5},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6','age':6},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7','age':7},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8','age':8},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9','age':9}]]",
                result.replace('"', '\''));
        
        Collection<Rectangle> resultDeSer = checkDeserCollection(result, List.class, Rectangle.class);
    }
    
    private <T> T checkDeserialization(String serJson, Class<T> target, Class mixin) throws Exception {
        sc.addAdapter("TestClass Adapter", target, mixin);
        T result = sc.deSer(serJson,target);
        //System.out.println("result:"+ result + " is of type "+ target.getName());
        assertTrue("result instance check", target.isAssignableFrom(result.getClass()));
        return result;
     }
    
    private <U> Collection<U> checkDeserCollection(String serJson,Class<? extends Collection> collClass, Class entryClass) throws Exception {
          Collection<U> result = ((Jackson2MapperService) sc).deSerCollectionWithType(serJson, collClass, entryClass);
          assertTrue("result instance check failed for result class "+ result.getClass() + " and target class: "+ collClass, 
                  collClass.isAssignableFrom(result.getClass()));
          return result;
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
        //System.out.println("res:" +result);
        // serialized json has type for list (ArrayList) and type for elements (Bean) -> TypeReference could be used
        TypeReference typeRef = new TypeReference<List<Bean>>(){};
        List<Bean> beanList2 = (List<Bean>) ((Jackson2MapperService) sc)
                .deSerCollection(result, typeRef, Bean.class);
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
                    ((List) beanList2).get(i) instanceof Bean);
            assertTrue("DeSer failed ", ((Bean) ((List) beanList2).get(i))
                    .getName().equals("joe" + i));

        }
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

       String serRect =  sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class).ser(filteredRectangle);
        assertEquals("Ser failed ", "{\"name\":\"jim\",\"width\":5}", serRect);

        //
        String bean = sc.addAdapter("M4RBeanMixin", Bean.class,
                BeanMixin.class).ser(filteredBean);;
        
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"type\":\"org.apache.fulcrum.json.jackson.Bean\",\"name\":\"joe\"}",
                bean);
    }
    @Test
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
                "{\"type\":\"org.apache.fulcrum.json.jackson.Bean\"}",
                bean);
    }
    @Test
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
                "{\"type\":\"org.apache.fulcrum.json.jackson.Bean\",\"profession\":\"\"}",
                bean);
    }
    @Test
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
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9'}]]",
                result.replace('"', '\''));
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
        //System.out.println("result:::"+ result);
        TypeReference typeRef = new TypeReference<List<Bean>>(){};
        List<Bean> beanList2 = (List<Bean>) ((Jackson2MapperService) sc)
                .deSerCollection(result, typeRef, Bean.class);
        assertTrue("DeSer failed ", beanList2.size() == 10);
        for (Bean bean : beanList2) {
            assertEquals("DeSer failed ", Bean.class, bean.getClass());
        }
    }
    @Test
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
        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "Serialization failed ",
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.Rectangle','width':25},{'type':'org.apache.fulcrum.json.Rectangle','width':250},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'}]]",
                serRect.replace('"', '\''));
    }
    @Test
    public void testSerializeCollectionWithOnlyFilter() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        assertEquals("[\"java.util.ArrayList\",[{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":0},{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":1}]]", 
                sc.serializeOnlyFilter(rectList, TypedRectangle.class, "w"));
    }
    
    // @JsonFilter("myFilter")
   
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
}
