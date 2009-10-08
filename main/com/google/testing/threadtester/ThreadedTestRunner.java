/*
 * Copyright 2009 Weaver authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.testing.threadtester;

/**
 * Runs a set of multithreaded tests. To create a multithreaded test, define a
 * class with a public no-arg constructor, and a set of public no-arg instance
 * methods annotated with the {@link ThreadedTest} annotation. The
 * ThreadedTestRunner will create a new instance of the test class, and invoke
 * each of the annotated methods in turn. For example, using JUnit 4 syntax:
 * <pre>
 * public class MyClassTest {
 *   public MyClassTest() {}
 *
 *   &064;Test
 *   public void runThreadedTests {
 *     new ThreadedTestRunner().runTests(MyClassTest.class, MyClass.class);
 *   }
 *
 *   &064;ThreadedTest
 *   public void testThreading {
 *     MyTest subject = new MyTest();
 *     ClassInstrumentationImpl instr =
 *         Instrumentation.getClassInstrumentation(MyClass.class);
 *     ...
 *   }
 * </pre>
 * <p>
 * Before invoking each method, the runner will invoke the method with the
 * {@link ThreadedBefore} annotation (if any). Setup code common the all of the tests
 * can be added here. After invoking each method, the @{link ThreadedAfter}
 * method will be invoked. This can be used to verify results, and to free
 * resources.
 * <p>
 * Note that this class is normally used with standalone tests that use an
 * {@link InterleavedRunner} or define their own {@link Breakpoint}s and manage
 * their own Threads. For simpler test cases, consider using the {@link
 * AnnotatedTestRunner}.
 *
 * @see BaseThreadedTestRunner
 * @see Instrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ThreadedTestRunner extends BaseThreadedTestRunner {

  @Override
  protected String getWrapperName() {
    return ThreadedTestWrapper.class.getName();
  }
}
