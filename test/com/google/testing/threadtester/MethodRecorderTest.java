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

/**
 * Tests that breakpoints can be created in an instance of SimpleClass
 * using a MethodRecorder. For each breakpoint, we run a thread until
 * the breakpoint has been reached, and then verify the internal state
 * of the SimpleClass instance. See {@link SimpleClass#position}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class MethodRecorderTest extends TestCase {

  private ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass.class, SimpleInteger.class);
  }

  private SimpleClass simple;
  private MethodRecorder<SimpleClass> recorder;
  private SimpleClass control;
  private Thread thread;
  private Breakpoint breakpoint;

  @ThreadedBefore
  public void before() {
    simple = new SimpleClass();
    SimpleClass2 second = new SimpleClass2();
    simple.setSecond(second);
    recorder = new MethodRecorder<SimpleClass>(simple);
    control = recorder.getControl();
  }

  @ThreadedAfter
  public void after() throws InterruptedException {
    if (thread != null) {
      breakpoint.resume();
      thread.join();
    }
  }

  @ThreadedTest
  public void getBreakpoint_atStartOfLastMethod() throws TestTimeoutException {
    thread = new Thread() {
      @Override
      public void run() {
        simple.add(1, 2);
      }
    };
    control.add(0, 0);
    breakpoint = recorder.atStartOfLastMethod().breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(0, simple.position);
  }

  @ThreadedTest
  public void getBreakpoint_atEndOfLastMethod() throws TestTimeoutException {
    thread = new Thread() {
      @Override
      public void run() {
        simple.add(1, 2);
      }
    };
    control.add(0, 0);
    breakpoint = recorder.atEndOfLastMethod().breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(2, simple.position);
  }

  @ThreadedTest
  public void getBreakpoint_beforeCall() throws TestTimeoutException {
    thread = new Thread() {
      @Override
      public void run() {
        simple.callSecond();
      }
    };
    SimpleClass2 target = recorder.createTarget(SimpleClass2.class);
    control.add(0, 0);
    breakpoint = recorder.in(control.callSecond()).
        beforeCalling(target.setPosition(null, 0)).breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(12, simple.position);
  }

  @ThreadedTest
  public void getBreakpoint_afterCall() throws TestTimeoutException {
    thread = new Thread() {
      @Override
      public void run() {
        simple.callSecond();
      }
    };
    SimpleClass2 target = recorder.createTarget(SimpleClass2.class);
    control.add(0, 0);
    breakpoint = recorder.in(control.callSecond()).
        afterCalling(target.setPosition(null, 0)).breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(13, simple.position);
  }

  @ThreadedTest
  public void getBreakpoint_atStart() throws TestTimeoutException {
    final SimpleInteger value = new SimpleInteger(0);
    thread = new Thread() {
      @Override
      public void run() {
        simple.add(value, value);
      }
    };
    breakpoint = recorder.atStartOf(control.add(value, value)).breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(0, simple.position);
  }

  @ThreadedTest
  public void getBreakpoint_atEnd() throws TestTimeoutException {
    final SimpleInteger value = new SimpleInteger(0);
    thread = new Thread() {
      @Override
      public void run() {
        simple.add(value, value);
      }
    };
    breakpoint = recorder.atEndOf(control.add(value, value)).breakpoint(thread);
    thread.start();
    breakpoint.await();
    assertEquals(6, simple.position);
  }
}
