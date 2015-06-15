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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.json.jackson.filters.CustomModuleWrapper;
import org.apache.fulcrum.testcontainer.BaseUnitTest;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Jackson 2 JSON Test
 * 
 * @author gk
 * @version $Id$
 */
public class DefaultServiceTest extends BaseUnitTest {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";

    /**
     * Constructor for test.
     * 
     * @param testName
     *            name of the test being executed
     */
    public DefaultServiceTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        //setLogLevel(ConsoleLogger.LEVEL_DEBUG);
        super.setUp();
        sc = (JsonService) this.lookup(JsonService.ROLE);
    }

    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals("Serialization failed ", preDefinedOutput, serJson);
    }

    //@Test
    // the default test class: one String field, one Map  
    public void testSerializeExcludeNothing() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"));
        assertEquals(
                "Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}",
                serJson);
    }

    // jackson does not deep exclusion of class types (by default?)
    public void testSerializeExcludeClass() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                String.class);
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"}}", serJson);
    }
    
    public void testSerializeExcludeClassAndField() throws Exception {
        String serJson = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
               new Class[] { TestClass.class, String.class} , "container");
        assertEquals("Serialization failed ", "{}", serJson);
    }

    // adding  expected result to be consistent
    public void testSerializeExcludeClassAndFields() throws Exception {
        String serJson = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
               new Class[] { Map.class, String.class} , "configurationName", "name");
        assertEquals("Serialization failed ", "{}", serJson);
        String serJson2 = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
                true, "configurationName", "name");
         assertEquals("Serialization failed ", "{}", serJson2);
    }

    public void testSerializeExcludeField() throws Exception {

        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "configurationName");
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}",
                serJson);
    }

    public void testSerializeDate() throws Exception {
        // non default date format
        final SimpleDateFormat MMddyyyy = new SimpleDateFormat("MM-dd-yyyy");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("date", Calendar.getInstance().getTime());

        sc.setDateFormat(MMddyyyy);
        String serJson = sc.ser(map);
        System.out.println("serJson:" + serJson);
        assertTrue("Serialize with Adapater failed ",
                serJson.matches("\\{\"date\":\"\\d\\d-\\d\\d-\\d{4}\"\\}"));
    }

    // jackson serializes size too
    public void testSerializeCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        assertEquals(
                "collect ser",
                "[{'w':0,'h':0,'name':'rect0','size':0},{'w':1,'h':1,'name':'rect1','size':1},{'w':2,'h':2,'name':'rect2','size':4},{'w':3,'h':3,'name':'rect3','size':9},{'w':4,'h':4,'name':'rect4','size':16},{'w':5,'h':5,'name':'rect5','size':25},{'w':6,'h':6,'name':'rect6','size':36},{'w':7,'h':7,'name':'rect7','size':49},{'w':8,'h':8,'name':'rect8','size':64},{'w':9,'h':9,'name':'rect9','size':81}]",
                adapterSer.replace('"', '\''));
    }
    
    public void testSerializationCollectioPrimitiveWrapper() throws Exception {

        List<Integer> intList = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            Integer integer = new Integer(i*i);
            intList.add(integer);
        }
        String result = sc.serializeOnlyFilter(intList, Integer.class);
        assertEquals(
                "Serialization of beans failed ",
                "[0,1,4,9,16,25,36,49,64,81]",
                result);
    }

    public void testSerializeTypeAdapterForCollection() throws Exception {
        TestSerializer tser = new TestSerializer();
        CustomModuleWrapper<List> cmw = new CustomModuleWrapper(tser, null);
        sc.addAdapter("Collection Adapter", ArrayList.class, cmw);
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        assertEquals(
                "collect ser",
                "{'rect0':0,'rect1':1,'rect2':4,'rect3':9,'rect4':16,'rect5':25,'rect6':36,'rect7':49,'rect8':64,'rect9':81}",
                adapterSer.replace('"', '\''));
    }
    
    public void testMixinAdapter() throws Exception {
        TestJsonSerializer tser = new TestJsonSerializer();
        CustomModuleWrapper<TestClass> cmw = new CustomModuleWrapper<TestClass>(
                tser, null);
        sc.addAdapter("Collection Adapter", TestClass.class, cmw);
        String adapterSer = sc.ser(new TestClass("mytest"));
        assertEquals("failed adapter serialization:",
                "{\"n\":\"mytest\",\"p\":\"Config.xml\",\"c\":[]}", adapterSer);
    }
    
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals("Serialization failed ", TestClass.class, deson.getClass());
    }

    
    public void testDeserializationCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        TypeReference typeRef = new TypeReference<List<Rectangle>>(){};
        Collection<Rectangle> resultList0 =  sc.deSerCollection(serColl, typeRef, Rectangle.class);
        //System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), ((List<Rectangle>)resultList0)
                    .get(i).getSize());
        }
    }

    public void testDeserializationTypeAdapterForCollection() throws Exception {
        TestSerializer tser = new TestSerializer();
        TestDeserializer tdeSer = new TestDeserializer();
        CustomModuleWrapper<List> cmw = new CustomModuleWrapper(tser, tdeSer);
        sc.addAdapter("Collection Adapter", ArrayList.class, cmw);
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String adapterSer = sc.ser(rectList);
        ArrayList<Rectangle> resultList0 = sc
                .deSer(adapterSer, ArrayList.class);
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), resultList0
                    .get(i).getSize());
        }
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
    
    public void testSerializeWithOnlyFilter() throws Exception {

        String serJson = sc.serializeOnlyFilter(new TestClass("mytest"),"configurationName");
        assertEquals("Serialization failed ",
                "{\"configurationName\":\"Config.xml\"}",
                serJson); 

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, "w");
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"w\":5}",
                rectangle);
        rectangle = sc.serializeOnlyFilter(filteredRectangle, true, "w");
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"w\":5}",
                rectangle);
    }
    
    public void testSerializeAllExceptANDWithOnlyFilter2() throws Exception {
        
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),"configurationName");
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}",
                serJson);

        serJson = sc.serializeOnlyFilter(new TestClass("mytest"), "configurationName");
        assertEquals("Serialization failed ",
                "{\"configurationName\":\"Config.xml\"}",
                serJson); 

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, "w");
        assertEquals(
                "Ser filtered Rectangle failed ",
                "{\"w\":5}",
                rectangle);
    }
    
    public void testSerializeBeanWithOnlyFilter() throws Exception {
        Bean bean = new BeanChild();
        bean.setAge(1);bean.setName("bean1");
        assertEquals("{\"name\":\"bean1\"}",sc.serializeOnlyFilter(bean, true,"name"));
        assertEquals("{\"name\":\"bean1\"}",sc.serializeOnlyFilter(bean, Bean.class, true,"name")); // parent filter
        assertEquals("{\"name\":\"bean1\"}",sc.serializeOnlyFilter(bean, BeanChild.class, true,"name"));
        assertEquals("{\"name\":\"bean1\"}",sc.serializeOnlyFilter(bean, Object.class, true,"name"));
        bean = new Bean();
        bean.setAge(0);bean.setName("bean0");
        assertEquals("{\"name\":\"bean0\"}",sc.serializeOnlyFilter(bean, true,"name"));
        assertEquals("{\"name\":\"bean0\"}",sc.serializeOnlyFilter(bean, Bean.class, true,"name"));
        assertEquals("{\"name\":\"bean0\"}",sc.serializeOnlyFilter(bean, BeanChild.class, true,"name"));// child filter
        assertEquals("{\"name\":\"bean0\"}",sc.serializeOnlyFilter(bean, Object.class, true,"name"));   
    }
    
    public void testSerializeCollectionWithOnlyFilterAndParentClass() throws Exception {
        
        List<BeanChild> beanList = new ArrayList<BeanChild>();
        for (int i = 0; i < 3; i++) {
            BeanChild bean = new BeanChild();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",sc.serializeOnlyFilter(beanList, Bean.class, true,"name"));
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));
    }
    
    public void testSerializeCollectionWithOnlyFilterAndExactClass() throws Exception {
        
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new BeanChild();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"name"));
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));
    }
    
    public void testSerializeCollectionWithOnlyFilterWithChildClass() throws Exception {
        
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new Bean();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"name"));
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));
    }
    
    
    public void testSerializeCollectionWithOnlyFilterAndType() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        Class<?> clazz = Class.forName("org.apache.fulcrum.json.jackson.TypedRectangle");
        // no type cft. https://github.com/FasterXML/jackson-databind/issues/303 !!
        assertEquals("[{\"w\":0},{\"w\":1}]",sc.serializeOnlyFilter(rectList, clazz, true,"w"));
    }
    
    public void testSerializeCollectionWithOnlyFilterAndMixin() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        Class<?> clazz = Class.forName("org.apache.fulcrum.json.jackson.TypedRectangle");
        sc.addAdapter("Collection Adapter", Object.class, TypedRectangle.Mixins.class);
        assertEquals("[\"java.util.ArrayList\",[{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":0},{\"type\":\"org.apache.fulcrum.json.jackson.TypedRectangle\",\"w\":1}]]",
                sc.serializeOnlyFilter(rectList, clazz, true, "w"));
    }
    
    public void testSerializeCollectionWithTypedReference() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        TypeReference<List<TypedRectangle>> typeRef = new TypeReference<List<TypedRectangle>>(){};
        System.out.println("aa:" +((Jackson2MapperService)sc).serCollectionWithTypeReference(rectList,typeRef, false));
    }

}
