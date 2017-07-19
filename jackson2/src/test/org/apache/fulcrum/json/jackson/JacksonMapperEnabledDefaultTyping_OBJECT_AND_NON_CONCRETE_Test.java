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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

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
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;


/**
 * Jackson2 JSON Test with EnabledDefaultTyping {@link DefaultTyping#OBJECT_AND_NON_CONCRETE}
 * 
 * cft. http://wiki.fasterxml.com/JacksonPolymorphicDeserialization
 * 
 * @author gk
 * @version $Id$
 */
public class JacksonMapperEnabledDefaultTyping_OBJECT_AND_NON_CONCRETE_Test extends BaseUnit4Test {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"type\":\"java.util.HashMap\",\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    Logger logger;

    @Before
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
        logger = new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
        ((Jackson2MapperService) sc).getMapper().enableDefaultTypingAsProperty(
                DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
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
        logger.debug("serJson:" +serJson);
        assertTrue(
                "Serialize with Adapater failed ",
                serJson.matches(".*\"java.util.Date\",\"\\d\\d/\\d\\d/\\d{4}\".*"));
    }
    @Test
    public void testSerializeDeSerializeDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        Calendar sourceDate = Calendar.getInstance();
        sourceDate.set(1999, 3, 10);
        
//        logger.debug("sourceDate calendar:"+ sourceDate);
        logger.debug("sourceDate date:"+ sourceDate.getTime());
        logger.debug("sourceDate millisec:"+ sourceDate.getTime().getTime());
        map.put("mydate",sourceDate.getTime());
        map.put("mydate2",Calendar.getInstance().getTime());
        // default dateformat dd/mm/yy -> day time will be cut off !(hh, mm)
        // first serialize
        String serJson0 =  sc.ser(map, false);
        String serJson =  sc.ser(map, Map.class, false);
        
        logger.debug("serJson:"+ serJson0);
        assertEquals(serJson0, serJson);
         //second deserialize 
        DateKeyMixin serObject =sc.deSer(serJson0, DateKeyMixin.class);
        assertTrue(serObject.mydate instanceof Date);
        
        logger.debug("resultDate millisec: " + ((Date)serObject.mydate).getTime() +" source:"+ sourceDate.getTime().getTime() );
        // cleanup all values the mapper dateformat, which is MM/dd/yyyy, does not contain.
        sourceDate.set(Calendar.HOUR, 0);
        sourceDate.set(Calendar.MINUTE, 0);
        sourceDate.set(Calendar.SECOND, 0);
        sourceDate.set(Calendar.MILLISECOND, 0);
        sourceDate.set(Calendar.HOUR_OF_DAY, 0);
        assertEquals("millisec of result and source date should be equal, after zeroing not used formatter values:: ",
                ((Date)serObject.mydate).getTime(),sourceDate.getTime().getTime() );
    }
    // all values represented in format of the object, which would be serialized are conserved, while the others are nulled   
    @Test
    public void testSerializeDeSerializeTZDate() throws Exception {
        Map<String, Date> map = new HashMap<String, Date>();
        Calendar sourceDate = Calendar.getInstance(TimeZone.getTimeZone("America/Montreal"));// UTC -5
        sourceDate.set(1999, 3, 10, 11, 10); // set in Montreal Time this date and time
        
        //this may be in "any" locale timezone, eg. 1999-04-10 17:10 PM MESZ
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm a Z");
        // shows date and time in locale time
        logger.debug("sourceDate string:"+ sourceDate.getTime());
        logger.debug("sourceDate format:"+df.format(sourceDate.getTime()));
        
        // any "timezone" information is lost from the Calendar when converting it  into a java.util.Date by calling getTime()
        map.put("mydate",sourceDate.getTime());
        
        sc.setDateFormat(df);
        
        String serJson0 =  sc.ser(map, false);
        String serJson =  sc.ser(map, Map.class, false);
        
        logger.debug("serJson:"+ serJson0);
        assertEquals(serJson0, serJson);
        
        DateKeyMixin serObject =sc.deSer(serJson0
                , DateKeyMixin.class);
        logger.debug("resultDate (serialized) millisec: " +
                ((Date)serObject.mydate).getTime() +" source:"+ sourceDate.getTime().getTime() );

        logger.debug("may not be equal: millisec(resultDate(string)):" + ((Date)serObject.mydate).getTime() + 
                " millisec(sourceDate):"+ sourceDate.getTime().getTime() );
        // cleanup all values the mapper dateformat does not contain.
        sourceDate.set(Calendar.SECOND, 0);
        sourceDate.set(Calendar.MILLISECOND, 0);
        assertEquals("milliseconds of resultDate (serialized) should be equal, if properly set the: ",
                ((Date)serObject.mydate).getTime(),sourceDate.getTime().getTime() );
        
    }
    // timezone handling example
    @Test
    public void testDeSerializeTZDate() throws Exception { 
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm a Z");
        sc.setDateFormat(df);
        DateKeyMixin serObject =sc.deSer(  "{\"mydate\":[\"java.util.Date\",\"1999-04-10 10:10 PM -0500\"]}"
                , DateKeyMixin.class);
        assertTrue(serObject.mydate instanceof Date);
        
        // compare object
        Calendar compareDate = Calendar.getInstance(TimeZone.getTimeZone("America/Montreal"));// UTC -5
        compareDate.set(1999, 3, 10, 11, 10);
        
        String compareDateFormatted =  df.format(compareDate.getTime());
        logger.debug("compareDate format: " + compareDateFormatted );
        logger.debug("may not be equal: millisec(resultDate(string)):" + ((Date)serObject.mydate).getTime() + 
                " millisec(sourceDate):"+ compareDate.getTime().getTime() );
        
        assertEquals("format should be equal",  compareDateFormatted,
                        df.format(((Date)serObject.mydate).getTime())
                        );
        logger.debug("format in locale timezone (resultDate(string)):" + df.format(((Date)serObject.mydate).getTime()));
    }
    @Test
    public void testSerializeWithCustomFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        String bean = sc.serializeOnlyFilter(filteredBean, "name");
        assertEquals(
                "Ser filtered Bean failed ",
                "{\"name\":\"joe\"}",
                bean);
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle,
               "w", "name");
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
                "[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0','age':0},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1','age':1},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2','age':2},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3','age':3},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4','age':4},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5','age':5},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6','age':6},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7','age':7},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8','age':8},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9','age':9}]",
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
                "{\"name\":\"joe\"}",
                bean);
    }
    @Test
    public void testSerializeWithMixinAndFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        sc.addAdapter("M4RBeanMixin", Bean.class,
                BeanMixin.class);
        // profession was already set to ignore, does not change
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "profession");
        assertEquals(
                "Ser filtered Bean failed ",
                "{}",
                bean);
    }
    @Test
    public void testSerializeWithUnregisteredMixinAndFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
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
    @Test
    public void testMultipleSerializingWithMixinAndFilter() throws Exception {
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
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
                "[{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe3'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe4'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe5'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe6'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe7'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe8'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe9'}]",
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
        logger.debug("result:::"+ result);
        // Type List.class / TypeReference -> Exception: need JSON String that contains type id (for subtype of java.util.List)
        // Type: Bean.class -> Exception: Can not deserialize instance of org.apache.fulcrum.json.jackson.Bean out of START_ARRAY token
        
        // -> need to use constructCollectionType
        List<Bean> beanList2 = (List<Bean>)sc.deSerCollection(result, new ArrayList(),Bean.class);
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
        // property w->width, BeanMixin:  name ignore other properties
        sc.addAdapter("M4RMixin", Rectangle.class, Mixin.class).addAdapter(
                "M4BeanRMixin", Bean.class, BeanMixin.class);
        String serRect = sc.ser(components);
        assertEquals(
                "Serialization failed ",
                "[{'type':'org.apache.fulcrum.json.Rectangle','width':25},{'type':'org.apache.fulcrum.json.Rectangle','width':250},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe0'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe1'},{'type':'org.apache.fulcrum.json.jackson.Bean','name':'joe2'}]",
                serRect.replace('"', '\''));
    }
    @Test
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
