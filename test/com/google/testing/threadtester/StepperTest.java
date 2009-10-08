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
 * Verifies that a Stepper can step correctly through instrumented code.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class StepperTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass.class, SimpleClass2.class);
  }

  private void stepToPosition(SimpleClass testObject, Stepper stepper, int position)
      throws TestTimeoutException {
    System.out.printf("-============= stepping from position %d\n", testObject.position);

    assertTrue(stepper.hasNext());
    stepper.step();
    System.out.printf("Stepped - position = %d\n======================\n", testObject.position);

    assertEquals(position, testObject.position);
  }

  //@ThreadedTest
  public void stepThroughSingleMethod() throws Exception {

    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp =
        ic.atMethodStart(SimpleClass.class.getDeclaredMethod("add", int.class, int.class));
    final SimpleClass testObject = new SimpleClass();
    final ObjectInstrumentation<SimpleClass> instrumented =
        Instrumentation.getObjectInstrumentation(testObject);
    TestThread thread = new TestThread("Stepper Test") {
        @Override
        public void run() {
          testObject.add(1, 2);
        }
      };
    Breakpoint startPoint = instrumented.createBreakpoint(cp, thread);
    thread.start();
    startPoint.await();
    assertEquals(0, testObject.position);

    Stepper stepper = instrumented.step(startPoint);

    // The stepper will step to the beginning of each line in the method. We
    // expect four such steps, with the position set to 0, 1, 2, 2. (The last
    // two lines in the method don't increment the position.) It will then step
    // to the end of the method, for a total of 5 steps.
    stepToPosition(testObject, stepper, 0);
    stepToPosition(testObject, stepper, 1);
    stepToPosition(testObject, stepper, 2);
    stepToPosition(testObject, stepper, 2);
    stepToPosition(testObject, stepper, 2);

    assertFalse(stepper.hasNext());
    stepper.resume();
    thread.join();
    thread.throwExceptionsIfAny();
  }

  //@ThreadedTest
  public void stepFromEndOfSingleMethod() throws Exception {

    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp =
        ic.atMethodEnd(SimpleClass.class.getDeclaredMethod("add", int.class, int.class));
    final SimpleClass testObject = new SimpleClass();
    final ObjectInstrumentation<SimpleClass> instrumented =
        Instrumentation.getObjectInstrumentation(testObject);
    TestThread thread = new TestThread("Stepper Test") {
        @Override
        public void run() {
          testObject.add(1, 2);
        }
      };
    Breakpoint endPoint = instrumented.createBreakpoint(cp, thread);
    thread.start();
    endPoint.await();
    assertEquals(2, testObject.position);

    Stepper stepper = instrumented.step(endPoint);
    // We have stopped at the end of the method. We should have no further lines
    // to step through.
    assertFalse(stepper.hasNext());
    stepper.resume();
    thread.join();
    thread.throwExceptionsIfAny();
  }


  //@ThreadedTest
  public void stepFromMiddleOfNestedMethod() throws Exception {

    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    Method outerMethod =  SimpleClass.class.getDeclaredMethod("unique");
    Method innerMethod = SimpleClass.class.getDeclaredMethod("innerMethod");
    CodePosition cp = ic.beforeCall(outerMethod, innerMethod);
    final SimpleClass testObject = new SimpleClass();
    final ObjectInstrumentation<SimpleClass> instrumented =
        Instrumentation.getObjectInstrumentation(testObject);
    TestThread thread = new TestThread("Stepper Test") {
        @Override
        public void run() {
          testObject.unique();
        }
      };
    Breakpoint startPoint = instrumented.createBreakpoint(cp, thread);
    thread.start();
    startPoint.await();

    // We should be just befopre the call to innerMethod()
    assertEquals(7, testObject.position);

    Stepper stepper = instrumented.step(startPoint);

    // We should step to the beginning of the innerMethod, and then to the first
    // line. Position will still be 7. Then we step to the last line of
    // innerMethod, where position is now 11, and then to the end of the
    // innerMethod. (Note that for a void method, there is a valid line number
    // at the end of the method, corresponding to the return statement..)
    stepToPosition(testObject, stepper, 7);
    stepToPosition(testObject, stepper, 7);
    stepToPosition(testObject, stepper, 11);
    stepToPosition(testObject, stepper, 11);
    // We step to the end of the call to the innerMethod, and then to the
    // beginning of the next line. Position is still 11.
    stepToPosition(testObject, stepper, 11);
    stepToPosition(testObject, stepper, 11);
    // We step over the next line (which sets position to 8), and then to the
    // return from the top method.
    stepToPosition(testObject, stepper, 8);
    stepToPosition(testObject, stepper, 8);
    // We should now be at the end of the top method. We cannot step any
    // further, as we are now returning from a call into instrumented code.
    assertFalse(stepper.hasNext());
    stepper.resume();
    thread.join();
    thread.throwExceptionsIfAny();
  }



  @ThreadedTest
  public void stepFromMiddleOfNestedMethodInOtherClass() throws Exception {

    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass2.class);
    Method innerMethod =
        SimpleClass2.class.getDeclaredMethod("setPosition", SimpleClass.class, int.class);
    CodePosition cp = ic.atMethodStart(innerMethod);
    CodePosition cp2 = ic.atMethodEnd(innerMethod);
    final SimpleClass testObject = new SimpleClass();
    final SimpleClass2 innerObject = new SimpleClass2();
    testObject.setSecond(innerObject);

    final ObjectInstrumentation<SimpleClass2> instrumented =
        Instrumentation.getObjectInstrumentation(innerObject);
    TestThread thread = new TestThread("Stepper Test") {
        @Override
        public void run() {
          testObject.callSecond();
        }
      };

    // Create a second thread, which will call setPosition() directly.
    TestThread thread2 = new TestThread("Stepper Test 2") {
        @Override
        public void run() {
          innerObject.setPosition(testObject, -1);
        }
      };

    Breakpoint startPoint = instrumented.createBreakpoint(cp, thread);

    // Create a breakPoint in the second thread, and wait for it. This helps to
    // test the behaviour of the steppper when two different threads are
    // accessing the same object, and verifies that the stepper does not get
    // confused.
    Breakpoint secondPoint = instrumented.createBreakpoint(cp2, thread2);
    thread2.start();
    secondPoint.await();
    assertEquals(-1, testObject.position);

    thread.start();
    startPoint.await();


    // We should be at the start of the call to setPosition(). The value of
    // position will have been set to 12 in SimpleClass.callSecond().
    assertEquals(12, testObject.position);

    Stepper stepper = instrumented.step(startPoint);
    // We should step to the beginning of the first line in setPosition()
    stepToPosition(testObject, stepper, 12);

    // We should step to the final line (at which point position is updated to
    // 13) and then to the end of the method.
    stepToPosition(testObject, stepper, 13);
    stepToPosition(testObject, stepper, 13);

    // We should step back up into SimpleClass.callSecond(), at the end of the
    // call to setPosition().
    stepToPosition(testObject, stepper, 13);

    // We should step to the beginning of the next line in callSecond(), then to
    // the return line, and then to the end of the method.
    stepToPosition(testObject, stepper, 13);
    stepToPosition(testObject, stepper, 14);
    stepToPosition(testObject, stepper, 14);

    assertFalse(stepper.hasNext());
    stepper.resume();
    thread.join();
    secondPoint.resume();
    thread2.join();
    thread.throwExceptionsIfAny();
  }
}
