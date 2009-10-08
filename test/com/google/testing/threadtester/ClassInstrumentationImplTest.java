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
 * Tests ClassInstrumentationImpl. Verifies that CodePositions can be
 * created correctly in a simple test class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ClassInstrumentationImplTest extends TestCase {

  private ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass.class, SimpleInteger.class);
  }

  @ThreadedTest
  public void getCodePosition_fromName() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    InstrumentedCodePosition cp = (InstrumentedCodePosition) ic.atMethodStart("unique");
    assertNotNull(cp);
  }

  @ThreadedTest
  public void getCodePosition_fromNameIsEqual() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    InstrumentedCodePosition cp = (InstrumentedCodePosition) ic.atMethodStart("unique");
    InstrumentedCodePosition cp2 = (InstrumentedCodePosition) ic.atMethodStart("unique");
    assertTrue(cp.matches(cp2));
  }

  @ThreadedTest
  public void getCodePosition_fromNameAndMethodIsEqual() throws Exception {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    InstrumentedCodePosition cp = (InstrumentedCodePosition) ic.atMethodStart("unique");
    InstrumentedCodePosition cp2 =
        (InstrumentedCodePosition) ic.atMethodStart(SimpleClass.class.getDeclaredMethod("unique"));
    assertTrue(cp.matches(cp2));
  }

  @ThreadedTest
  public void getCodePosition_fromNamesAreNotEqual() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    InstrumentedCodePosition cp = (InstrumentedCodePosition) ic.atMethodStart("unique");
    InstrumentedCodePosition cp2 = (InstrumentedCodePosition) ic.atMethodStart("unique2");
    assertFalse(cp.matches(cp2));
  }

  @ThreadedTest
  public void getCodePosition_startAndEndAreNotEqual() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    InstrumentedCodePosition cp = (InstrumentedCodePosition) ic.atMethodStart("unique");
    InstrumentedCodePosition cp2 = (InstrumentedCodePosition) ic.atMethodEnd("unique");
    assertFalse(cp.matches(cp2));
  }

  @ThreadedTest
  public CodePosition getCodePosition_fromMethod() throws Exception {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp =
        ic.atMethodStart(SimpleClass.class.getDeclaredMethod("add", Integer.class, Integer.class));
    return cp;
  }

  @ThreadedTest (expected = IllegalArgumentException.class)
  public void getCodePosition_fromInvalidMethod() throws Exception {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp = ic.atMethodStart(Object.class.getDeclaredMethod("toString"));
  }

  @ThreadedTest (expected = IllegalArgumentException.class)
  public void getCodePosition_duplicate() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp = ic.atMethodStart("add");
  }

  @ThreadedTest (expected = IllegalArgumentException.class)
  public void getCodePosition_nonExistent() {
    ClassInstrumentation ic = Instrumentation.getClassInstrumentation(SimpleClass.class);
    CodePosition cp = ic.atMethodStart("noSuchMethod");
  }
}
