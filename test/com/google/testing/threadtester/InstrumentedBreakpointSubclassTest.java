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

import java.lang.reflect.Method;

/**
 * Verifies that Breakpoints can be created correctly when dealing with one or
 * more instrumented subclasses
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InstrumentedBreakpointSubclassTest extends InstrumentedBreakpointTest {


  @Override
  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleSubclass.class, SimpleClass.class);
  }

  @Override
  protected SimpleClass getTestObject() {
    return new SimpleSubclass();
  }

  /**
   * Sets a breakpoint at the point where SimpleSubclass calls a method in
   * SimpleClass.  (We use add(int, int), which is defined in SimpleClass and
   * overriden in SimpleSubclass.)
   */
  @ThreadedTest
  public void doBreakpoint_inOverriddenMethodCallingSuper() throws Exception {
    final SimpleSubclass simple = new SimpleSubclass();

    ObjectInstrumentation<SimpleSubclass> obj = Instrumentation.getObjectInstrumentation(simple);
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleSubclass.class);
    Method add = SimpleSubclass.class.getDeclaredMethod("add", int.class, int.class);
    Method superAdd = SimpleClass.class.getDeclaredMethod("add", int.class, int.class);
    CodePosition beforeCall = ic.beforeCall(add, superAdd);
    CodePosition afterCall = ic.afterCall(add, superAdd);
    TestThread testThread = new TestThread("test thread") {
      @Override
      public void run() {
        simple.add(1, 2);
      }
    };
    Breakpoint beforeBreakpoint = obj.createBreakpoint(beforeCall, testThread);
    Breakpoint afterBreakpoint = obj.createBreakpoint(afterCall, testThread);
    testThread.start();
    beforeBreakpoint.await();
    assertEquals(1, simple.subPosition);
    assertEquals(0, simple.position);
    beforeBreakpoint.resume(afterBreakpoint);
    afterBreakpoint.await();
    assertEquals(2, simple.position);
    afterBreakpoint.resume();
    testThread.finish();
    testThread.throwExceptionsIfAny();
  }

}
