package org.apache.fulcrum.json.gson;

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

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.reflect.TypeToken;

/**
 * GSON JSON Test
 * 
 * @author gk
 * @version $Id$
 */
public class DefaultServiceTest extends BaseUnit4Test {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
    @Before
    public void setUp() throws Exception
    {
        sc = (JsonService) this.lookup( JsonService.ROLE );
    }

    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals("Serialization failed ", preDefinedOutput, serJson);
    }
    @Test
    public void testSerializeExcludeNothing() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                (String[]) null);
        assertEquals(
                "Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}",
                serJson);
    }
    @Test
    // deep exclude?!
    public void testSerializeExcludeClass() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                String.class, (String[]) null);
        assertEquals("Serialization failed ", "{\"container\":{}}", serJson);
    }
    @Test
    public void testSerializeExcludeClassAndField() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                String.class, "container");
        assertEquals("Serialization failed ", "{}", serJson);
    }
    @Test
    public void testSerializeExcludeClassAndFields() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                Map.class, "configurationName", "name");
        assertEquals("Serialization failed ", "{}", serJson);
    }
    @Test
    public void testSerializeExcludeField() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                 "configurationName");
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}",
                serJson);
    }
    @Test
    public void testSerializeDefaultDate() throws Exception {
        //MM:dd:yyyy
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("date", Calendar.getInstance().getTime());
        String serJson = sc.ser(map);
        System.out.println("serJson:" + serJson);
        assertTrue("Serialize with Adapater failed ",
                serJson.matches("\\{\"date\":\"\\d\\d:\\d\\d:\\d{4}\"\\}"));
    }
    @Test
    public void testSerializeDate() throws Exception {
        final SimpleDateFormat MMddyyyy = new SimpleDateFormat("MM/dd/yyyy");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("date", Calendar.getInstance().getTime());
        sc.setDateFormat(MMddyyyy);
        String serJson = sc.ser(map);
        System.out.println("serJson:" + serJson);
        assertTrue("Serialize with Adapater failed ",
                serJson.matches("\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}"));
    }
    @Test
    // does not seerialize size
    public void testSerializeCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        assertEquals(
                "collect ser",
                "[{'w':0,'h':0,'name':'rect0'},{'w':1,'h':1,'name':'rect1'},{'w':2,'h':2,'name':'rect2'},{'w':3,'h':3,'name':'rect3'},{'w':4,'h':4,'name':'rect4'},{'w':5,'h':5,'name':'rect5'},{'w':6,'h':6,'name':'rect6'},{'w':7,'h':7,'name':'rect7'},{'w':8,'h':8,'name':'rect8'},{'w':9,'h':9,'name':'rect9'}]",
                adapterSer.replace('"', '\''));
    }
    @Test
    public void testSerializationCollectioPrimitiveWrapper() throws Exception {

        List<Integer> intList = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            Integer integer = new Integer(i*i);
            intList.add(integer);
        }
        String result = sc.ser(intList);
        assertEquals(
                "Serialization of beans failed ",
                "[0,1,4,9,16,25,36,49,64,81]",
                result);
    }
    @Test
    public void testSerializeTypeAdapterForCollection() throws Exception {
        sc.addAdapter("Collection Adapter", ArrayList.class,
                new TypeAdapterForCollection());
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        assertEquals(
                "collect ser",
                "[{'rect0':0,'rect1':1,'rect2':4,'rect3':9,'rect4':16,'rect5':25,'rect6':36,'rect7':49,'rect8':64,'rect9':81}]",
                adapterSer.replace('"', '\''));
    }
    @Test
    public void testMixinAdapter() throws Exception {
        sc.addAdapter("Test Adapter", TestClass.class, new TestJsonSerializer());
        String adapterSer = sc.ser(new TestClass("mytest"));
        assertEquals("failed adapter serialization:",
                "{\"n\":\"mytest\",\"p\":\"Config.xml\",\"c\":[]}", adapterSer);
        //System.out.println("adapterSer:"+adapterSer);
        ((Initializable)sc).initialize(); //reinit
        String defaultSer = sc.ser(new TestClass("mytest"));
        assertEquals("failed adapter serialization:",
                "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}", defaultSer);
    }
    @Test 
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals("Serialization failed ", TestClass.class, deson.getClass());
    }
    @Test
    public void testDeserializationCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        Type collectionType = new TypeToken<Collection<Rectangle>>() {}.getType();
        List<Rectangle> resultList0 = (List<Rectangle>) ((org.apache.fulcrum.json.gson.GSONBuilderService)sc).deSerCollection(serColl, collectionType,Rectangle.class);
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), resultList0
                    .get(i).getSize());
        }
    }
    @Test
    public void testDeserializationTypeAdapterForCollection() throws Exception {
        sc.addAdapter("Collection Adapter", ArrayList.class,
                TypeAdapterForCollection.class);
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        ArrayList<Rectangle> resultList0 = sc.deSer(adapterSer, ArrayList.class);
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), resultList0
                    .get(i).getSize());
        }
    }
    @Test
    public void testSerializeWithMixinAndFilter() throws Exception {
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        //
        sc.addAdapter("M4RMixin2", Rectangle.class,
                TypeAdapterForRectangle.class);
        
        // as gson adds adapters/strategies, we have to be cautious if using multiple strategies
        String rectangle = sc.ser(filteredRectangle);
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"name\":\"jim\",\"width\":5}",
                rectangle); 
    }
    @Test
    public void testSerializeWithOnlyFilter() throws Exception {

        // as gson adds we could not use multiple disjunct exclusion strategies
        String serJson = sc.serializeOnlyFilter(new TestClass("mytest"),
                (Class) null, "configurationName");
        assertEquals("Serialization failed ",
                "{\"configurationName\":\"Config.xml\"}",
                serJson);
    }
    @Test
    public void testSerializeWithOnlyFilter2() throws Exception {

        // as gson adds we could not use multiple disjunct exclusion strategies
        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, "w");
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"w\":5}",
                rectangle);
    }



}
