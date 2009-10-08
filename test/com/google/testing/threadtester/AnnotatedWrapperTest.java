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
import java.util.List;

/**
 * Verifies that test cases can be run using an AnnotatedTestRunner
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class AnnotatedWrapperTest extends TestCase {

  AnnotatedTestRunner runner = new AnnotatedTestRunner();
  private SimpleClass simple;
  private SimpleClass2 simple2;

  private static int numVerifyCalls;
  private static int numVerifyCalls2;
  private static int numAfterCalls;

  private static List<Integer> positions;
  private static List<Integer> positions2;

  public void testThreadedTests() {
    try {
      runner.runTests(getClass(), SimpleClass.class, SimpleClass2.class);
    } catch (RuntimeException e) {
      Throwable cause = e.getCause();
      System.out.printf("Cause = %s\n", cause);
      while (cause.getCause() != null) {
        cause = cause.getCause();
      }
      System.out.printf("Cause = %s\n", cause);
    }
  }

  @ThreadedBeforeAll
  public static void beforeAll() {
    positions = new ArrayList<Integer>();
    positions2 = new ArrayList<Integer>();
  }

  @ThreadedBefore
  public void before() {
    // Create both a SimpleClass and a SimpleClass2 instance. Each individual
    // test will only interleave a method from one of these classes, but we want
    // to verify that the test framework doesn't get confused if we instantiate
    // instances of two different classes here.
    simple = new SimpleClass();
    simple2 = new SimpleClass2();

    // Ensure that we invoke some methods on the test instances. These are not
    // the methods that we invoke in the ThreadedMain section of the test, and
    // the framework should ignore these. We want to verify this behaviour.
    // Note that the setSecond() call is meaningful to subsequent tests. The
    // getArbitraryValue() call is arbitrary.
    simple.setSecond(simple2);
    simple2.getArbitraryValue();
  }

  @ThreadedAfter
  public void after() {
    numAfterCalls++;
  }


  @ThreadedMain
  public void mainThread(){
    simple.add(0, 1);
    System.out.printf("#### Finished add\n");

    simple.add(Integer.valueOf(1), Integer.valueOf(1));
    simple2.getValue();
  }

  @ThreadedSecondary
  public void secondaryThread() {
    positions.add(simple.position);
    System.out.printf("#### Finished secondary\n");
  }

  @ThreadedVerification
  public void verify() {
    numVerifyCalls++;
  }

  @ThreadedMain(name = "test2")
  public void mainThread2() {
    simple2.getValue();
  }

  @ThreadedSecondary(name = "test2")
  public void secondaryThread2() {
    positions2.add(simple2.position);
  }

  @ThreadedVerification(name = "test2")
  public void verify2() {
    numVerifyCalls2++;
  }


  @ThreadedMain(name = "test3")
  public void mainThread3() {
    simple2.getValue();
  }

  @ThreadedSecondary(name = "test3")
  public void secondaryThread3() {
    positions2.add(simple2.position);
  }

  @ThreadedVerification(name = "test3")
  public void verify3() {
    numVerifyCalls2++;
  }

  @ThreadedAfterAll
  public static void afterAll() {
    // The expected sequence of positions for the first test is 0, 1, 2, 2.
    // There are 4 executable lines in SimpleClass.add(), and we should break
    // before each of them. Only the first two lines update the position
    // variable, so when we break before the 4th line, position will not have
    // changed.
    assertEquals(4, positions.size());
    assertEquals(Integer.valueOf(0), positions.get(0));
    assertEquals(Integer.valueOf(1), positions.get(1));
    assertEquals(Integer.valueOf(2), positions.get(2));
    assertEquals(Integer.valueOf(2), positions.get(3));
    assertEquals(4, numVerifyCalls);

    // For the second test, we expect 0, 1, 2. (See source.)
    assertEquals(3, positions2.size());
    assertEquals(Integer.valueOf(0), positions2.get(0));
    assertEquals(Integer.valueOf(1), positions2.get(1));
    assertEquals(Integer.valueOf(2), positions2.get(2));
    assertEquals(3, numVerifyCalls2);

    assertEquals(7, numAfterCalls);

  }
}
