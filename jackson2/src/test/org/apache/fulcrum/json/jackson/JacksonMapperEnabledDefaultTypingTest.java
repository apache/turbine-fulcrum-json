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
 * @version $Id$
 */
public class JacksonMapperEnabledDefaultTypingTest extends BaseUnitTest {
    private JsonService sc = null;
    Logger logger;

    /**
     * Constructor for test.
     * 
     * @param testName
     *            name of the test being executed
     */
    public JacksonMapperEnabledDefaultTypingTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
        ((Jackson2MapperService) sc).getMapper().enableDefaultTypingAsProperty(
                DefaultTyping.NON_FINAL, "type");
    }

    public void testSerialize() throws Exception {
        String serJson = sc.ser(new JacksonMapperEnabledDefaultTypingTest(
                "mytest"));
        assertEquals(
                "Set failed ",
                "{\"type\":\"org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest\",\"name\":\"mytest\"}",
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
        assertTrue(
                "Serialize with Adapater failed ",
                serJson.matches(".*\"java.util.Date\",\"\\d\\d/\\d\\d/\\d{4}\".*"));

    }

    public void testDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = ((Jackson2MapperService) sc).ser(map, Map.class);
        Map<String, Date> serDate = (Map<String, Date>) sc.deSer(serJson,
                Map.class);
        assertEquals("Date DeSer failed ", Date.class, serDate.get("date")
                .getClass());
    }

    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"type\":\"org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean\",\"name\":\"joe\"}",
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
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe0','age':0},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe1','age':1},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe2','age':2},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe3','age':3},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe4','age':4},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe5','age':5},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe6','age':6},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe7','age':7},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe8','age':8},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe9','age':9}]]",
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
        List<Bean> beanList2 = (List<Bean>) ((Jackson2MapperService) sc)
                .deSerCollection(result, beanList, Bean.class);
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

        String serRect = sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class).ser(filteredRectangle);
        assertEquals("Ser failed ", "{\"name\":\"jim\",\"width\":5}", serRect);

        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"type\":\"org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean\",\"name\":\"joe\"}",
                bean);
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
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe2'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe3'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe4'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe5'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe6'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe7'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe8'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe9'}]]",
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
        List<Bean> beanList2 = (List<Bean>) ((Jackson2MapperService) sc)
                .deSerCollection(result, beanList, Bean.class);
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

        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "Serialization failed ",
                "['java.util.ArrayList',[{'type':'org.apache.fulcrum.json.Rectangle','width':25},{'type':'org.apache.fulcrum.json.Rectangle','width':250},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.JacksonMapperEnabledDefaultTypingTest$Bean','name':'joe2'}]]",
                serRect.replace('"', '\''));

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
