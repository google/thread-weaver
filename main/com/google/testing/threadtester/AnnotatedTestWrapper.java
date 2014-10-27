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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of BaseTestWrapper designed for use with {@link
 * AnnotatedTestRunner}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class AnnotatedTestWrapper implements BaseTestWrapper {

  /**
   * Represents a single test case. Contains the various methods that define the
   * test. E.g. the 'before' method, which is the method in the test class
   * tagged with the ThreadedBefore annotation.
   */
  // Visible for testing
  class TestCase {
    final String name;
    final Method before;
    final Method main;
    final Method secondary;
    final Method verification;
    final Method after;
    final Method target;

    TestCase(String name, Method before, Method main, Method secondary, Method verification,
        Method after, Method target) {
      this.name = name;
      this.before = before;
      this.main = main;
      this.secondary = secondary;
      this.verification = verification;
      this.after = after;
      this.target = target;
    }
  }

  /**
   * Represents a set of test cases, as defined in a single test class.
   */
  // Visible for testing
  class TestCases extends ArrayList<TestCase> {
    final Method beforeAllMethod;
    final Method afterAllMethod;

    TestCases(int size, Method beforeAllMethod, Method afterAllMethod) {
      super(size);
      this.beforeAllMethod = beforeAllMethod;
      this.afterAllMethod = afterAllMethod;
    }
  }

  /**
   * Gets an annotation from a method, and verifies that if the given annotation
   * type is non-null, then all of the other given annotation objects are null
   */
  private <T extends Annotation> T getUniqueAnnotation(Class<T> annotationClass, Method method,
      Annotation... others) {
    T annotation = method.getAnnotation(annotationClass);
    if (annotation != null) {
      if (Modifier.isStatic(method.getModifiers())) {
        throw new IllegalArgumentException("Cannot apply " + annotation + " to static method");
      }
      for (Annotation other : others) {
        if (other != null) {
          throw new IllegalArgumentException("Cannot combine " + annotation + " with " + other +
                                             " on method " + method);
        }
      }
    }
    return annotation;
  }

  /**
   * Adds a name->method pair to the given map, but throws an exception if the name already exists.
   */
  private void addUniqueMethod(Map<String, Method> map, String name, Method method,
      Annotation annotation) {
    if (map.containsKey(name)) {
      throw new IllegalArgumentException("Cannot have multiple " + annotation +
          " annotations with name " + name);
    }
    map.put(name, method);
  }

  /**
   * Verifies the annotations on the test class, and returns a list of TestCase objects defining
   * the tests in the test class.
   */
  // Visible for testing
  TestCases getTestCases(Class<?> testClass, List<Class <?>> instrumentedClasses) {
    Map<String, Method> mainMethods = new HashMap<String,Method>();
    Map<String, Method> secondaryMethods = new HashMap<String,Method>();
    Map<String, Method> verifyMethods = new HashMap<String,Method>();
    Method beforeMethod = null;
    Method beforeAllMethod = null;
    Method afterMethod = null;
    Method afterAllMethod = null;

    // If we are running in MAIN_METHOD mode then the method to interleave is produced by
    // analysing the method invoked by ThreadedMain, and the methodMap is used to identify
    // this. Otherwise we have an explicit list of methods. The list of targetMethods is
    // used to identify these.
    Map<Method, Set<Method>> methodMap = null;
    List<Method> targetMethods = null;
    MethodOption option = Options.getMethodOption();
    if (option == MethodOption.MAIN_METHOD) {
      // Get a list of all methods in the instrumentedClasses called directly by the test
      // class.  Below we will extract the specific method for each test.
      methodMap = new CallChecker().getCallers(testClass, instrumentedClasses);
    } else if (option == MethodOption.ALL_METHODS) {
      // Get a list of all methods in the instrumentedClasses called recursively by the
      // test class.  Below we will extract the specific method for each test.
      methodMap = new CallChecker().getAllCallers(testClass, instrumentedClasses);
    } else {
      CallLoggerFactory logger = CallLoggerFactory.getFactory();
      targetMethods = new ArrayList<Method>();
      Set<String> filter = Options.methodsToTest();
      for (Class <?> clss : instrumentedClasses) {
        ClassInstrumentation instrClss = logger.getClassInstrumentation(clss);
        for (MethodInstrumentation mi : instrClss.getMethods()) {
          Method m = mi.getUnderlyingMethod();
          Options.debugPrint("Checking %s.%s\n", clss.getName(), m.getName()); 
          if (filter.contains(clss.getName() + "." + m.getName())) {
            targetMethods.add(m);
          }
        }
      }
      if (targetMethods.size() == 0) {
        throw new IllegalStateException("No valid target methods were specified.");
      }
    }

    // Go through the methods in the test class, extracting and verifying the
    // annotations.
    Method[] methods = testClass.getMethods();
    for (Method method : methods) {

      ThreadedMain main = getUniqueAnnotation(ThreadedMain.class, method);
      if (main != null) {
        addUniqueMethod(mainMethods, main.name(), method, main);
      }

      ThreadedSecondary secondary = getUniqueAnnotation(ThreadedSecondary.class, method, main);
      if (secondary != null) {
        addUniqueMethod(secondaryMethods, secondary.name(), method, secondary);
      }

      ThreadedVerification verification =
          getUniqueAnnotation(ThreadedVerification.class, method, main, secondary);
      if (verification != null) {
        addUniqueMethod(verifyMethods, verification.name(), method, verification);
      }

      ThreadedBefore before =
          getUniqueAnnotation(ThreadedBefore.class, method, main, secondary, verification);
      if (before != null) {
        if (beforeMethod != null) {
          throw new IllegalArgumentException("Only one " + before + " annotation allowed");
        }
        beforeMethod = method;
      }

      ThreadedAfter after =
          getUniqueAnnotation(ThreadedAfter.class, method, main, secondary, verification, before);
      if (after != null) {
        if (afterMethod != null) {
          throw new IllegalArgumentException("Only one " + after  + " annotation allowed");
        }
        afterMethod = method;
      }

      ThreadedBeforeAll beforeAll = method.getAnnotation(ThreadedBeforeAll.class);
      if (beforeAll != null) {
        if (!Modifier.isStatic(method.getModifiers())) {
          throw new IllegalArgumentException("ThreadedBeforeAll only allowed on static methods");
        }
        beforeAllMethod = method;
      }

      ThreadedAfterAll afterAll = method.getAnnotation(ThreadedAfterAll.class);
      if (afterAll != null) {
        if (!Modifier.isStatic(method.getModifiers())) {
          throw new IllegalArgumentException("ThreadedAfterAll only allowed on static methods");
        }
        afterAllMethod = method;
      }
    }

    if (mainMethods.size() == 0) {
      throw new IllegalArgumentException("No methods tagged with @ThreadedMain");
    }
    if (beforeMethod == null) {
      throw new IllegalArgumentException("No method tagged with @ThreadedBefore");
    }

    // Create a new set of test case objects based on the annotations.
    //
    TestCases testCases = new TestCases(mainMethods.size(), beforeAllMethod, afterAllMethod);
    for (String name : mainMethods.keySet()) {
      Method secondaryMethod = secondaryMethods.get(name);
      if (secondaryMethod == null) {
        throw new IllegalArgumentException("No secondary method for test \"" + name + "\"");
      }
      secondaryMethods.remove(name);

      // If we have a methodMap, use that to generate the set of methods for this
      // test. Otherwise use the explicit list. See comments above.
      List<Method> targetMethodsForTest = null;
      if (methodMap != null) {
        // Find the target methods invoked by the main test method, using the map
        // from the CallChecker.
        Set<Method> targetMethodSet = methodMap.get(mainMethods.get(name));
        if (targetMethodSet == null) {
          throw new IllegalArgumentException("Method @ThreadedMain(\"" + name +
                                             "\") does not call a method in an instrumented class");
        }
        targetMethodsForTest = new ArrayList<Method>(targetMethodSet);
      } else {
        targetMethodsForTest = targetMethods;
      }
      for (Method targetMethod : targetMethodsForTest) {
        // Create a new test case. The verification method is optional, so we
        // don't test for it.
        testCases.add(new TestCase(name, beforeMethod, mainMethods.get(name), secondaryMethod,
                                   verifyMethods.get(name), afterMethod, targetMethod));
      }
    }
    // After creating the test cases, we should have removed all of the
    // secondary methods from the list. If any are left, we don't have
    // corresponding main methods.
    if (secondaryMethods.size() > 0) {
      for (String name : secondaryMethods.keySet()) {
        throw new IllegalArgumentException("Secondary method for test case \"" + name +
            "\" has no main method");
      }
    }
    return testCases;
  }

  @Override
  public void runTests(Class<?> testClass, List<String> instrumentedClassNames) throws Exception {
    Object mainObject = null;

    List<Class<?>> instrumentedClasses = new ArrayList<Class<?>>(instrumentedClassNames.size());
    for (String name : instrumentedClassNames) {
      instrumentedClasses.add(Class.forName(name));
    }

    // Get the test cases defined by the annotations
    TestCases testCases = getTestCases(testClass, instrumentedClasses);

    // And run the generated test cases, bracketed by the before/after methods.
    if (testCases.beforeAllMethod != null) {
      MethodCaller.invoke(testCases.beforeAllMethod, null);
    }

    runTestCases(testClass, testCases);

    if (testCases.afterAllMethod != null) {
      MethodCaller.invoke(testCases.afterAllMethod, null);
    }
  }

  private void runTestCases(Class<?> testClass, List<TestCase> testCases) {
    Options.debugPrint("Running tests for class %s\n", testClass);
    for (TestCase testCase : testCases) {
      Options.debugPrint("  test case %s has %s, %s, %s\n", testCase.name, testCase.main.getName(),
          testCase.secondary.getName(),
          testCase.verification == null ? "null" : testCase.verification.getName());
      MainTestCaseRunner main =
        new MainTestCaseRunner(testClass, testCase);
      SecondaryTestCaseRunner secondary = new SecondaryTestCaseRunner();
      RunResult result = InterleavedRunner.interleave(main, secondary);
      result.throwExceptionsIfAny();
    }
  }

  /**
   * Implementation of MainRunnable that runs the main thread of an
   * annotated test case. It does this by creating a new instance of the test
   * class, and invoking the methods defined in the TestCase on that new
   * instance. This runner is passed to an InterleavedRunner, and hence will be
   * invoked several times as the main and secondary threads are interleaved.
   */
  private class MainTestCaseRunner implements MainRunnable<Object>, ObjectCreationListener {

    /** The test case executed by the runner */
    private final TestCase testCase;

    /** The class that will execute the test. */
    private final Class<?> testRunnerClass;

    /**
     * The instance of the class that is executing the test. We create a new
     * instance for each interleaved test run.
     */
    private volatile Object testRunner;

    /**
     * The object-under-test. We expect the testRunner to create a new instance
     * for each interleaved test run.
     */
    volatile Object targetObject;

    /** The thread in which the initialize() method is called. */
    volatile Thread executionThread;

    MainTestCaseRunner(Class<?> testClass, TestCase testCase) {
      this.testRunnerClass = testClass;
      this.testCase = testCase;
    }

    @Override
    public void initialize() {

      // This method is invoked at the beginning of every test case. Create a
      // new instance of the test runner, and invoke its 'before' method. We
      // expect the before method to create a new instance of the class under
      // test. Record that instance using a CallLoggerFactory callback.
      testRunner = MethodCaller.newInstance(testRunnerClass);
      CallLoggerFactory factory = CallLoggerFactory.getFactory();
      try {
        executionThread = Thread.currentThread();
        targetObject = null;
        factory.addObjectCreationListener(this);
        MethodCaller.invoke(testCase.before, testRunner);
      } finally {
        factory.removeObjectCreationListener(this);
      }

      // Check to see if the testRunner's initialize method created an
      // appropriate mainObject.
      if (targetObject == null) {
        throw new IllegalStateException(
            "Neither @ThreadedBefore nor @ThreadedPrepare created a new test object of class "
            + getClassUnderTest().getName());
      }
    }

    @Override
    public void newObject(ObjectInstrumentationImpl<?> newObject, Thread thread) {
      // This callback will be invoked whenever a new InstrumentedObject is
      // created.  We use it in order to work out the mainObject
      if (thread.equals(executionThread)) {
        if (getClassUnderTest().isAssignableFrom(newObject.getUnderlyingObject().getClass())) {
          if (targetObject == null) {
            targetObject = newObject.getUnderlyingObject();
          } else {
            throw new IllegalStateException(
                "Creating second instance of " + targetObject.getClass().getName() +
                ". Only one instance can be created");
          }
        }
      }
    }

    @Override
    public Object getMainObject() {
      return targetObject;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Object> getClassUnderTest() {
      return (Class<Object>) getMethod().getDeclaringClass();
    }

    @Override
    public Method getMethod() {
      return testCase.target;
    }

    @Override
    public String getMethodName() {
      // Not used, as we implement getMethod()
      return null;
    }

    @Override
    public void terminate() {
      if (testCase.verification != null) {
        MethodCaller.invoke(testCase.verification, testRunner);
      }
      if (testCase.after != null) {
        MethodCaller.invoke(testCase.after, testRunner);
      }
    }

    @Override
    public void run() {
      MethodCaller.invoke(testCase.main, testRunner);
    }
  }

  private class SecondaryTestCaseRunner extends SecondaryRunnableImpl<Object, MainTestCaseRunner> {
    TestCase testCase;
    Object testRunner;

    @Override
    public void initialize(MainTestCaseRunner main) {
      this.testCase = main.testCase;
      this.testRunner = main.testRunner;
    }

    @Override
    public void run() {
      MethodCaller.invoke(testCase.secondary, testRunner);
    }
  }
}
