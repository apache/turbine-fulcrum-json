package org.apache.fulcrum.json.jackson.concurrent;

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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.Jackson2MapperService;
import org.apache.fulcrum.json.jackson.SimpleNameIntrospector;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@RunWith(Parameterized.class)
public class JSONConcurrentTest extends BaseUnit4Test {

    private static volatile Thread fOne = null;
    private static volatile Thread fTwo = null;
    private static volatile Thread fThree = null;
    private static JsonService jsonService = null;

    @Parameters(name = "test run {index}")
    public static Object[][] multiple() {
        return new Object[10][]; // { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }; // new Object[100][0];
    }

    public JSONConcurrentTest(Object val1) {
    }

    @BeforeClass
    public static void setUp() throws Exception {

    }

    @Before
    public void init() throws ComponentException {
        fOne = null;
        fTwo = null;
        fThree = null;
        jsonService = (JsonService) this.lookup(JsonService.ROLE);
    }

    // seems to have no effect
    // @RunWith(Parameterized.class)
    public static class JSONBeansTests extends BaseUnit4Test {

        private volatile CountDownLatch fSynchronizer;
        static int N = 3;// number of concurrent test methods = methods

        @Before
        public void init() throws ComponentException {
            fSynchronizer = new CountDownLatch(N);
        }

        @Test
        public void one() throws InterruptedException {
            String result = doJob("name");
            assertTrue("Result does contain type", !result.contains("org.apache.fulcrum.json.jackson.example.Bean"));
            assertTrue("Result does contain type", !result.contains("java.util.ArrayList"));
            fSynchronizer.countDown();
            // assertTrue("waiting failed", fSynchronizer.await(TIMEOUT, TimeUnit.SECONDS));
            fOne = Thread.currentThread();
            System.out.println(fThree + " count:" + fSynchronizer.getCount());
        }

        @Test
        public void two() throws InterruptedException {
            String result = doJob("name", "age");

            assertTrue("Result does contain type", !result.contains("org.apache.fulcrum.json.jackson.example.Bean"));
            assertTrue("Result does contain type", !result.contains("java.util.ArrayList"));
            fSynchronizer.countDown();
            // assertTrue("waiting failed", fSynchronizer.await(TIMEOUT, TimeUnit.SECONDS));
            fTwo = Thread.currentThread();
            System.out.println(fThree + " count:" + fSynchronizer.getCount());
        }

        @Test
        public void three() throws InterruptedException, JsonProcessingException {
            ObjectMapper mapper = customMapper(true);
            // ((Jackson2MapperService) jsonService).setMapper(mapper);
            // String result = doTaskJob("name", "age","profession");
            String result = doFilteredJob(mapper, new String[] { "age", "profession" });
            assertTrue("Result does not contain type, which it should",
                    result.contains("org.apache" + ".fulcrum.json.jackson.example.Bean"));
            assertTrue("Result does not contain type, which it should", result.contains("java.util.ArrayList"));
            assertTrue("Result should not contain attribute name", !result.contains("\"name\""));
            fSynchronizer.countDown();
            // assertTrue("waiting failed", fSynchronizer.await(TIMEOUT, TimeUnit.SECONDS));
            fThree = Thread.currentThread();
            System.out.println(fThree + " count:" + fSynchronizer.getCount());
        }

        private String doJob(String... filtereIds) {
            final int randInt = randInt(999, 1001);
            List<Bean> tasks = getBeans(randInt);
            String result = getJson(tasks, Bean.class, true, filtereIds);
            System.out.println("ser result (id=" + Thread.currentThread() + "):" + result);
            for (int i = 0; i < filtereIds.length; i++) {
                assertTrue("Result does not contain (" + filtereIds[i] + ")",
                        result.contains("\"" + filtereIds[i] + "\""));
            }
            return result;
        }

        private String doFilteredJob(ObjectMapper mapper, String... filtereIds) throws JsonProcessingException {
            final int randInt = randInt(999, 1001);
            List<Bean> tasks = getBeans(randInt);
            String result = serFiltered(tasks, Bean.class, mapper, filtereIds);
            System.out.println("ser result (id=" + Thread.currentThread() + "):" + result);
            for (int i = 0; i < filtereIds.length; i++) {
                assertTrue("Result does not contain (" + filtereIds[i] + ")",
                        result.contains("\"" + filtereIds[i] + "\""));
            }
            return result;
        }

        private String serFiltered(List<?> list, Class<?> filterClass, ObjectMapper mapper, String[] filterAttr)
                throws JsonProcessingException {
            Collection<AnnotationIntrospector> ais = ((AnnotationIntrospectorPair) mapper.getSerializationConfig()
                    .getAnnotationIntrospector()).allIntrospectors();
            for (AnnotationIntrospector ai : ais) {
                if (ai instanceof SimpleNameIntrospector) {
                    // activate filtering
                    ((SimpleNameIntrospector) ai).setFilteredClasses(filterClass);
                }
            }
            PropertyFilter pf = SimpleBeanPropertyFilter.filterOutAllExcept(filterAttr);
            SimpleFilterProvider filter = new SimpleFilterProvider();
            filter.setDefaultFilter(pf);
            return mapper.writer(filter).writeValueAsString(list);
        }

        private ObjectMapper customMapper(boolean withType) {
            // without copy test fails probably in testsRunInParallel
            ObjectMapper objectMapper = null;
            if (withType) {
                // without copy tests will fail in testsRunInParallel
                objectMapper = ((Jackson2MapperService) jsonService).getMapper().copy();
                objectMapper.enableDefaultTypingAsProperty(DefaultTyping.NON_FINAL, "type");
                AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
                AnnotationIntrospector siai = new SimpleNameIntrospector();
                AnnotationIntrospector pair = new AnnotationIntrospectorPair(siai, ai);
                objectMapper.setAnnotationIntrospector(pair);
            } else {
                objectMapper = ((Jackson2MapperService) jsonService).getMapper();
            }
            return objectMapper;
        }

        private List<Bean> getBeans(int randNum) {
            List<Bean> beans = new ArrayList<Bean>();
            for (int i = 0; i < 50; i++) {
                Bean dct = new Bean();
                dct.setName("title" + i);
                dct.setAge(randNum + i);
                dct.profession = "prof" + i;
                beans.add(dct);
            }
            return beans;
        }

        private String getJson(List<?> list, Class clazz, Boolean refresh, String... props) {
            String result = "";
            try {
                result = jsonService.serializeOnlyFilter(list, clazz, refresh, props);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                fail();
            }
            return result;
        }
    }

    @Test
    public void testsRunInParallel() {
        // Parallel among methods in a class in one thread pool
        Result result = JUnitCore.runClasses(ParallelComputer.methods(), JSONBeansTests.class);
        System.out.println("fOne:" + fOne);
        System.out.println("fTwo:" + fTwo);
        System.out.println("fThree:" + fThree);
        assertNotNull("Parallel Init exist" + getFailures(result), result.getFailureCount() == 0);
        assertTrue("Parallel Init" + getTrace(result), result.wasSuccessful());

        assertNotNull(fOne);
        assertNotNull(fTwo);
        assertNotNull(fThree);

        // if multiple where > 1 sometimes the following statements would fail with high
        // confidence
        //
//		assertThat(fOne, is(not(fTwo)));
//		assertThat(fTwo, is(not(fThree)));
//		assertThat(fOne, is(not(fThree)));
    }

    public static String getFailures(Result result) {
        List<Failure> failures = result.getFailures();
        StringBuffer sb = new StringBuffer();
        for (Failure failure : failures) {
            sb.append(failure.getMessage());
            // System.out.println(failure.getMessage());
        }
        return sb.toString();
    }

    public static String getTrace(Result result) {
        List<Failure> failures = result.getFailures();
        StringBuffer sb = new StringBuffer();
        for (Failure failure : failures) {
            failure.getException().printStackTrace();
            if (failure.getException().getCause() != null)
                failure.getException().getCause().printStackTrace();
            sb.append(failure.getTrace());
        }
        return sb.toString();
    }

    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

}
