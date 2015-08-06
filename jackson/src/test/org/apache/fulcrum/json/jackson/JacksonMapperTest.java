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
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.junit.Before;
import org.junit.Test;

/**
 * Jackson1 JSON Test
 * 
 * @author gk
 * @version $Id$
 */
public class JacksonMapperTest extends BaseUnit4Test {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
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
    @Test
    public void testSerializeDateWithDefaultDateFormat() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        assertTrue("Serialize with Adapater failed ",
                serJson.matches("\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}"));

    }
    @Test
    public void testDeSerialize1() throws Exception {

        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("name", 5001);
        Map deserMap = (Map) sc.deSer(sc.ser(map), Map.class);
        assertEquals("Integer DeSer failed ", 5001, deserMap.get("name"));

    }
    @Test
    public void testSerializeSingleObjectExcludeWithMixins() throws Exception {
        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class);
        String serRect = sc.ser(new Rectangle(25, 3));
        assertEquals("DeSer failed ", "{\"width\":25}", serRect);

    }
    @Test
    public void testSerializeTwoObjectsIncludeOnlyAnnotationCustomFilterId()
            throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "name");
        assertEquals("Ser filtered Bean failed ", "{\"name\":\"joe\"}", bean);
        logger.debug("bean: " + bean);

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
                Rectangle.class, "w", "name");
        assertEquals("Ser filtered Rectangle failed ",
                "{\"w\":5,\"name\":\"jim\"}", rectangle);
        logger.debug("rectangle: " + rectangle);

    }
    @Test
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals("DeSer failed ", TestClass.class, deson.getClass());
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
        assertEquals("Ser failed ", "{\"width\":5}", serRect);
    }
    @Test
    public void testMixis2() throws Exception {
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
        assertTrue("DeSer failed ", beanList2 instanceof List);
        assertTrue("DeSer failed ", beanList2.size() == 10);
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
        assertTrue("DeSer failed ", beanList2 instanceof List);
        assertTrue("DeSer failed ", beanList2.size() == 10);
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
