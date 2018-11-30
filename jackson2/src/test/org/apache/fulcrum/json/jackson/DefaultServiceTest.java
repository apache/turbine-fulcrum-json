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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.json.jackson.example.BeanChild;
import org.apache.fulcrum.json.jackson.example.Rectangle;
import org.apache.fulcrum.json.jackson.example.TestClass;
import org.apache.fulcrum.json.jackson.filters.CustomModuleWrapper;
import org.apache.fulcrum.json.jackson.mixins.BeanMixin;
import org.apache.fulcrum.json.jackson.mixins.TypedRectangle;
import org.apache.fulcrum.json.jackson.serializers.TestDeserializer;
import org.apache.fulcrum.json.jackson.serializers.TestDummyWrapperDeserializer;
import org.apache.fulcrum.json.jackson.serializers.TestJsonSerializer;
import org.apache.fulcrum.json.jackson.serializers.TestSerializer;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.apache.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

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
@RunWith(JUnitPlatform.class)
public class DefaultServiceTest extends BaseUnit5Test {
    
	private JsonService sc = null;
	private final String preDefinedOutput = "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";
	Logger logger;

	/**
	 * Test setup
	 * 
	 * @throws Exception generic exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		logger = new Log4JLogger(LogManager.getLogger(getClass().getName()) );
		                //new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG);
		sc = (JsonService) this.lookup(JsonService.ROLE);
	}

	/**
	 * Test serialization
	 * 
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerialize() throws Exception {
		String serJson = sc.ser(new TestClass("mytest"));
		assertEquals(preDefinedOutput, serJson, "Serialization failed ");
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	public void testCustomSerializeWithoutServiceMapper() throws Exception {
		ObjectMapper objectMapper = customMapper(true);
		String expected = "{\"type\":\"org.apache.fulcrum.json.jackson.example.TestClass\",\"container\":{\"type\":\"java.util.HashMap\",\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\"}";
		String serJson = customAllExceptFilter(objectMapper, new TestClass("mytest"), TestClass.class, "name");
		logger.debug("serJson:" + serJson);
		assertEquals(expected, serJson, "Serialization with custom mapper failed ");
	}

	/**
	 * @param withType
	 * @return an objectMapper
	 */
	private ObjectMapper customMapper(boolean withType) {
		// inheriting Jackson2MapperService mapper does not get the configs,
		// but has e.g. JsonFactory.Feature fields
		ObjectMapper objectMapper = new ObjectMapper(new MappingJsonFactory(((Jackson2MapperService) sc).getMapper()));
		// use other configuration
		if (withType)
			objectMapper.enableDefaultTypingAsProperty(DefaultTyping.NON_FINAL, "type");
		AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
		// AnnotationIntrospector is by default JacksonAnnotationIntrospector
		assertTrue(ai != null && ai instanceof JacksonAnnotationIntrospector, "Expected Default JacksonAnnotationIntrospector");
		// add to allow filtering properties for non annotated class
		AnnotationIntrospector siai = new SimpleNameIntrospector();
		AnnotationIntrospector pair = new AnnotationIntrospectorPair(siai, ai);
		objectMapper.setAnnotationIntrospector(pair);
		return objectMapper;
	}

	/**
	 * @param objectMapper our object mapper
	 * @param target       the target to serialize
	 * @param filterClass  the filter class
	 * @param props        properties
	 * @return JSON string
	 * @throws JsonProcessingException generic exception
	 */
	private String customAllExceptFilter(ObjectMapper objectMapper, Object target, Class<?> filterClass,
			String... props) throws JsonProcessingException {
		PropertyFilter pf = SimpleBeanPropertyFilter.SerializeExceptFilter.serializeAllExcept(props);
		SimpleFilterProvider filter = new SimpleFilterProvider();
		filter.setDefaultFilter(pf);
		// we know thats a pair, and the second is our simple
		Collection<AnnotationIntrospector> ais = ((AnnotationIntrospectorPair) objectMapper.getSerializationConfig()
				.getAnnotationIntrospector()).allIntrospectors();
		for (AnnotationIntrospector ai : ais) {
			if (ai instanceof SimpleNameIntrospector) {
				// activate filtering
				((SimpleNameIntrospector) ai).setFilteredClasses(filterClass);
			}
		}
		// alternatively we could have set it here, if ref is still available
		// ((SimpleNameIntrospector) siai).setFilteredClasses(filterClass);
		String serJson = objectMapper.writer(filter).writeValueAsString(target);
		// alternatively
		// String serJson2 =
		// objectMapper.setFilterProvider(filter).writeValueAsString(new
		// TestClass("mytest"));;
		// assertEquals(serJson, serJson2);
		return serJson;
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	public void testCustomSerializeListWithoutServiceMapper() throws Exception {
		String expected = "[{\"age\":0},{\"age\":1},{\"age\":2}]";
		List<Bean> beanList = new ArrayList<Bean>();
		for (int i = 0; i < 3; i++) {
			Bean bean = new Bean();
			bean.setAge(i);
			bean.setName("bean" + i);
			beanList.add(bean);
		}
		ObjectMapper objectMapper = customMapper(false);
		String serJson = customAllExceptFilter(objectMapper, beanList, Bean.class, "name", "profession");
		logger.debug("serJson:" + serJson);
		assertEquals(expected, serJson);
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerializeList() throws Exception {
		String expected = "[{\"age\":0},{\"age\":1},{\"age\":2}]";
		List<Bean> beanList = new ArrayList<Bean>();
		for (int i = 0; i < 3; i++) {
			Bean bean = new Bean();
			bean.setAge(i);
			bean.setName("bean" + i);
			beanList.add(bean);
		}
		String serJson = sc.serializeAllExceptFilter(beanList, Bean.class, "name", "profession");
		logger.debug("serJsonByService:" + serJson);
		assertEquals(expected, serJson, "Serialization with service mapper failed");
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	// the default test class: one String field, one Map
	public void testSerializeExcludeNothing() throws Exception {
		String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"));
		assertEquals(
				"{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}",
				serJson);

		// test round trip
		TestClass result2 = checkDeserialization(serJson, TestClass.class, TextClassMixin.class);
		assertTrue(result2.getContainer() == null); // mixin set to ignore
		assertTrue(result2.getConfigurationName().equals("Config.xml"));
	}

	/**
	 * jackson does not deep exclusion of class types (by default?)
	 * 
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerializeExcludeClass() throws Exception {
		String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), String.class);
		assertEquals("{\"container\":{\"cf\":\"Config.xml\"}}", serJson, "Serialization failed ");
		TestClass result2 = checkDeserialization(serJson, TestClass.class, TextClassMixin.class);
		assertTrue(result2.getContainer() == null);
	}

	@Test
	public void testSerializeExcludeClassAndField() throws Exception {
		String serJson = ((Jackson2MapperService) sc).serializeAllExceptFilter(new TestClass("mytest"),
				new Class[] { TestClass.class, String.class }, "container");
		assertEquals("{}", serJson);
		TestClass result2 = checkDeserialization(serJson, TestClass.class, TextClassMixin.class);
		assertTrue(result2.getContainer() == null);
	}

	@Test
	// adding expected result to be consistent
	public void testSerializeExcludeClassAndFields() throws Exception {
		String serJson = ((Jackson2MapperService) sc).serializeAllExceptFilter(new TestClass("mytest"),
				new Class[] { Map.class, String.class }, "configurationName", "name");
		assertEquals("{}", serJson);
		checkDeserialization(serJson, TestClass.class, TextClassMixin.class);
		String serJson2 = ((Jackson2MapperService) sc).serializeAllExceptFilter(new TestClass("mytest"), true,
				"configurationName", "name");
		assertEquals("{}", serJson2);
		checkDeserialization(serJson2, TestClass.class, TextClassMixin.class);
	}

	/**
	 * Overwriting mixin
	 * {@link com.fasterxml.jackson.databind.Module.SetupContext#setMixInAnnotations(Class, Class)}
	 * 
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerializeExcludeField() throws Exception {

		String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "configurationName");
		assertEquals("{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}", serJson, "Serialization failed ");
		sc.addAdapter("Mixin Adapter", TestClass.class, TextClassMixin.class);
		// overwriting mixin with null: container is included
		TestClass result2 = checkDeserialization(serJson, TestClass.class, null);
		assertTrue(result2.getContainer() != null && result2.getContainer() instanceof Map);
		assertTrue(result2.getName() != null);
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerializeDate() throws Exception {
		// non default date format
		final SimpleDateFormat MMddyyyy = new SimpleDateFormat("MM-dd-yyyy");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("date", Calendar.getInstance().getTime());

		sc.setDateFormat(MMddyyyy);
		String serJson = sc.ser(map);
		logger.debug("serJson:" + serJson);
		assertTrue(serJson.matches("\\{\"date\":\"\\d\\d-\\d\\d-\\d{4}\"\\}"),
		           "Serialize with Adapater failed ");
	}

	/**
	 * @throws Exception generic exception
	 */
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
				"[{'w':0,'h':0,'name':'rect0','size':0},{'w':1,'h':1,'name':'rect1','size':1},{'w':2,'h':2,'name':'rect2','size':4},{'w':3,'h':3,'name':'rect3','size':9},{'w':4,'h':4,'name':'rect4','size':16},{'w':5,'h':5,'name':'rect5','size':25},{'w':6,'h':6,'name':'rect6','size':36},{'w':7,'h':7,'name':'rect7','size':49},{'w':8,'h':8,'name':'rect8','size':64},{'w':9,'h':9,'name':'rect9','size':81}]",
				adapterSer.replace('"', '\''),
				"collect ser failed");
	}

	/**
	 * @throws Exception generic exception
	 */
	@Test
	public void testSerializationCollectioPrimitiveWrapper() throws Exception {
		List<Integer> intList = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			Integer integer = new Integer(i * i);
			intList.add(integer);
		}
		String result = sc.serializeOnlyFilter(intList, Integer.class);
		assertEquals("[0,1,4,9,16,25,36,49,64,81]", result, "Serialization of beans failed ");
		// primitives could be deserialzed without type
		Collection<Integer> result2 = checkDeserCollection(result, List.class, Integer.class);
		assertTrue( !result2.isEmpty(), "expect at least one entry ");
		assertTrue( result2.iterator().next().getClass().isAssignableFrom(Integer.class), "result entry instance check");
	}

    /**
     * @throws Exception generic exception
     */
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
                "{'rect0':0,'rect1':1,'rect2':4,'rect3':9,'rect4':16,'rect5':25,'rect6':36,'rect7':49,'rect8':64,'rect9':81}",
                adapterSer.replace('"', '\''));
        // can only deserialize with type deserializer, adapter already added above
        List<Rectangle> result = sc.deSer(adapterSer, ArrayList.class);
        assertTrue( result.size() == 10, " expected result: 10");
        int nr = 3; // new Random().nextInt(10);
        assertTrue(result.get(nr).getName().equals("rect" + nr),
                   "result (" + nr + ") !=:" + result.get(nr).getName());
    }

    @Test
    public void testMixinAdapter() throws Exception {
        TestJsonSerializer tser = new TestJsonSerializer();
        CustomModuleWrapper<TestClass> cmw = new CustomModuleWrapper<TestClass>(tser,
                new TestDummyWrapperDeserializer(TestClass.class));
        sc.addAdapter("Collection Adapter", TestClass.class, cmw);
        String adapterSer = sc.ser(new TestClass("mytest"));
        assertEquals("{\"n\":\"mytest\",\"p\":\"Config.xml\",\"c\":[]}", adapterSer,
                     "failed adapter serialization:");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testDeSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        Object deson = sc.deSer(serJson, TestClass.class);
        assertEquals(TestClass.class, deson.getClass());
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testDeserializationCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        TypeReference<List<Rectangle>> typeRef = new TypeReference<List<Rectangle>>() {
        };
        Collection<Rectangle> resultList0 = sc.deSerCollection(serColl, typeRef, Rectangle.class);
        // logger.debug("resultList0 class:" +resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals((i * i), ((List<Rectangle>) resultList0).get(i).getSize(),
                         "deser reread size failed");
        }
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testDeserializationWithPlainListCollection() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        Collection<Rectangle> resultList0 = sc.deSerCollection(serColl, new ArrayList(), Rectangle.class);
        logger.debug("resultList0 class:" + resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals( (i * i), ((List<Rectangle>) resultList0).get(i).getSize(),
                          "deser reread size failed");
        }
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testDeserializationWithPlainList() throws Exception {
        List<Rectangle> rectList = new ArrayList<Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        String serColl = sc.ser(rectList);
        // Collection<Rectangle> resultList0 = sc.deSerCollection(serColl, List.class,
        // Rectangle.class);
        List<Rectangle> resultList0 = ((Jackson2MapperService) sc).deSerList(serColl, ArrayList.class, Rectangle.class);
        logger.debug("resultList0 class:" + resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals( (i * i), resultList0.get(i).getSize(),
                 "deser reread size failed");
        }
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testDeserializationWithPlainMap() throws Exception {
        Map<String, Rectangle> rectList = new HashMap<String, Rectangle>();
        for (int i = 0; i < 10; i++) {
            Rectangle filteredRect = new Rectangle(i, i, "rect" + i);
            rectList.put("" + i, filteredRect);
        }
        String serColl = sc.ser(rectList);
        Map<String, Rectangle> resultList0 = ((Jackson2MapperService) sc).deSerMap(serColl, Map.class, String.class,
                Rectangle.class);
        logger.debug("resultList0 class:" + resultList0.getClass());
        for (int i = 0; i < 10; i++) {
            assertEquals( (i * i), resultList0.get("" + i).getSize(),
                          "deser reread size failed");
        }
    }

    /**
     * @throws Exception generic exception
     */
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
        ArrayList<Rectangle> resultList0 = sc.deSer(adapterSer, ArrayList.class);
        for (int i = 0; i < 10; i++) {
            assertEquals( (i * i), resultList0.get(i).getSize(),
                          "deser reread size failed");
        }
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeWithMixinAndFilter() throws Exception {
        Bean filteredBean = new Bean();
        filteredBean.setName("joe");
        //
        sc.addAdapter("M4RBeanMixin", Bean.class, BeanMixin.class);
        // profession was already set to ignore, does not change
        String bean = sc.serializeOnlyFilter(filteredBean, Bean.class, "profession");
        assertEquals("{}", bean);
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeWithOnlyFilter() throws Exception {

        String serJson = sc.serializeOnlyFilter(new TestClass("mytest"), "configurationName");
        assertEquals("{\"configurationName\":\"Config.xml\"}", serJson, "Serialization failed ");

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, "w");
        assertEquals( "{\"w\":5}", rectangle, "Ser filtered Rectangle failed ");
        rectangle = sc.serializeOnlyFilter(filteredRectangle, true, "w");
        assertEquals( "{\"w\":5}", rectangle, "Ser filtered Rectangle failed ");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeAllExceptANDWithOnlyFilter2() throws Exception {

        String serJson = sc.serializeAllExceptFilter(new TestClass("mytest"), "configurationName");
        assertEquals( "{\"container\":{\"cf\":\"Config.xml\"},\"name\":\"mytest\"}", serJson);

        serJson = sc.serializeOnlyFilter(new TestClass("mytest"), "configurationName");
        assertEquals( "{\"configurationName\":\"Config.xml\"}", serJson);

        Rectangle filteredRectangle = new Rectangle(5, 10);
        filteredRectangle.setName("jim");
        String rectangle = sc.serializeOnlyFilter(filteredRectangle, "w");
        assertEquals( "{\"w\":5}", rectangle, "Ser filtered Rectangle failed ");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeBeanWithOnlyFilter() throws Exception {
        Bean bean = new BeanChild();
        bean.setAge(1);
        bean.setName("bean1");
        assertEquals("{\"name\":\"bean1\"}", sc.serializeOnlyFilter(bean, true, "name"));
        assertEquals("{\"name\":\"bean1\"}", sc.serializeOnlyFilter(bean, Bean.class, true, "name")); // parent filter
        assertEquals("{\"name\":\"bean1\"}", sc.serializeOnlyFilter(bean, BeanChild.class, true, "name"));
        assertEquals("{\"name\":\"bean1\"}", sc.serializeOnlyFilter(bean, Object.class, true, "name"));
        bean = new Bean();
        bean.setAge(0);
        bean.setName("bean0");
        assertEquals("{\"name\":\"bean0\"}", sc.serializeOnlyFilter(bean, true, "name"));
        assertEquals("{\"name\":\"bean0\"}", sc.serializeOnlyFilter(bean, Bean.class, true, "name"));
        assertEquals("{\"name\":\"bean0\"}", sc.serializeOnlyFilter(bean, BeanChild.class, true, "name"));// child
                                                                                                            // filter
        assertEquals("{\"name\":\"bean0\"}", sc.serializeOnlyFilter(bean, Object.class, true, "name"));
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithOnlyFilterAndParentClass() throws Exception {
        List<BeanChild> beanList = new ArrayList<BeanChild>();
        for (int i = 0; i < 3; i++) {
            BeanChild bean = new BeanChild();
            bean.setAge(i);
            bean.setName("bean" + i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, Bean.class, true, "name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]", jsonResult);
        // assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList,
        // BeanChild.class, true,"type"));

        Collection<BeanChild> result2 = checkDeserCollection(jsonResult, List.class, BeanChild.class);
        assertTrue( !result2.isEmpty());
        assertTrue(
                result2.iterator().next().getClass().isAssignableFrom(BeanChild.class),
                "result entry instance check");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithOnlyFilterAndExactClass() throws Exception {
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new BeanChild();
            bean.setAge(i);
            bean.setName("bean" + i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, BeanChild.class, true, "name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]", jsonResult);
        // assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList,
        // BeanChild.class, true,"type"));
        Collection<Bean> result2 = checkDeserCollection(jsonResult, List.class, Bean.class);
        assertTrue( !result2.isEmpty(), "expect at least one entry ");
        assertTrue( result2.iterator().next().getClass().isAssignableFrom(Bean.class), "result entry instance check");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithOnlyFilterWithChildClass() throws Exception {
        List<Bean> beanList = new ArrayList<Bean>();
        for (int i = 0; i < 3; i++) {
            Bean bean = new Bean();
            bean.setAge(i);
            bean.setName("bean" + i);
            beanList.add(bean);
        }
        String jsonResult = sc.serializeOnlyFilter(beanList, BeanChild.class, true, "name");
        assertEquals("[{\"name\":\"bean0\"},{\"name\":\"bean1\"},{\"name\":\"bean2\"}]", jsonResult);
        // assertEquals("[{\"type\":\"\"},{\"type\":\"\"},{\"type\":\"\"}]",sc.serializeOnlyFilter(beanList,
        // BeanChild.class, true,"type"));
        Collection<Bean> result2 = checkDeserCollection(jsonResult, List.class, Bean.class);
        assertTrue( !result2.isEmpty(), "expect at least one entry ");
        assertTrue( result2.iterator().next().getClass().isAssignableFrom(Bean.class), "result entry instance check");
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithOnlyFilterAndType() throws Exception {

        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        Class<?> clazz = Class.forName("org.apache.fulcrum.json.jackson.mixins.TypedRectangle");
        // no type cft. https://github.com/FasterXML/jackson-databind/issues/303 !!
        String jsonResult = sc.serializeOnlyFilter(rectList, clazz, true, "w");
        assertEquals("[{\"w\":0},{\"w\":1}]", jsonResult);
        // could not deserialize easily with missing property type
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithOnlyFilterAndMixin() throws Exception {

        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        Class<?> clazz = Class.forName("org.apache.fulcrum.json.jackson.mixins.TypedRectangle");
        sc.addAdapter("Collection Adapter", Object.class, TypedRectangle.Mixins.class);
        assertEquals(
                "[\"java.util.ArrayList\",[{\"type\":\"org.apache.fulcrum.json.jackson.mixins.TypedRectangle\",\"w\":0},{\"type\":\"org.apache.fulcrum.json.jackson.mixins.TypedRectangle\",\"w\":1}]]",
                sc.serializeOnlyFilter(rectList, clazz, true, "w"));
    }

    /**
     * @throws Exception generic exception
     */
    @Test
    public void testSerializeCollectionWithTypedReference() throws Exception {

        List<TypedRectangle> rectList = new ArrayList<TypedRectangle>();
        for (int i = 0; i < 2; i++) {
            TypedRectangle filteredRect = new TypedRectangle(i, i, "rect" + i);
            rectList.add(filteredRect);
        }
        TypeReference<List<TypedRectangle>> typeRef = new TypeReference<List<TypedRectangle>>() {
        };
        String jsonResult = ((Jackson2MapperService) sc).serCollectionWithTypeReference(rectList, typeRef, false);
        logger.debug("aa:" + jsonResult);
        // could deserialize with type information
        Collection<TypedRectangle> result2 = checkDeserCollection(jsonResult, List.class, TypedRectangle.class);
        assertTrue( !result2.isEmpty(), "expect at least one entry ");
        assertTrue(
                result2.iterator().next().getClass().isAssignableFrom(TypedRectangle.class),
                "result entry instance check");

    }

    @Test
    // jackson does not escape anything, except double quotes and backslash,
    // additional characters could be provided
    // by activationg escapeCharsGlobal xml characters are added
    public void testSerializeHTMLEscape() throws Exception {
        Rectangle filteredRect = new Rectangle(2, 3,
                "rectÜber<strong>StockundStein &iuml;</strong></script><script>alert('xss')</script>" + 0);
        String adapterSer = sc.ser(filteredRect);
        logger.debug("Escaped serialized string:" + adapterSer);
        assertEquals(
                "{'w':2,'h':3,'name':'rectÜber\\u003Cstrong\\u003EStockundStein \\u0026iuml;\\u003C/strong\\u003E\\u003C/script\\u003E\\u003Cscript\\u003Ealert(\\u0027xss\\u0027)\\u003C/script\\u003E0','size':6}",
                adapterSer.replace('"', '\''),
                "escaped html entities ser expected, iei <,>,&,\\ escaped (requires escapeCharsGlobal in json component configuration");
        // you could set your own escapes here in class esc extending from
        // CharacterEscapes.
        // ((Jackson2MapperService)sc).getMapper().getFactory().setCharacterEscapes(esc
        // ) );
    }

    /**
     * checks if string serJson is deserializable to class target with adapter mixin
     * and returns result.
     * 
     * @param serJson JSON String to be tested
     * @param target  class to be expected
     * @param mixin   adapter set
     * @return the resulting instance
     * @throws Exception
     */
    private <T> T checkDeserialization(String serJson, Class<T> target, Class mixin) throws Exception {
        sc.addAdapter("Mixin Adapter", target, mixin);
        T result = sc.deSer(serJson, target);
        assertTrue( target.isAssignableFrom(result.getClass()), "Result Instance Check");
        return result;
    }

    private <U> Collection<U> checkDeserCollection(String serJson, Class<? extends Collection> collClass,
            Class<U> entryClass) throws Exception {
        Collection<U> result = ((Jackson2MapperService) sc).deSerCollectionWithType(serJson, collClass, entryClass);
        // System.out.println("result:"+ result + " is of type: "+ result.getClass() +
        // "and assignable from "+ collClass);
        assertTrue(collClass.isAssignableFrom(result.getClass()),
                "Result Instance Check failed for result class " + result.getClass() + " and target class: " + collClass);
        return result;
    }
}

abstract class TextClassMixin {

    @JsonIgnore
    abstract Map<String, Object> getContainer();
}
