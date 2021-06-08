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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.jackson.Jackson2MapperService;
import org.apache.fulcrum.json.jackson.SimpleNameIntrospector;
import org.apache.fulcrum.json.jackson.example.Bean;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@TestInstance(Lifecycle.PER_CLASS)
public class JSONConcurrentJunit5Test extends BaseUnit5Test {

    private static volatile Thread fOne = null;
    private static volatile Thread fTwo = null;
    private static volatile Thread fThree = null;
    private static JsonService jsonService = null;

//    
    public JSONConcurrentJunit5Test() throws Exception {
    }

    @BeforeAll
    public static void setUp() throws Exception {

    }

    @BeforeEach
    public void init() throws ComponentException {
        fOne = null;
        fTwo = null;
        fThree = null;
        jsonService = (JsonService) this.lookup(JsonService.ROLE);
    }

    // @TestInstance(Lifecycle.PER_CLASS)
    @Execution(ExecutionMode.CONCURRENT)
    public static class JSONBeansTests extends BaseUnit5Test implements TestExecutionExceptionHandler {

        @BeforeEach
        public void init() throws ComponentException {

        }

        @ParameterizedTest
        @MethodSource("multipleRandStream")
        public void one(int randStreamInt) throws InterruptedException {
            String result = doJob(randStreamInt, "name");
            assertTrue(!result.contains("org.apache.fulcrum.json.jackson.example.Bean"), "Result does contain type");
            assertTrue(!result.contains("java.util.ArrayList"), "Result does contain type");
            fOne = Thread.currentThread();
        }

        @ParameterizedTest
        @MethodSource("multipleRandStream")
        public void two(int randStreamInt) throws InterruptedException {
            String result = doJob(randStreamInt, "name", "age");
            assertTrue(!result.contains("org.apache.fulcrum.json.jackson.example.Bean"), "Result does contain type");
            assertTrue(!result.contains("java.util.ArrayList"), "Result does contain type");
            fTwo = Thread.currentThread();
        }

        @ParameterizedTest
        @MethodSource("multipleRandStream")
        public void three(int randStreamInt) throws InterruptedException, JsonProcessingException {
            ObjectMapper mapper = customMapper(true);
            // ((Jackson2MapperService) jsonService).setMapper(mapper);
            // String result = doTaskJob("name", "age","profession");
            String result = doFilteredJob(mapper, randStreamInt, new String[] { "age", "profession" });
            assertTrue(result.contains("org.apache.fulcrum.json.jackson.example.Bean"),
                    "Result does not contain type, but it shouldn't");
            assertTrue(result.contains("java.util.ArrayList"), "Result does not contain type, but it shouldn't");
            assertTrue(!result.contains("\"name\""), "Result should not contain attribute name");
            fThree = Thread.currentThread();
        }

        private String doJob(int randStreamInt, String... filtereIds) {
            List<Bean> tasks = getBeans(randStreamInt);
            String result = getJson(tasks, Bean.class, true, filtereIds);
            System.out.println("ser result (id=" + Thread.currentThread() + "):" + result);

            List<String> fidsArr = Arrays.asList(filtereIds);
            fidsArr.forEach(fidId -> {
                assertTrue(result.contains("\"" + fidId + "\""), "Result does not contain (" + fidId + ")");
                assertTrue(result.contains(randStreamInt + ""), result + " does not contain " + randStreamInt + ")");
            });
            return result;
        }

        private String doFilteredJob(ObjectMapper mapper, int randStreamInt, String... filtereIds)
                throws JsonProcessingException {
            List<Bean> tasks = getBeans(randStreamInt);
            String result = serFiltered(tasks, Bean.class, mapper, filtereIds);
            System.out.println("ser result (id=" + Thread.currentThread() + "):" + result);
            List<String> fidsArr = Arrays.asList(filtereIds);
            fidsArr.forEach(fidId -> {
                assertTrue(result.contains("\"" + fidId + "\""), "Result does not contain (" + fidId + ")");
                assertTrue(result.contains(randStreamInt + ""), result + " does not contain " + randStreamInt + ")");
            });
            return result;
        }

        private String serFiltered(List<?> list, Class<?> filterClass, ObjectMapper mapper, String[] filterAttr)
                throws JsonProcessingException {
            Collection<AnnotationIntrospector> ais = ((AnnotationIntrospectorPair) mapper.getSerializationConfig()
                    .getAnnotationIntrospector()).allIntrospectors();
            // activate filtering
            ais.stream().filter(ai -> ai instanceof SimpleNameIntrospector)
                    .forEach(ai -> ((SimpleNameIntrospector) ai).setFilteredClasses(filterClass));
            PropertyFilter pf = SimpleBeanPropertyFilter.filterOutAllExcept(filterAttr);
            SimpleFilterProvider filter = new SimpleFilterProvider();
            filter.setDefaultFilter(pf);
            return mapper.writer(filter).writeValueAsString(list);
        }

        private ObjectMapper customMapper(boolean withType) {
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

        // randNum is in any property -> unconditionally checkable
        private List<Bean> getBeans(int randNum) {
            List<Bean> beans = new ArrayList<Bean>();
            IntStream.of(50).forEach(i -> {
                Bean dct = new Bean();
                dct.setName("title" + i + "_" + randNum);
                dct.setAge(randNum);
                dct.profession = "prof" + i + "_" + randNum;
                beans.add(dct);
            });
            return beans;
        }

        private String getJson(List<?> list, Class clazz, Boolean refresh, String... props) {
            try {
                return jsonService.serializeOnlyFilter(list, clazz, refresh, props);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                fail();
            }
            return null;
        }

        public static IntStream multipleRandStream() {
            return new Random().ints(5, 1, 1001);
        }

        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            throw throwable;
        }
    }

    /**
     *  Repeated Test
     * @param testReporter to report
     */
    @RepeatedTest(10)
    public void testsRunInParallel(TestReporter testReporter) {
        // Parallel among methods in a class in one thread pool
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(JSONBeansTests.class))
                // does not matter:
                // if JSONBeansTests is set to per_class
                // (junit.jupiter.testinstance.lifecycle.default) test methods may reuse threads
                // or
                // if it is default per_method.
                // .configurationParameter("junit.jupiter.testinstance.lifecycle.default",
                // "per_class")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true").build();
        TestExecutionSummary summary = defaultExecute(request);
        defaultAsserts(testReporter, summary);

        // this method may be executed multiple times, any method may get a reused thread at the end, no assert
//		assertThat(fOne, is(not(fTwo))); 
//		assertThat(fTwo, is(not(fThree)));
//		assertThat(fOne, is(not(fThree)));
    }

    /**
     * 
     * @param testReporter to reportfrom assets
     */
    @RepeatedTest(3)
    public void testsRunInSameThread(TestReporter testReporter) {
        // Parallel among methods in a class in one thread pool
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(JSONBeansTests.class))
                // .configurationParameter("junit.jupiter.testinstance.lifecycle.default",
                // "per_class")
                .build();
        TestExecutionSummary summary = defaultExecute(request);
        defaultAsserts(testReporter, summary);
        assertThat(fOne, is(fTwo));
        assertThat(fTwo, is(fThree));
        assertThat(fOne, is(fThree));
    }

    private TestExecutionSummary defaultExecute(final LauncherDiscoveryRequest request) {
        final Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        return listener.getSummary();
    }

    /**
     * 
     * @param testReporter to report summary, threads and failures
     * @param summary to get failures and test count
     */
    private void defaultAsserts(TestReporter testReporter, TestExecutionSummary summary) {
        List<Failure> failures = summary.getFailures();
        assertNotNull(summary.getTestsFoundCount() > 0, "No Tests found");

        testReporter.publishEntry("summary getTestsStartedCount:" + summary.getTestsStartedCount());
        testReporter.publishEntry("summary getTestsSucceededCount:" + summary.getTestsSucceededCount());
        testReporter.publishEntry("fOne:" + fOne);
        testReporter.publishEntry("fTwo:" + fTwo);
        testReporter.publishEntry("fThree:" + fThree);

        failures.forEach(failure -> {
            testReporter.publishEntry("failure - " + failure.getException());
            testReporter.publishEntry("failure at - " + failure.getTestIdentifier());
        });
        long testFoundCount = summary.getTestsFoundCount();

		assertNotNull(fOne);
		assertNotNull(fTwo);
		assertNotNull(fThree);

        assertEquals(testFoundCount, summary.getTestsSucceededCount(),
                "Not exactly " + testFoundCount + " Tests succeessfull");
        assertEquals(0, summary.getTestsFailedCount(), "No failed tests");
        assertTrue(failures.isEmpty(), "Parallel Test failures not empty");
    }

}
