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

import com.google.testing.instrumentation.InstrumentedClassLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs a set of multithreaded tests. This is an abstract base class that
 * defines the basic framework for running tests. Subclasses will add specific
 * mechanisms for defining tests.
 * <p>
 * To create a multithreaded test, define a class with a public no-arg
 * constructor, and a set of public instance methods that run the actual test
 * cases. These methods typically use custom annotations to identify them. To
 * run the tests, use the {@link BaseThreadedTestRunner#runTests(Class, Class...)}
 * method in one of the regular unit test cases. For example, using JUnit 4 syntax:
 * <pre>
 * public class MyClassTest {
 *
 *   public MyClassTest() {}
 *
 *   // This method is invoked as part of the regular unit test
 *   &#064;Test
 *   public void runThreadedTests {
 *     new BaseThreadedTestRunner().runTests(MyClassTest.class, MyClass.class);
 *   }
 *
 *   // This method is invoked by the BaseThreadedTestRunner. It should not be
 *   // invoked as part of the regular unit test.
 *   &#064;SomeTestAnnotation
 *   public void testThreading {
 *     ...
 *   }
 *
 * </pre>
 * Note that when a test is run via the BaseThreadedTestRunner, the test targets
 * will have been instrumented, allowing {@link Breakpoint}s to be created. For
 * this reason, the threaded tests cannot be run directly from the test harness,
 * as the test targets will not have been correctly instrumented. Internally,
 * the test framework reloads the clases using an instrumenting classloader.
 *
 * @see Instrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class BaseThreadedTestRunner {

  /** The name of the method in BaseTestWrapper that is invoked to run the tests. */
  private static final String RUN_TESTS = "runTests";

  /** The name of the setDebug method in Options */
  private static final String SET_DEBUG = "setDebug";

  /** The name of the setTimeout method in Options */
  private static final String SET_TIMEOUT = "setTimeout";

  /** The name of the setMethodOption method in Options */
  private static final String SET_METHOD_OPTION = "setMethodOption";

  private boolean debug = Options.DEFAULT_DEBUG;
  private long timeout = Options.DEFAULT_TIMEOUT;
  private MethodOption methodOption = Options.DEFAULT_METHOD_OPTION;
  private Set<String> methodNames;

  public BaseThreadedTestRunner() {
    // Nothing
  }

  /**
   * Sets the debug mode. (The default is false.) If true, then additional
   * debugging information is printed to stdout. Useful for debugging tests that
   * fail.
   */
  public void setDebug(boolean newDebug) {
    this.debug = newDebug;
  }

  /**
   * Sets the timeout value (in milliseconds) used for all internal waits and
   * joins. Any thread that takes longer than this time to terminate, or to
   * reach a break point will trigger an exception (and hence a test failure.)
   */
  public void setTimeout(long newTimeout) {
    this.timeout = newTimeout;
  }

  /**
   * Sets the option that determine which methods are run. Iff the option is {@link
   * MethodOption#LISTED_METHODS}, newMethodNames must be a non-null non-empty set.
   * The set of method names is in the format "classname.methodname", e.g. 
   * "com.google.project.MyClass.myMethod". <p>
   * If not explicitly set, the default value is {@link MethodOption#MAIN_METHOD}.
   */
  public void setMethodOption(MethodOption option, Set<String> newMethodNames) {
    boolean namesSpecified = (newMethodNames != null && newMethodNames.size() > 0);
    if (option == MethodOption.LISTED_METHODS) {
      if (!namesSpecified) {
        throw new IllegalArgumentException(
            "Must specify at least one method when using LISTED_METHODS");
      }
      methodNames = new HashSet<String>(newMethodNames);
    } else {
      if (namesSpecified) {
        throw new IllegalArgumentException(
            "Cannot specify method names except when using LISTED_METHODS");
      }
    }
    methodOption = option;
  }

  /**
   * Returns true when called within a multithreaded test that has been executed
   * via a subclass of this class. Returns false otherwise.
   */
  public boolean inThreadedTest() {

    // We're in a threaded test if we have been loaded by an
    // InstrumentedClassLoader. (See runTests()). Note that we have to do a
    // name-based comparison. If we have been loaded by an
    // InstrumentedClassLoader, then the class of that loader is not the same as
    // the InstrumentedClassLoader.class variable that we can reference directly
    // here. The InstrumentedClassLoader that loaded us was itself loaded by the
    // system class loader in runTests(). The InstrumentedClassLoader.class
    // variable was then loaded by the InstrumentedClassLoader that loaded us.
    //
    ClassLoader cl = getClass().getClassLoader();
    return cl.getClass().getName().equals(InstrumentedClassLoader.class.getName());
  }

  /**
   * Run the multithreaded tests defined in a given class. This method creates a
   * new instance of the given class, and invokes the relevant test methods.
   * <p>
   *
   * A multithreaded test will normally require at least one class to be
   * instrumented. To specify a single such class, use this method. To specify
   * multiple classes, use {@link #runTests(Class, List)}.
   *
   * @param test the class containing a set of multithreaded test methods.
   * @param targets the classes which should be instrumented.
   *
   * @see Instrumentation
   * @see ClassInstrumentation
   *
   * @throws IllegalArgumentException if the test class cannot be loaded, or its
   * test methods cannot be invoked. Will also throw any RuntimeExceptions thrown
   * by the invoked test methods.
   */
  public void runTests(Class<?> test, Class<?>... targets) {
    List<String> targetNames = new ArrayList<String>(targets.length);
    for (Class<?> target : targets) {
      targetNames.add(target.getName());
    }
    runTests(test.getName(), targetNames);
  }

  /**
   * Run the multithreaded tests defined in a given class. This method creates a
   * new instance of the given class, and invokes the relevant test methods.
   *
   * @param tester the class containing a set of multithreaded test methods.
   * @param targets the classes which should be instrumented.
   *
   * @see Instrumentation
   * @see ClassInstrumentation
   *
   * @throws IllegalArgumentException if the test class cannot be loaded, or its
   * test methods cannot be invoked. Will also throw any RuntimeExceptions
   * thrown by the invoked test methods.
   */
  public final void runTests(Class<?> tester, List<Class<?>> targets) {
    List<String> targetNames = new ArrayList<String>(targets.size());
    for (Class<?> target : targets) {
      targetNames.add(target.getName());
    }
    runTests(tester.getName(), targetNames);
  }

  @SuppressWarnings("unchecked")
  private void runTests(String mainName, List<String> targets) {
    TestInstrumenter instrumenter = new TestInstrumenter(targets);
    InstrumentedClassLoader loader = new InstrumentedClassLoader(instrumenter);
    setOptions(loader);
    String wrapperName = getWrapperName();
    Class<?> wrapperClass = loader.getExpectedClass(wrapperName);
    Class<?> testClass = loader.getExpectedClass(mainName);
    Object wrapper = MethodCaller.newInstance(wrapperClass);
    Method runTests = MethodCaller.getDeclaredMethod(wrapperClass, RUN_TESTS,
        Class.class, List.class);
    MethodCaller.invoke(runTests, wrapper, testClass, targets);
  }

  /**
   * Sets the options values in the Options class loaded by the test class
   * loader.
   */
  private void setOptions(InstrumentedClassLoader loader) {
    if (debug != Options.DEFAULT_DEBUG || timeout != Options.DEFAULT_TIMEOUT) {
      Class<?> optionsClass = loader.getExpectedClass(Options.class.getName());
      Class<?> methodOptionClass = loader.getExpectedClass(MethodOption.class.getName());
      Method setDebug = MethodCaller.getDeclaredMethod(optionsClass, SET_DEBUG, Boolean.TYPE);
      Method setTimeout = MethodCaller.getDeclaredMethod(optionsClass, SET_TIMEOUT, Long.TYPE);
      Method setMethodOption = MethodCaller.getDeclaredMethod(
          optionsClass, SET_METHOD_OPTION, Integer.TYPE, Set.class);

      // Need to make the methods accessible. Although Options is in our
      // package, this is an instance of the Options class in a different
      // classloader, and hence not accessible by default
      setDebug.setAccessible(true);
      setTimeout.setAccessible(true);
      setMethodOption.setAccessible(true);
      MethodCaller.invoke(setDebug, null, Boolean.valueOf(debug));
      MethodCaller.invoke(setTimeout, null, Long.valueOf(timeout));
      MethodCaller.invoke(setMethodOption, null, methodOption.value, methodNames);
    }
  }

  /**
   * Gets the name of the wrapper class that runs the test. The named class
   * should be a subclass of {@link BaseTestWrapper}.
   * <p>
   * This method is overridden by concrete implementations. An instance of the
   * wrapper class will be created when {@link #runTests} is invoked, and the
   * {@link BaseTestWrapper#runTests} method will be invoked.
   */
  protected abstract String getWrapperName();
}
