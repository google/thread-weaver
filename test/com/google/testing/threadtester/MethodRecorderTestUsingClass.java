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
 * Test that verifies that we can create a MethodRecorder using a
 * Class object instead of an instance, and create CodePositions
 * correctly.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class MethodRecorderTestUsingClass extends TestCase {

  private ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.runTests(getClass(), SimpleClass.class, SimpleInteger.class);
  }

  private MethodRecorder<SimpleClass> recorder;
  private SimpleClass control;
  private ClassInstrumentation ic;

  @ThreadedBefore
  public void before() {
    SimpleClass instance = new SimpleClass();
    recorder = new MethodRecorder<SimpleClass>(SimpleClass.class);
    control = recorder.getControl();
    ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
  }

  @ThreadedTest
  public void getPosition_atStartOfLastMethod() throws Exception {
    control.add(0, 0);
    CodePosition cp = recorder.atStartOfLastMethod().position();
    CodePosition verification =
        ic.atMethodStart(SimpleClass.class.getDeclaredMethod("add", int.class, int.class));
    assertTrue(verification.matches(cp));
  }

  @ThreadedTest
  public void getPosition_atEndOfLastMethod() throws Exception {
    control.add(0, 0);
    CodePosition cp = recorder.atEndOfLastMethod().position();
    CodePosition verification =
        ic.atMethodEnd(SimpleClass.class.getDeclaredMethod("add", int.class, int.class));
    assertTrue(verification.matches(cp));
  }

  @ThreadedTest
  public void getPosition_beforeCall() throws Exception {
    SimpleClass2 target = recorder.createTarget(SimpleClass2.class);
    control.add(0, 0);
    CodePosition cp = recorder.in(control.callSecond()).
        beforeCalling(target.setPosition(null, 0)).position();
    CodePosition verification = ic.beforeCall(
        SimpleClass.class.getDeclaredMethod("callSecond"),
        SimpleClass2.class.getDeclaredMethod("setPosition", SimpleClass.class, int.class));
    assertTrue(verification.matches(cp));
  }

  @ThreadedTest
  public void getPosition_afterCall() throws Exception {
    SimpleClass2 target = recorder.createTarget(SimpleClass2.class);
    control.add(0, 0);
    CodePosition cp = recorder.in(control.callSecond()).
        afterCalling(target.setPosition(null, 0)).position();
    CodePosition verification = ic.afterCall(
        SimpleClass.class.getDeclaredMethod("callSecond"),
        SimpleClass2.class.getDeclaredMethod("setPosition", SimpleClass.class, int.class));
    assertTrue(verification.matches(cp));
  }

  @ThreadedTest
  public void getPosition_atStart() throws Exception {
    final SimpleInteger value = new SimpleInteger(0);
    CodePosition cp = recorder.atStartOf(control.add(value, value)).position();
    CodePosition verification = ic.atMethodStart(
        SimpleClass.class.getDeclaredMethod("add", SimpleInteger.class, SimpleInteger.class));
    assertTrue(verification.matches(cp));
  }

  @ThreadedTest
  public void getPosition_atEnd() throws Exception {
    final SimpleInteger value = new SimpleInteger(0);
    CodePosition cp = recorder.atEndOf(control.add(value, value)).position();
    CodePosition verification = ic.atMethodEnd(
        SimpleClass.class.getDeclaredMethod("add", SimpleInteger.class, SimpleInteger.class));
    assertTrue(verification.matches(cp));
  }

}
