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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.EnumSet;

/**
 * Tests exception handling in an InterleavedRunner
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InterleavedRunnerExceptionTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass.class);
  }

  private enum WhereToThrow {
    INIT,
    DURING_CALL,
    DURING_INSTRUMENTED,
    TERMINATE
  }

  private enum WhatToThrow {
    EXCEPTION,
    RUNTIME_EXCEPTION,
    ERROR
  }

  /**
   * Throws an error or exception, according to the parameter. Note that
   * the exception types here should match the types thrown in SimpleClass.
   */
  private void throwSomething(WhatToThrow what) throws Exception {
    switch (what) {
      case EXCEPTION:
        throw new IOException();
      case RUNTIME_EXCEPTION:
        throw new IllegalArgumentException();
      case ERROR:
        throw new AssertionFailedError();
      default:
        new Throwable("Unknown type " + what).printStackTrace();
    }
  }

  void checkForThrowDuringRun(SimpleClass simple, WhereToThrow whereInRun, WhatToThrow whatInRun) throws Exception {
    if (whereInRun == WhereToThrow.DURING_INSTRUMENTED) {
      switch (whatInRun) {
        case EXCEPTION:
          simple.throwException();
          break;
        case RUNTIME_EXCEPTION:
          simple.throwRuntimeException();
          break;
        case ERROR:
          simple.throwError();
          break;
        default:
          throw new RuntimeException("Unknown type " + whatInRun);
      }
    } else if (whereInRun == WhereToThrow.DURING_CALL) {
      throwSomething(whatInRun);
    }
  }

  private class SimpleMainRunnable extends  MainRunnableImpl<SimpleClass> {
    protected SimpleClass simple;

    @Override
    public Class<SimpleClass> getClassUnderTest() {
      return SimpleClass.class;
    }

    @Override
    public SimpleClass getMainObject() {
      return simple;
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
      return SimpleClass.class.getDeclaredMethod("nonBlocking");
    }

    @Override
    public void initialize() throws Exception {
      simple = new SimpleClass();
    }

    @Override
    public void run() throws Exception {
      simple.nonBlocking();
    }
  }

  private class ThrowingMainRunnable extends SimpleMainRunnable {
    private final WhereToThrow where;
    private final WhatToThrow what;

    ThrowingMainRunnable(WhereToThrow where, WhatToThrow what) {
      this.where = where;
      this.what = what;
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
      if (where == WhereToThrow.DURING_INSTRUMENTED) {
        switch (what) {
          case EXCEPTION:
            return SimpleClass.class.getDeclaredMethod("throwException");
          case RUNTIME_EXCEPTION:
            return SimpleClass.class.getDeclaredMethod("throwRuntimeException");
          case ERROR:
            return SimpleClass.class.getDeclaredMethod("throwError");
          default:
            throw new RuntimeException("Unknown type " + what);
        }
      } else {
        return super.getMethod();
      }
    }

    @Override
    public void initialize() throws Exception {
      if (where == WhereToThrow.INIT) {
        throwSomething(what);
      }
      super.initialize();
    }

    @Override
    public void run() throws Exception {
      checkForThrowDuringRun(simple, where, what);
      super.run();
    }

    @Override
    public void terminate() throws Exception {
      if (where == WhereToThrow.TERMINATE) {
        throwSomething(what);
      }
    }
  }

  private class SimpleSecondaryRunnable extends
      SecondaryRunnableImpl<SimpleClass, SimpleMainRunnable> {
    protected SimpleClass simple;

    @Override
    public void initialize(SimpleMainRunnable main) throws Exception {
      simple = main.getMainObject();
    }

    @Override
    public void run() throws Exception {
      simple.nonBlocking();
    }
  }


  private class ThrowingSecondaryRunnable extends SimpleSecondaryRunnable {

    private final WhereToThrow where;
    private final WhatToThrow what;
    ThrowingSecondaryRunnable(WhereToThrow where, WhatToThrow what) {
      this.where = where;
      this.what = what;
    }

    @Override
    public void initialize(SimpleMainRunnable main) throws Exception {
      if (where == WhereToThrow.INIT) {
        throwSomething(what);
      }
      super.initialize(main);
    }

    @Override
    public void run() throws Exception {
      checkForThrowDuringRun(simple, where, what);
      simple.nonBlocking();
    }

    @Override
    public void terminate() throws Exception {
      if (where == WhereToThrow.TERMINATE) {
        throwSomething(what);
      }
    }
  }

  /**
   * Tests an InterleavedRunner where the main runnable throws an error or
   * exception.
   */
  @ThreadedTest
  public void checkInterleavedRunnerMainThrows() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    for (WhereToThrow where : EnumSet.allOf(WhereToThrow.class)) {
      for (WhatToThrow what : EnumSet.allOf(WhatToThrow.class)) {
        runMain(instr, where, what);
      }
    }
  }

  private void runMain(ClassInstrumentation inst, WhereToThrow where, WhatToThrow what) {
    String message = "Throw " + what + ", at " + where;
    System.out.printf("runMain %s\n", message);
    if (what == WhatToThrow.ERROR && where == WhereToThrow.DURING_CALL) {
      System.out.printf("Gonna fail?\n");
    }
    ThrowingMainRunnable main = new ThrowingMainRunnable(where, what);
    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable();
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable mainException = result.getMainException();
    System.out.printf("Main exception = %s\n", mainException);
    assertNotNull(message, mainException);
    try {
      throwSomething(what);
    } catch (Throwable t) {
      assertEquals(message, t.getClass(), mainException.getClass());
    }
    assertNull(message, result.getSecondaryException());
  }

  /**
   * Tests an InterleavedRunner where the secondary runnable throws an error or
   * exception.
   */
  @ThreadedTest
  public void checkInterleavedRunnerSecondaryThrows() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    for (WhereToThrow where : EnumSet.allOf(WhereToThrow.class)) {
      for (WhatToThrow what : EnumSet.allOf(WhatToThrow.class)) {
        if (what == WhatToThrow.ERROR) {
          continue;
        }
        runSecondary(instr, where, what);
      }
    }
  }

  private void runSecondary(ClassInstrumentation inst, WhereToThrow where, WhatToThrow what) {
    String message = "Throw " + what + ", at " + where;
    SimpleMainRunnable main = new SimpleMainRunnable();
    ThrowingSecondaryRunnable secondary = new ThrowingSecondaryRunnable(where, what);
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable secondaryException = result.getSecondaryException();
    assertNotNull(message, secondaryException);
    try {
      throwSomething(what);
    } catch (Throwable t) {
      assertEquals(message, t.getClass(), secondaryException.getClass());
    }
    assertNull(message, result.getMainException());
  }
}
