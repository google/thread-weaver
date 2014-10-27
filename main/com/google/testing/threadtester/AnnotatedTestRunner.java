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
 * Runs a set of multithreaded tests defined by a series of annotations. To
 * create a set of tests, define a class with a public no-arg constructor, and a
 * set of publically annotated methods. A typical test setup would be:
 *
 * <pre>
 * public class MyClassTest extends TestCase {
 *   public MyClassTest() {}
 *
 *   private MyClass myClass;
 *
 *   // This method is invoked as part of the regular unit test
 *   &#064;Test
 *   public void runThreadedTests {
 *     new AnnotatedTestRunner().runTests(MyClassTest.class, MyClass.class);
 *   }
 *
 *   &#064;ThreadedBefore
 *   public void before() {
 *     myClass = new MyClass();
 *   }
 *
 *   &#064;ThreadedAfter
 *   public void after() {
 *     assertEquals("Hello World", myClass.getMessage());
 *   }
 *
 *   &#064;ThreadedMain(name="TestOne")
 *   public void testPrint() {
 *     myClass.printMessage("Hello");
 *   }
 *
 *   &#064;ThreadedSecondary(name="TestOne")
 *   public void testPrint2() {
 *     myClass.printMessage("World");
 *   }
 * }
 * </pre>
 *
 * The runner will run every test case defined in the test class. A test case
 * consists of an initialisation method with the
 * <code>&#064;ThreadedBefore</code> annotation, and a pair of execution
 * methods, one with the {@link ThreadedMain} annotation, and one with the
 * {@link ThreadedSecondary}. The execution annotations should have the same
 * name. The test runner will run each pair of annotated methods in two separate
 * threads, interleaving them to verify that the invoked calls are thread safe.
 * <p>
 * Before invoking each method pair, the runner will invoke the method with the
 * {@link ThreadedBefore} annotation. This method must create a new instance of
 * the object being tested. Additional setup code common the all of the tests
 * can also be added here. Note that only a single instance of the object under
 * test can be created.
 * <p>
 * After invoking each method pair, the {@link ThreadedAfter}
 * method will be invoked. This can be used to verify results, and to free
 * resources.
 * <p>
 * A test case may also contain an optional third method tagged with the {@link
 * ThreadedVerification} attribute. This will be invoked after the named test
 * case has been run, and can be used to verify that the particular case has run
 * successfully.
 * <p>
 * The sequence for a test case named "test1" is thus:
 * <code><ul>
 * <li>&#064;ThreadedBefore
 * <li>&#064;ThreadedMain("test1")
 * <li>&#064;ThreadedSecondary("test1")
 * <li>&#064;ThreadedVerification("test1");
 * <li>&#064;ThreadedAfter();
 * </ul></code>
 * <p>
 * The interleaving of the main and secondary methods is handled automatically
 * by the test framework. It will iterate over the set of testable methods. For 
 * each executable line of each testable method, the framework will:
 * <ul>
 * <li>Invoke the <code>&#064;ThreadedBefore</code> method</li>
 * <li>Invoke the <code>&#064;ThreadedMain</code> method, and pause it at the line</li>
 * <li>Invoke the <code>&#064;ThreadedSecondary</code> method</li>
 * <li>Allow the <code>&#064;ThreadedMain</code> method to continue</li>
 * <li>Invoke the <code>&#064;ThreadedAfter</code> method</li>
 * </ul>
 *
 * Note that in some cases, the <code>&#064;ThreadedMain</code> method may not
 * invoke a call that hits the executable line. In that case, the main method
 * will run to its full completion before the  <code>&#064;ThreadedSecondary</code>
 * method runs.
 * <p>
 * Finally, the entire test class may define one static method with the {@link
 * ThreadedBeforeAll} annotation, and one static method with the {@link
 * ThreadedAfterAll}. These methods are invoked before and after all of the test
 * cases have been run.
 * <p>
 * The set of testable methods is determined by the {@link MethodOption} set in
 * {@link BaseThreadedTestRunner#setMethodOption}. Possible options are:
 * <ul>
 * <li>MAIN_METHOD. (Default.) The framework will analyse the
 * <code>&#064;ThreadedMain</code> method to find the first call to a method in 
 * the class-under-test. This become the only testable method.</li>
 *
 * <li>ALL_METHODS. The framework will analyse the <code>&#064;ThreadedMain</code> method
 * to recursively find all calls to methods in the classes-under-test. (I.e. all methods
 * called directly, and all methods called by those methods. Note that tests using this
 * option may run slowly if the instrumented classes have deep call heirarchies.</li>
 *
 * <li>LISTED_METHODS. All methods listed in the call to
 * {@link BaseThreadedTestRunner#setMethodOption} are testable.</li>
 * </ul>
 *
 * @see BaseThreadedTestRunner
 * @see Instrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class AnnotatedTestRunner extends BaseThreadedTestRunner {
  @Override
  protected String getWrapperName() {
    return AnnotatedTestWrapper.class.getName();
  }
}
