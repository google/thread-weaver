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
 * Verifies that Breakpoints can be created correctly when dealing with a sub-subclass
 * of an instrumented class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InstrumentedBreakpointSubSubclassTest extends InstrumentedBreakpointTest {

  @Override
  public void testThreadedTests() {
    runner.setDebug(true);
    // Note that we are instrumenting SimpleSubSubclass and SimpleClass, but not
    // SimpleSubclass
    runner.runTests(getClass(), SimpleSubSubclass.class, SimpleClass.class);
  }

  @Override
  protected SimpleClass getTestObject() {
    return new SimpleSubSubclass();
  }

  /**
   * Sets a breakpoint at the point where SimpleSubSubclass calls a method in
   * SimpleClass.  (We use add(Integer, Integer), which is defined in
   * SimpleClass and overriden in SimpleSubSubclass, but not in SimpleSubclass.)
   */
  @ThreadedTest
  public void doBreakpoint_inOverriddenMethodCallingSuperSuper() throws Exception {
    final SimpleSubSubclass simple = new SimpleSubSubclass();

    ObjectInstrumentation<SimpleSubSubclass> obj = Instrumentation.getObjectInstrumentation(simple);
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleSubSubclass.class);
    Method add = SimpleSubSubclass.class.getDeclaredMethod("add", Integer.class, Integer.class);
    Method superAdd = SimpleClass.class.getDeclaredMethod("add", Integer.class, Integer.class);
    CodePosition beforeCall = ic.beforeCall(add, superAdd);
    CodePosition afterCall = ic.afterCall(add, superAdd);
    TestThread testThread = new TestThread("test thread") {
      @Override
      public void run() {
        simple.add(Integer.valueOf(1), Integer.valueOf(2));
      }
    };
    Breakpoint beforeBreakpoint = obj.createBreakpoint(beforeCall, testThread);
    Breakpoint afterBreakpoint = obj.createBreakpoint(afterCall, testThread);
    testThread.start();
    beforeBreakpoint.await();
    assertEquals(1, simple.subSubPosition);
    assertEquals(0, simple.position);
    beforeBreakpoint.resume(afterBreakpoint);
    afterBreakpoint.await();
    assertEquals(4, simple.position);
    afterBreakpoint.resume();
    testThread.finish();
    testThread.throwExceptionsIfAny();
  }
}
