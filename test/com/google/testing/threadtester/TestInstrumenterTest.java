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
import java.lang.reflect.Method;
import java.util.List;

/**
 * Tests for TestInstrumenter. Note that this is not strictly a Unit Test, as we
 * need to load up a test class, and instrument it via the TestInstrumenter. We
 * thus depend on the ThreadedTestRunner framework.
 * <p>

 * The test works by injecting a variant CallLoggerFactory into the
 * TestInstrumenter, which creates DummyCallLoggers for the instrumented
 * objects.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class TestInstrumenterTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    String factory =  TestInstrumenter.FACTORY_CLASS;
    try {
      // Set the instrumenter to use a Test factory
      TestInstrumenter.FACTORY_CLASS = FakeLoggerFactory.class.getName();
      runner.runTests(getClass(), InstrumenterTestClass.class, InstrumenterTestSubclass.class);
    } finally {
      TestInstrumenter.FACTORY_CLASS = factory;
    }
  }

  @ThreadedTest
  public void constructors() throws Exception {
    // The default constructor calls the String constructor with an empty
    // value. Hence we expect 2 calls to the TestLoggerFactory
    InstrumenterTestClass tc = new InstrumenterTestClass();
    assertEquals(Integer.valueOf(2), FakeLoggerFactory.getNumCalls(tc));

    // The String constructor calls no others, so we expect only one call.
    tc = new InstrumenterTestClass("Hello");
    assertEquals(Integer.valueOf(1), FakeLoggerFactory.getNumCalls(tc));
  }

  @ThreadedTest
  public void stringArg() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    tc.stringArg(new String[][] {{"a", "b"},{"c", "d"}});

    Method stringArg = InstrumenterTestClass.class.getDeclaredMethod(
        "stringArg", Class.forName("[[Ljava.lang.String;"));
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(stringArg, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.END, records.get(2).type);
    assertEquals(stringArg, records.get(2).caller);
  }

  @ThreadedTest
  public void intArg() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    tc.intArg(new int[] {0, 1});

    Method intArg = InstrumenterTestClass.class.getDeclaredMethod(
        "intArg", Class.forName("[I"));
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(intArg, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.END, records.get(2).type);
    assertEquals(intArg, records.get(2).caller);
  }

  @ThreadedTest
  public void throwException() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    try {
      tc.throwException();
      fail("Did not throw");
    } catch (IOException e) {
    }

    // Even though the code threw an exception, we still expect the end of the
    // call to be logged.
    Method throwException = InstrumenterTestClass.class.getDeclaredMethod("throwException");
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(throwException, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.END, records.get(2).type);
    assertEquals(throwException, records.get(2).caller);
  }

  @ThreadedTest
  public void throwRuntimeException() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    try {
      tc.throwRuntimeException();
      fail("Did not throw");
    } catch (IllegalArgumentException e) {
    }

    // Even though the code threw an exception, we still expect the end of the
    // call to be logged.
    Method throwRuntimeException =
        InstrumenterTestClass.class.getDeclaredMethod("throwRuntimeException");
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(throwRuntimeException, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.END, records.get(2).type);
    assertEquals(throwRuntimeException, records.get(2).caller);
  }

  @ThreadedTest
  public void callVarArgs() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    tc.callVarArgs();

    // Slightly more complex expectations, as the callVarArgs method internally
    // calls another method.
    Method callVarArgs = InstrumenterTestClass.class.getDeclaredMethod("callVarArgs");
    Method varArgs = InstrumenterTestClass.class.getDeclaredMethod(
        "varArgs", Class.forName("[Ljava.lang.String;"));
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(callVarArgs, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.BEGIN_CALL, records.get(2).type);
    assertEquals(callVarArgs, records.get(2).caller);
    assertEquals(varArgs, records.get(2).called);

    assertEquals(FakeLogger.Type.START, records.get(3).type);
    assertEquals(varArgs, records.get(3).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(4).type);
    assertEquals(FakeLogger.Type.END, records.get(5).type);
    assertEquals(varArgs, records.get(5).caller);

    assertEquals(FakeLogger.Type.END_CALL, records.get(6).type);
    assertEquals(callVarArgs, records.get(6).caller);
    assertEquals(varArgs, records.get(6).called);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(7).type);
    assertEquals(FakeLogger.Type.END, records.get(8).type);
    assertEquals(callVarArgs, records.get(8).caller);
  }

  @ThreadedTest
  public void callOverloaded_WithString() throws Exception {
    InstrumenterTestClass tc = new InstrumenterTestClass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    tc.overloaded("a");

    Method overloaded =
        InstrumenterTestClass.class.getDeclaredMethod("overloaded", String.class);
    List<FakeLogger.Record> records = logger.getRecords();
    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(overloaded, records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.END, records.get(2).type);
    assertEquals(overloaded, records.get(2).caller);
  }

  @ThreadedTest
  public void callOverloaded_inSubclass() throws Exception {
    InstrumenterTestSubclass tc = new InstrumenterTestSubclass();
    FakeLogger logger = FakeLoggerFactory.getLogger(tc);
    tc.overloaded("a");

    Method overloaded =
        InstrumenterTestClass.class.getDeclaredMethod("overloaded", String.class);
    Method overloadedSublass =
        InstrumenterTestSubclass.class.getDeclaredMethod("overloaded", String.class);

    List<FakeLogger.Record> records = logger.getRecords();

    // We expect to log the call to InstrumenterTestSubclass.overloaded(), which
    // should then call InstrumenterTestClass.overloaded()

    assertEquals(FakeLogger.Type.START, records.get(0).type);
    assertEquals(overloadedSublass,records.get(0).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(1).type);
    assertEquals(FakeLogger.Type.BEGIN_CALL, records.get(2).type);
    assertEquals(overloadedSublass, records.get(2).caller);
    assertEquals(overloaded, records.get(2).called);

    assertEquals(FakeLogger.Type.START, records.get(3).type);
    assertEquals(overloaded, records.get(3).caller);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(4).type);
    assertEquals(FakeLogger.Type.END, records.get(5).type);
    assertEquals(overloaded, records.get(5).caller);

    assertEquals(FakeLogger.Type.END_CALL, records.get(6).type);
    assertEquals(overloadedSublass, records.get(6).caller);
    assertEquals(overloaded, records.get(6).called);
    assertEquals(FakeLogger.Type.AT_LINE, records.get(7).type);
    assertEquals(FakeLogger.Type.END, records.get(8).type);
    assertEquals(overloadedSublass, records.get(8).caller);
  }
}
