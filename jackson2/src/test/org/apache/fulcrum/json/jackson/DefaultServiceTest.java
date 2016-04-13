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
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Jackson 2 JSON Test
 * 
 * @author gk
 * @version $Id$
 */
public class DefaultServiceTest extends BaseUnit4Test {
    private JsonService sc = null;
    private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";

    @Before
    public void setUp() throws Exception {
        setLogLevel(ConsoleLogger.LEVEL_DEBUG);
        sc = (JsonService) this.lookup(JsonService.ROLE);
    }

    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals("Serialization failed ", preDefinedOutput, serJson);
    }
    
    @Test
    public void testCustomSerializeWithoutServiceMapper() throws Exception {
        ObjectMapper objectMapper = customMapper(true);
        String expected = "{\"type\":\"org.apache.fulcrum.json.TestClass\",\"container\":{\"type\":\"java.util.HashMap\",\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\"}";
        String serJson = customAllExceptFilter(objectMapper, new TestClass("mytest"), TestClass.class,"name");
        System.out.println("serJson:"+ serJson);
        assertEquals("Serialization with custom mapper failed ",expected, serJson);
    }

    private ObjectMapper customMapper(boolean withType) {
        // inheriting Jackson2MapperService mapper does not get the configs,
        // but has e.g. JsonFactory.Feature fields
        ObjectMapper objectMapper = new ObjectMapper(
                new MappingJsonFactory(((Jackson2MapperService) sc).getMapper()));
        // use other configuration
        if (withType) objectMapper.enableDefaultTypingAsProperty(
                DefaultTyping.NON_FINAL, "type");
        AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
        // AnnotationIntrospector is by default JacksonAnnotationIntrospector 
        assertTrue("Expected Default JacksonAnnotationIntrospector", "ai:"+ ai != null && ai instanceof JacksonAnnotationIntrospector);
        // add to allow filtering properties for non annotated class
        AnnotationIntrospector siai = new SimpleNameIntrospector();
        AnnotationIntrospector pair = new AnnotationIntrospectorPair(siai,ai);
        objectMapper.setAnnotationIntrospector(pair);
        return objectMapper;
    }

    private String customAllExceptFilter(ObjectMapper objectMapper, Object target, Class<?> filterClass, String... props) throws JsonProcessingException {
        PropertyFilter pf = SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept(props);
        SimpleFilterProvider filter = new SimpleFilterProvider();
        filter.setDefaultFilter(pf);
        // we know thats a pair, and the second is our simple
        Collection<AnnotationIntrospector> ais = ((AnnotationIntrospectorPair)objectMapper.getSerializationConfig().getAnnotationIntrospector()).allIntrospectors();
        for (AnnotationIntrospector ai : ais) {
            if (ai instanceof SimpleNameIntrospector) {
                //activate filtering
                ((SimpleNameIntrospector) ai).setFilteredClasses(filterClass);
            }
        } 
        // alternatively we could have set it here, if ref is still available 
        // ((SimpleNameIntrospector) siai).setFilteredClasses(filterClass);        
        String serJson = objectMapper.writer(filter).writeValueAsString(target);
        // alternatively
        //String serJson2 = objectMapper.setFilterProvider(filter).writeValueAsString(new TestClass("mytest"));;
        //assertEquals(serJson, serJson2);
        return serJson;
    }
    
    @Test
    public void testCustomSerializeListWithoutServiceMapper() throws Exception {
        String expected = "[{\"age\":0},{\"age\":1},{\"age\":2}]";
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new Bean();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        ObjectMapper objectMapper = customMapper(false);
        String serJson = customAllExceptFilter(objectMapper, beanList, Bean.class,"name","profession");
        System.out.println("serJson:"+ serJson);
        assertEquals("Serialization with custom mapper failed ",expected, serJson);
    }
    
    @Test
    public void testSerializeList() throws Exception {
        String expected = "[{\"age\":0},{\"age\":1},{\"age\":2}]";
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new Bean();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }    
        String serJson = sc.serializeAllExceptFilter(beanList, Bean.class, "name","profession");
        System.out.println("serJsonByService:"+ serJson);
        assertEquals("Serialization with service mapper failed",expected, serJson);
    }

    @Test
    // the default test class: one String field, one Map  
    public void testSerializeExcludeNothing() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"));
        assertEquals(
                "Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}",
                serJson);

      // test round trip
      TestClass result2 = checkDeserialization(serJson,TestClass.class, TextClassMixin.class);     
      assertTrue(result2.getContainer() == null); // mixin set to ignore
      assertTrue(result2.getConfigurationName().equals("Config.xml")); 
    }
    
    @Test
    // jackson does not deep exclusion of class types (by default?)
    public void testSerializeExcludeClass() throws Exception {
        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"),
                String.class);
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"}}", serJson);
        TestClass result2 = checkDeserialization(serJson,TestClass.class, TextClassMixin.class);
        assertTrue(result2.getContainer() == null); 
    }
    @Test
    public void testSerializeExcludeClassAndField() throws Exception {
        String serJson = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
               new Class[] { TestClass.class, String.class} , "container");
        assertEquals("Serialization failed ", "{}", serJson);
        TestClass result2 =  checkDeserialization(serJson,TestClass.class, TextClassMixin.class);
        assertTrue(result2.getContainer() == null); 
    }
    @Test
    // adding  expected result to be consistent
    public void testSerializeExcludeClassAndFields() throws Exception {
        String serJson = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
               new Class[] { Map.class, String.class} , "configurationName", "name");
        assertEquals("Serialization failed ", "{}", serJson);
        checkDeserialization(serJson,TestClass.class, TextClassMixin.class); 
        String serJson2 = ((Jackson2MapperService)sc).serializeAllExceptFilter(new TestClass("mytest"),
                true, "configurationName", "name");
         assertEquals("Serialization failed ", "{}", serJson2);
         checkDeserialization(serJson2,TestClass.class, TextClassMixin.class); 
    }
    /**
     * Overwriting mixin 
     * @see com.fasterxml.jackson.databind.Module.SetupContext#setMixInAnnotations(Class, Class)
     * 
     * @throws Exception
     */
    @Test
    public void testSerializeExcludeField() throws Exception {

        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "configurationName");
        assertEquals("Serialization failed ",
                "{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}",
                serJson);
        sc.addAdapter("Mixin Adapter", TestClass.class, TextClassMixin.class);
        // overwriting mixin with null: container is included
        TestClass result2 =checkDeserialization(serJson,TestClass.class, null);
        assertTrue(result2.getContainer() != null && result2.getContainer() instanceof Map);
        assertTrue(result2.getName() != null); 
    }
    @Test
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
    @Test
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
    @Test
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
        // primitives could be deserialzed without type
        Collection<Integer> result2 = checkDeserCollection(result, List.class, Integer.class);
        assertTrue("expect at least one entry ", !result2.isEmpty()); 
        assertTrue("result entry instance check", result2.iterator().next().getClass().isAssignableFrom(Integer.class));
    }
    @Test
    public void testSerializeTypeAdapterForCollection() throws Exception {
        TestSerializer tser = new TestSerializer();
        TestDeserializer tdeSer = new TestDeserializer();
        CustomModuleWrapper<List<Rectangle>> cmw = new CustomModuleWrapper<List<Rectangle>>(tser, tdeSer);
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
        // can only deserialize with type deserializer, adapter already added above
        List<Rectangle> result = sc.deSer(adapterSer,ArrayList.class);
        assertTrue("result:" +result.size(),result.size() == 10);
        int nr = 3; //new Random().nextInt(10);
        assertTrue("result ("+nr+"):" +result.get(nr).getName(),result.get(nr).getName().equals("rect"+nr) );
    }
    @Test
    public void testMixinAdapter() throws Exception {
        TestJsonSerializer tser = new TestJsonSerializer();
        CustomModuleWrapper<TestClass> cmw = new CustomModuleWrapper<TestClass>(
                tser, null);
        sc.addAdapter("Collection Adapter", TestClass.class, cmw);
        String adapterSer = sc.ser(new TestClass("mytest"));
        assertEquals("failed adapter serialization:",
                "{\"n\":\"mytest\",\"p\":\"Config.xml\",\"c\":[]}", adapterSer);
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
        TypeReference<List<Rectangle>> typeRef = new TypeReference<List<Rectangle>>(){};
        Collection<Rectangle> resultList0 =  sc.deSerCollection(serColl, typeRef, Rectangle.class);
        //System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), ((List<Rectangle>)resultList0)
                    .get(i).getSize());
        }
    }
    @Test
    public void testDeserializationWithPlainListCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        Collection<Rectangle> resultList0 =  sc.deSerCollection(serColl, new ArrayList(), Rectangle.class);
        System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i),  ((List<Rectangle>)resultList0)
                    .get(i).getSize());
        }
    }
    @Test
    public void testDeserializationWithPlainList() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        //Collection<Rectangle> resultList0 =  sc.deSerCollection(serColl, List.class, Rectangle.class);
        List<Rectangle> resultList0 =  ((Jackson2MapperService)sc).deSerList(serColl, ArrayList.class,List.class, Rectangle.class);
        System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), resultList0
                    .get(i).getSize());
        }
    }
    @Test
    public void testDeserializationWithPlainMap() throws Exception {
        Map<String,Rectangle> rectList = new HashMap<String,Rectangle>(); 
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.put(""+i,filteredRect);
        }
        String serColl = sc.ser(rectList);
        Map<String,Rectangle> resultList0 =  ((Jackson2MapperService)sc).deSerMap(serColl, Map.class, String.class,Rectangle.class);
        System.out.println("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals("deser reread size failed", (i * i), resultList0
                    .get(""+i).getSize());
        }
    }
    @Test
    public void testDeserializationTypeAdapterForCollection() throws Exception {
        TestSerializer tser = new TestSerializer();
        TestDeserializer tdeSer = new TestDeserializer();
        CustomModuleWrapper<List<Rectangle>> cmw = new CustomModuleWrapper<List<Rectangle>>(tser, tdeSer);
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
                "{}",
                bean);
    }
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testSerializeCollectionWithOnlyFilterAndParentClass() throws Exception {
        List<BeanChild> beanList = new ArrayList<BeanChild>();
        for (int i = 0; i < 3; i++) {
            BeanChild bean = new BeanChild();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, Bean.class, true,"name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",jsonResult);
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));

        Collection<BeanChild> result2 =checkDeserCollection(jsonResult, List.class, BeanChild.class);
        assertTrue("expect at least one entry ", !result2.isEmpty()); 
        assertTrue("result entry instance check", result2.iterator().next().getClass().isAssignableFrom(BeanChild.class));
    }
    @Test
    public void testSerializeCollectionWithOnlyFilterAndExactClass() throws Exception {    
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new BeanChild();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, BeanChild.class, true,"name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",jsonResult);
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));   
        Collection<Bean> result2 =checkDeserCollection(jsonResult, List.class, Bean.class);
        assertTrue("expect at least one entry ", !result2.isEmpty()); 
        assertTrue("result entry instance check", result2.iterator().next().getClass().isAssignableFrom(Bean.class));
    }
    @Test
    public void testSerializeCollectionWithOnlyFilterWithChildClass() throws Exception {
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new Bean();
            bean.setAge(i);bean.setName("bean"+i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, BeanChild.class, true,"name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]",jsonResult);
        //assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList, BeanChild.class, true,"type"));
        Collection<Bean> result2 =checkDeserCollection(jsonResult, List.class, Bean.class);
        assertTrue("expect at least one entry ", !result2.isEmpty()); 
        assertTrue("result entry instance check", result2.iterator().next().getClass().isAssignableFrom(Bean.class));    
    }
    @Test
    public void testSerializeCollectionWithOnlyFilterAndType() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        Class<?> clazz = Class.forName("org.apache.fulcrum.json.jackson.TypedRectangle");
        // no type cft. https://github.com/FasterXML/jackson-databind/issues/303 !!
        String jsonResult = sc.serializeOnlyFilter(rectList, clazz, true,"w");
        assertEquals("[{\"w\":0},{\"w\":1}]",jsonResult);
        // could not deserialize easily with missing property type        
    }
    @Test
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
    @Test
    public void testSerializeCollectionWithTypedReference() throws Exception {
        
        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        TypeReference<List<TypedRectangle>> typeRef = new TypeReference<List<TypedRectangle>>(){};
        String jsonResult = ((Jackson2MapperService)sc).serCollectionWithTypeReference(rectList,typeRef, false);
        System.out.println("aa:" +jsonResult);
        // could deserialize with type information 
        Collection<TypedRectangle> result2 =checkDeserCollection(jsonResult, List.class, TypedRectangle.class);
        assertTrue("expect at least one entry ", !result2.isEmpty()); 
        assertTrue("result entry instance check", result2.iterator().next().getClass().isAssignableFrom(TypedRectangle.class));
        
    }
    @Test
    // jackson does not escape anything, except double quotes and backslash, you could provide 
    public void testSerializeHTMLEscape() throws Exception {
        Rectangle filteredRect = new Rectangle(2, 3, "rectÜber<strong>StockundStein &iuml;</strong></script><script>alert('xss')</script>" + 0);
        String adapterSer = sc.ser(filteredRect);
        System.out.println(adapterSer);
        assertEquals("html entities ser",
                "{'w':2,'h':3,'name':'rectÜber\\u003Cstrong\\u003EStockundStein \\u0026iuml;\\u003C/strong\\u003E\\u003C/script\\u003E\\u003Cscript\\u003Ealert(\\u0027xss\\u0027)\\u003C/script\\u003E0','size':6}",
                adapterSer.replace('"', '\''));
        // you could set your own escapes here in class esc extending from CharacterEscapes. 
        //((Jackson2MapperService)sc).getMapper().getFactory().setCharacterEscapes(esc ) );
    }
    
    private <T> T checkDeserialization(String serJson, Class<T> target, Class mixin) throws Exception {
        sc.addAdapter("Mixin Adapter", target, mixin);
        T result = sc.deSer(serJson,target);
        assertTrue("Result Instance Check", target.isAssignableFrom(result.getClass()));
        return result;
    }
    private <U> Collection<U> checkDeserCollection(String serJson,Class<? extends Collection> collClass, Class<U> entryClass) throws Exception {
          Collection<U> result = ((Jackson2MapperService) sc).deSerCollectionWithType(serJson, collClass, entryClass);
          //System.out.println("result:"+ result + " is of type: "+ result.getClass() + "and assignable from "+ collClass);
          assertTrue("Result Instance Check failed for result class "+ result.getClass() + " and target class: "+ collClass, 
                  collClass.isAssignableFrom(result.getClass()));
          return result;
    }
}


abstract class TextClassMixin {

    @JsonIgnore abstract Map<String, Object> getContainer();
}
