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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for AnnotatedTestWrapper. Note that this a small test that only
 * checks for various invalid combinations of the test annotations. Testing the
 * entire class requires the full instrumented test framework. See
 * {@link AnnotatedTestWrapper}
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class AnnotatedTestWrapperTest extends TestCase {

  /**
   * Empty list of instrumented classes, passed in to {@link
   * AnnotatedTestWrapper#runTests}. Many tests do not use instrumentation.
   */
  private List<Class<?>> instrumented = Collections.emptyList();

  /**
   * Utility method used by the majority of the test cases. Each test uses a
   * class with invalid annotations. We call
   * AnnotatedTestWrapper.getTestCases(), and expect an
   * IllegalArgumentException.
   */
  private void expectIllegalArgument(Class<?> clss) {
    try {
      new AnnotatedTestWrapper().getTestCases(clss, instrumented);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  /**
   * Utility class that is the target of a ThreadedMain method
   */
  static class InstrumentedTarget {
    public void aMethod() {
    }
  }

  static class ClassWithNoAnnotations {
    public void notAnnotated() {
    }
  }

  public void testGetTestCases_noAnnotations() {
    expectIllegalArgument(ClassWithNoAnnotations.class);
  }

  static class ClassWithMultipleAnnotations {
    @ThreadedBefore
    @ThreadedMain
    public void before() {
    }

    @ThreadedSecondary
    public void secondary() {
    }
  }

  public void testGetTestCases_multipleAnnotations() {
    expectIllegalArgument(ClassWithMultipleAnnotations.class);
  }

  static class ClassWithStaticAnnotations {
    @ThreadedBefore
    public static void before() {
    }

    @ThreadedMain
    public static void main() {
    }

    @ThreadedSecondary
    public static void secondary() {
    }
  }

  public void testGetTestCases_staticAnnotations() {
    expectIllegalArgument(ClassWithStaticAnnotations.class);
  }


  static class ClassWithMainAndNoSecond {
    @ThreadedBefore
    public static void before() {
    }

    @ThreadedMain
    public void main() {
    }
  }

  public void testGetTestCases_mainAndNoSecond() {
    expectIllegalArgument(ClassWithMainAndNoSecond.class);
  }

  static class ClassWithSecondAndNoMain {
    @ThreadedBefore
    public static void before() {
    }

    @ThreadedSecondary
    public void second() {
    }
  }

  public void testGetTestCases_secondAndNoMain() {
    expectIllegalArgument(ClassWithSecondAndNoMain.class);
  }

  static class ClassWithMultipleNames {
    @ThreadedBefore
    public static void before() {
    }

    @ThreadedMain(name = "Duplicate")
    public void main() {
    }

    @ThreadedSecondary(name = "Duplicate")
    public void second() {
    }

    @ThreadedSecondary(name = "Duplicate")
    public void secondSecond() {
    }
  }

  public void testGetTestCases_multipleNames() {
    expectIllegalArgument(ClassWithMultipleNames.class);
  }

  static class ClassWithNonStaticBeforeAll {
    @ThreadedBeforeAll
    public void beforeAll() {
    }
  }

  public void testGetTestCases_nonStaticBeforeAll() {
    expectIllegalArgument(ClassWithNonStaticBeforeAll.class);
  }

  static class ClassWithNonStaticAfterAll {
    @ThreadedAfterAll
    public void afterAll() {
    }
  }

  public void testGetTestCases_nonStaticAfterAll() {
    expectIllegalArgument(ClassWithNonStaticAfterAll.class);
  }

  static class ClassWithValidAnnotations {
    @ThreadedBeforeAll
    public static void beforeAll() {
    }

    @ThreadedBefore
    public void before() {
    }

    @ThreadedMain(name = "testCase")
    public void main() {
      // The ThreadedMain method is expected to invoke a method in one
      // of the instrumented classes.
      new InstrumentedTarget().aMethod();
    }

    @ThreadedSecondary(name = "testCase")
    public void secondary() {
    }

    @ThreadedVerification(name = "testCase")
    public void verification() {
    }

    @ThreadedAfter
    public void after() {
    }

    @ThreadedAfterAll
    public static void afterAll() {
    }
  }

  public void testGetTestCases_validAnnotations() throws Exception {
    Class<ClassWithValidAnnotations> testClass = ClassWithValidAnnotations.class;
    List<Class<?>> instrumentedClasses = new ArrayList<Class<?>>();
    instrumentedClasses.add(InstrumentedTarget.class);
    AnnotatedTestWrapper.TestCases cases = new AnnotatedTestWrapper().getTestCases(
        testClass, instrumentedClasses);

    // verify that the TestCase derived from the test class is correct.
    assertEquals(1, cases.size());
    assertEquals(testClass.getDeclaredMethod("beforeAll"), cases.beforeAllMethod);
    assertEquals(testClass.getDeclaredMethod("afterAll"), cases.afterAllMethod);
    AnnotatedTestWrapper.TestCase testCase = cases.get(0);
    assertEquals(testClass.getDeclaredMethod("before"), testCase.before);
    assertEquals(testClass.getDeclaredMethod("main"), testCase.main);
    assertEquals(testClass.getDeclaredMethod("secondary"), testCase.secondary);
    assertEquals(testClass.getDeclaredMethod("verification"), testCase.verification);
    assertEquals(testClass.getDeclaredMethod("after"), testCase.after);
  }

  static class ClassThatDoesNotCallInstrumentedClass extends ClassWithValidAnnotations {
    @Override
    @ThreadedMain(name = "testCase")
    public void main() {
      // This class deliberately fails to call a method in the instrumented
      // class.
    }
  }

  public void testGetTestCases_doesNotCallInstrumentedClass() {
    try {
      List<Class<?>> instrumentedClasses = new ArrayList<Class<?>>();
      instrumentedClasses.add(InstrumentedTarget.class);
      new AnnotatedTestWrapper().getTestCases(
          ClassThatDoesNotCallInstrumentedClass.class, instrumentedClasses);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

}
