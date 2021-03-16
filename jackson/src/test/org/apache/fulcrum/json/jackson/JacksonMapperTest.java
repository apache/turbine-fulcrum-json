package org.apache.fulcrum.json.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * Jackson1 JSON Test
 * 
 * @author gk
 * @version $Id$
 */
@RunWith(JUnitPlatform.class)
public class JacksonMapperTest extends BaseUnit5Test {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    Logger logger;

    @BeforeEach
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
    }
    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals(preDefinedOutput, serJson, "Serialization failed ");
    }
    @Test
    public void testSerializeDateWithDefaultDateFormat() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        assertTrue(serJson.matches("\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}"),
                "Serialize with Adapater failed");

    }
    @Test
    public void testDeSerialize1() throws Exception {

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("name", 5001);
        Map deserMap = (Map) sc.deSer(sc.ser(map), Map.class);
        assertEquals(5001, deserMap.get("name"), "Integer DeSer failed");

    }
    @Test
    public void testSerializeSingleObjectExcludeWithMixins() throws Exception {
        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class);
        String serRect = sc.ser(new Rectangle(25, 3));
        assertEquals("{\"width\":25}", serRect, "DeSer failed");

    }
    @Test
    public void testSerializeTwoObjectsIncludeOnlyAnnotationCustomFilterId()
            throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", bean, "Ser filtered Bean failed ");
        logger.debug("bean: " + bean);

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
                Rectangle.class, "w", "name");
        assertEquals( "{\"w\":5,\"name\":\"jim\"}", rectangle,
                "Ser filtered Rectangle failed ");
        logger.debug("rectangle: " + rectangle);

    }
    @Test
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals(TestClass.class, deson.getClass(), "DeSer failed ");
    }

//    public void testDeserializationCollection() throws Exception {
//        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
//        for (int i = 0; i < 10; i++) {
//            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
//            rectList.add(filteredRect);
//        }
//        String serColl = sc.ser(rectList);
//
//        List<Rectangle> typeRectList = new ArrayList<Rectangle>(); //empty
//        System.out.println("serColl:" + serColl);
//        Collection<Rectangle> resultList0 =   sc.deSerCollection(serColl, typeRectList, Rectangle.class);
//        
//        for (int i = 0; i < 10; i++) {
//            assertEquals("deser reread size failed", i , ((List<Rectangle>)resultList0)
//                    .get(i).getW());
//        }
//    }
    @Test
    public void testMixins() throws Exception {

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String serRect = sc
                .addAdapter("M4RMixin", Rectangle.class, Mixin.class).ser(
                        filteredRectangle);
        assertEquals("{\"width\":5}", serRect, "Ser failed ");
    }
    @Test
    public void testMixis2() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");

        String serRect = sc.addAdapter("M4RMixin2", Rectangle.class,
                Mixin2.class).ser(filteredRectangle);
        assertEquals("{\"name\":\"jim\",\"width\":5}", serRect,
                "Ser failed ");

        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("{\"name\":\"joe\"}", bean, 
                "Ser filtered Bean failed ");
    }
    @Test
    public void testFilteredCollectionOfBeans() throws Exception {
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.serializeOnlyFilter(beanList, Bean.class, "name",
                "age");
        logger.debug("result: " + result);
        Object objResult = ((JacksonMapperService) sc).deSer(result,
                List.class, Bean.class);
        List<Bean> beanList2 = (List<Bean>) objResult;
        assertTrue(beanList2 instanceof List, "DeSer Type failed ");
        assertTrue(beanList2.size() == 10, "DeSer size failed ");
        for (Bean bean : beanList2) {
            logger.debug("deser bean: " + bean.getName() + " is "
                    + bean.getAge());
        }
    }
    @Test
    public void testMixinCollectionOfBeans() throws Exception {
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 10; i++) {
            Bean filteredBean = new Bean();
            filteredBean.setName("joe" + i);
            filteredBean.setAge(i);
            beanList.add(filteredBean);
        }
        String result = sc.addAdapter("M4RMixin", Bean.class, BeanMixin.class)
                .ser(beanList);
        logger.debug("result: " + result);
        Object objResult = ((JacksonMapperService) sc).deSer(result,
                List.class, Bean.class);
        List<Bean> beanList2 = (List<Bean>) objResult;
        assertTrue(beanList2 instanceof List, "DeSer failed ");
        assertTrue(beanList2.size() == 10, "DeSer failed ");
        for (Bean bean : beanList2) {
            logger.debug("deser bean: " + bean.getName() + " is "
                    + bean.getAge());
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
