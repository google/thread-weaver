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

import java.lang.reflect.Method;

/**
 * Verifies that Breakpoints can be created correctly in a simple test class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InstrumentedBreakpointTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass.class, SimpleInteger.class);
  }

  protected SimpleClass getTestObject() {
    return new SimpleClass();
  }

  @ThreadedTest
  public void callSimpleMethods_noBreakpoints() {
    // Verifies that we can call all of the simple methods in an instrumented
    // class without any errors arising from the added callbacks.
    SimpleClass simple = getTestObject();
    simple.add(1, 2);
    simple.add(Integer.valueOf(1), Integer.valueOf(2));
    simple.add(new SimpleInteger(1), new SimpleInteger(2));
    simple.unique();
    simple.unique2();
    simple.callSecond();
  }


  @ThreadedTest
  public void doBreakpoint_atStartAndEnd() throws Exception {
    final SimpleClass simple = getTestObject();
    ObjectInstrumentation<SimpleClass> obj = Instrumentation.getObjectInstrumentation(simple);
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    Method add = SimpleClass.class.getDeclaredMethod("add", int.class, int.class);
    CodePosition start = ic.atMethodStart(add);
    CodePosition end = ic.atMethodEnd(add);
    TestThread testThread = new TestThread("test thread") {
      @Override
      public void run() {
        simple.add(1, 2);
      }
    };
    Breakpoint startBreakpoint = obj.createBreakpoint(start, testThread);
    Breakpoint endBreakpoint = obj.createBreakpoint(end, testThread);
    testThread.start();
    startBreakpoint.await();
    assertEquals(0, simple.position);
    startBreakpoint.resume(endBreakpoint);
    endBreakpoint.await();
    assertEquals(2, simple.position);
    endBreakpoint.resume();
    testThread.finish();
    testThread.throwExceptionsIfAny();
  }

  @ThreadedTest
  public void doBreakpoint_atStartAndEndOfInnerMethod() throws Exception {
    final SimpleClass simple = getTestObject();
    ObjectInstrumentation<SimpleClass> obj = Instrumentation.getObjectInstrumentation(simple);
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    Method innerMethod = SimpleClass.class.getDeclaredMethod("innerMethod");
    CodePosition start = ic.atMethodStart(innerMethod);
    CodePosition end = ic.atMethodEnd(innerMethod);
    TestThread testThread = new TestThread("test thread") {
      @Override
      public void run() {
        simple.unique();
      }
    };
    Breakpoint startBreakpoint = obj.createBreakpoint(start, testThread);
    Breakpoint endBreakpoint = obj.createBreakpoint(end, testThread);
    testThread.start();
    startBreakpoint.await();
    assertEquals(7, simple.position);
    startBreakpoint.resume(endBreakpoint);
    endBreakpoint.await();
    assertEquals(11, simple.position);
    endBreakpoint.resume();
    testThread.finish();
    testThread.throwExceptionsIfAny();
  }



//   @ThreadedTest
//   public void getCodePosition_fromNameIsEqual() {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp = ic.atMethodStart("unique");
//     CodePosition cp2 = ic.atMethodStart("unique");
//     assertTrue(cp.equals(cp2));
//   }

//   @ThreadedTest
//   public void getCodePosition_fromNameAndMethodIsEqual() throws Exception {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp = ic.atMethodStart("unique");
//     CodePosition cp2 = ic.atMethodStart(SimpleClass.class.getDeclaredMethod("unique"));
//     assertTrue(cp.equals(cp2));
//   }

//   @ThreadedTest
//   public void getCodePosition_fromNamesAreNotEqual() {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp = ic.atMethodStart("unique");
//     CodePosition cp2 = ic.atMethodStart("unique2");
//     assertFalse(cp.equals(cp2));
//   }

//   @ThreadedTest
//   public void getCodePosition_startAndEndAreNotEqual() {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp = ic.atMethodStart("unique");
//     CodePosition cp2 = ic.atMethodEnd("unique");
//     assertFalse(cp.equals(cp2));
//   }

//   @ThreadedTest
//   public void getCodePosition_fromMethod() throws Exception {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp =
//         ic.atMethodStart(SimpleClass.class.getDeclaredMethod("add", Integer.class, Integer.class));
//   }

//   @ThreadedTest
//   public void getCodePosition_fromInvalidMethod() throws Exception {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     try {
//       CodePosition cp = ic.atMethodStart(Object.class.getDeclaredMethod("toString"));
//     fail();
//     } catch (IllegalArgumentException e) {
//       // Expected
//     }
//   }

//   @ThreadedTest (expected = IllegalArgumentException.class)
//   public void getCodePosition_duplicate() {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//       CodePosition cp = ic.atMethodStart("add");
//   }

//   @ThreadedTest (expected = IllegalArgumentException.class)
//   public void getCodePosition_nonExistent() {
//     ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
//     CodePosition cp = ic.atMethodStart("noSuchMethod");
//   }
}
