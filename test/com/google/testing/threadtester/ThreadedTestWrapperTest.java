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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for ThreadedTestWrapper
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ThreadedTestWrapperTest extends TestCase {

  /**
   * Empty list of instrumented classes, passed in to
   * {@link ThreadedTestWrapper#runTests}. This test does not
   * use instrumentation.
   */
  private List<String> instrumented = Collections.emptyList();

  /** Represents the a called method. */
  enum Call {
    BEFORE,
    FIRST_METHOD,
    SECOND_METHOD,
    THIRD_METHOD,
    AFTER
  }

  static class ClassWithValidAnnotations {

    static ClassWithValidAnnotations lastInstance;
    List<Call> calls = new ArrayList<Call>();

    public ClassWithValidAnnotations() {
      lastInstance = this;
    }

    @ThreadedBefore
    public void before() {
      calls.add(Call.BEFORE);
    }

    @ThreadedAfter
    public void after() {
      calls.add(Call.AFTER);
    }

    @ThreadedTest
    public void firstMethod() {
      calls.add(Call.FIRST_METHOD);
    }

    @ThreadedTest(expected = IOException.class)
    public void secondMethod() throws IOException {
      calls.add(Call.SECOND_METHOD);
      throw new IOException();
    }

    public void thirdMethod() {
      calls.add(Call.THIRD_METHOD);
    }
  }

  static class ClassWithMethodThatDoesNotThrow extends ClassWithValidAnnotations {
    @SuppressWarnings("unused")
    @Override
    @ThreadedTest(expected = IOException.class)
    public void secondMethod() throws IOException {
      // Don't throw an exception. This should be a test failure.
    }
  }

  static class ClassWithTwoBeforeMethods extends ClassWithValidAnnotations {
    @ThreadedBefore
    public void secondBefore() {
      // Do nothing
    }
  }

  static class ClassWithTwoAfterMethods extends ClassWithValidAnnotations {
    @ThreadedAfter
    public void secondAfter() {
      // Do nothing
    }
  }

  static class ClassWithNoTestMethods {
    public void someMethod() {
      // Do nothing
    }
  }

  public void testRunTests_normalExecution() {
    new ThreadedTestWrapper().runTests(ClassWithValidAnnotations.class, instrumented);
    ClassWithValidAnnotations target = ClassWithValidAnnotations.lastInstance;
    assertNotNull(target);

    // We expect a call to the first and second methods, each bracketed with a
    // before and after. There's no guarantee about the ordering of the first and
    // second calls, so we have to expect wither ordering.
    assertEquals(6, target.calls.size());
    assertEquals(Call.BEFORE, target.calls.get(0));
    Call firstTestCall = target.calls.get(1);
    assertTrue(firstTestCall == Call.FIRST_METHOD || firstTestCall == Call.SECOND_METHOD);
    assertEquals(Call.AFTER, target.calls.get(2));

    assertEquals(Call.BEFORE, target.calls.get(3));
    Call secondTestCall = target.calls.get(4);
    if (firstTestCall == Call.FIRST_METHOD) {
      assertTrue(secondTestCall == Call.SECOND_METHOD);
    } else {
      assertTrue(secondTestCall == Call.FIRST_METHOD);
    }
    assertEquals(Call.AFTER, target.calls.get(5));
  }

  public void testRunTests_nonThrowingMethodCausesException() {
    try {
      new ThreadedTestWrapper().runTests(ClassWithMethodThatDoesNotThrow.class, instrumented);
      fail("Method that does not throw the annotated exception should cause the run to fail");
    } catch (RuntimeException e) {
      // expected
    }
  }

  public void testRunTests_twoBeforeMethodsCausesException() {
    try {
      new ThreadedTestWrapper().runTests(ClassWithTwoBeforeMethods.class, instrumented);
      fail("Class with two ThreadedBefore methods should cause the run to fail");
    } catch (RuntimeException e) {
      // expected
    }
  }

  public void testRunTests_twoAfterMethodsCausesException() {
    try {
      new ThreadedTestWrapper().runTests(ClassWithTwoAfterMethods.class, instrumented);
      fail("Class with two ThreadedAfter methods should cause the run to fail");
    } catch (RuntimeException e) {
      // expected
    }
  }

  public void testRunTests_noTestMethodsCausesException() {
    try {
      new ThreadedTestWrapper().runTests(ClassWithNoTestMethods.class, instrumented);
      fail("Class with no test methods should cause the run to fail");
    } catch (RuntimeException e) {
      // expected
    }
  }
}
