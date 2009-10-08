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
import java.util.ArrayList;
import java.util.List;

/**
 * Tests that an InterleavedRunner can be used to interleave method calls.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InterleavedRunnerTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();
  private static final String MAIN_RUNNER = "Main ";
  private static final String SECOND_RUNNER = "Second ";

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.runTests(getClass(), SimpleClass5.class, SimpleInteger.class);
  }

  private abstract class SimpleMainRunnable extends MainRunnableImpl<SimpleClass5> {
    protected SimpleClass5 simple;
    @Override
    public Class<SimpleClass5> getClassUnderTest() {
      return SimpleClass5.class;
    }

    @Override
    public void initialize() {
      simple = new SimpleClass5();
    }

    @Override
    public SimpleClass5 getMainObject() {
      return simple;
    }
  }

  private abstract class SimpleSecondaryRunnable extends
      SecondaryRunnableImpl<SimpleClass5, SimpleMainRunnable> {
    protected SimpleClass5 simple;

    @Override
    public void initialize(SimpleMainRunnable main) {
      simple = main.getMainObject();
    }
  }

  @ThreadedBefore
  public void resetSequence() {
    SimpleClass5.getAndResetSequence();
  }

  /**
   * Tests an InterleavedRunner calling a simple method. Verifies that the
   * method is interleaved in the expected order.
   */
  @ThreadedTest
  public void checkInterleavedRunner() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass5.class);
    SimpleMainRunnable main = new SimpleMainRunnable() {
        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass5.class.getDeclaredMethod("mainMethod", String.class);
        }

        @Override
        public void run() {
          simple.mainMethod(MAIN_RUNNER);
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.mainMethod(SECOND_RUNNER);
        }
      };
    RunResult result = InterleavedRunner.interleave(main, secondary);
    result.throwExceptionsIfAny();
    List<String> sequence = SimpleClass5.getAndResetSequence();

    // The InterleavedRunner will run the two runnables separate times, stopping
    // at a different line each time. There are three executable lines in the
    // target method, so we expect four different possible interleavings.

    // Main stops at first line.
    assertEquals(SECOND_RUNNER + "1", sequence.get(0));
    assertEquals(SECOND_RUNNER + "2", sequence.get(1));
    assertEquals(SECOND_RUNNER + "3", sequence.get(2));
    assertEquals(MAIN_RUNNER + "1", sequence.get(3));
    assertEquals(MAIN_RUNNER + "2", sequence.get(4));
    assertEquals(MAIN_RUNNER + "3", sequence.get(5));

    // Main stops at second line
    assertEquals(MAIN_RUNNER + "1", sequence.get(6));
    assertEquals(SECOND_RUNNER + "1", sequence.get(7));
    assertEquals(SECOND_RUNNER + "2", sequence.get(8));
    assertEquals(SECOND_RUNNER + "3", sequence.get(9));
    assertEquals(MAIN_RUNNER + "2", sequence.get(10));
    assertEquals(MAIN_RUNNER + "3", sequence.get(11));

    // Main stops at third line
    assertEquals(MAIN_RUNNER + "1", sequence.get(12));
    assertEquals(MAIN_RUNNER + "2", sequence.get(13));
    assertEquals(SECOND_RUNNER + "1", sequence.get(14));
    assertEquals(SECOND_RUNNER + "2", sequence.get(15));
    assertEquals(SECOND_RUNNER + "3", sequence.get(16));
    assertEquals(MAIN_RUNNER + "3", sequence.get(17));

    // Main stops at 4th line. (This line is the 'return 0' line.)
    assertEquals(MAIN_RUNNER + "1", sequence.get(18));
    assertEquals(MAIN_RUNNER + "2", sequence.get(19));
    assertEquals(MAIN_RUNNER + "3", sequence.get(20));
    assertEquals(SECOND_RUNNER + "1", sequence.get(21));
    assertEquals(SECOND_RUNNER + "2", sequence.get(22));
    assertEquals(SECOND_RUNNER + "3", sequence.get(23));
  }

  /**
   * Tests an InterleavedRunner calling a method that calls other methods in the
   * same class. Verifies that the method is interleaved in the expected order,
   * and that the test runnables only stop in the specifed test method (and not
   * in any other method invoked from that test method.)
   */
  @ThreadedTest
  public void checkInterleavedRunner_callingSecondMethod() {
    callSecondMethod(false);
  }
  @ThreadedTest
  public void checkInterleavedRunner_callingSecondMethodWithStartPosition() {
    callSecondMethod(true);
  }

  private void callSecondMethod(boolean createStartPosition) {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass5.class);
    SimpleMainRunnable main = new SimpleMainRunnable() {
        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass5.class.getDeclaredMethod("secondMethod", String.class);
        }

        @Override
        public void run() {
          simple.secondMethod(MAIN_RUNNER);
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.secondMethod(SECOND_RUNNER);
        }
      };
    RunResult result;
    if (createStartPosition) {
      CodePosition cp = instr.beforeCall("secondMethod", "fourthMethod");
      result = InterleavedRunner.interleaveAfter(main, secondary, cp, 0);
    } else {
      result = InterleavedRunner.interleave(main, secondary);
    }
    result.throwExceptionsIfAny();
    List<String> sequence = SimpleClass5.getAndResetSequence();

    // Slightly more complex expectations, as the method under test has 6 lines.
    // Note that some sequences are the same, as not every line is calling a
    // method that increments the sequence number.

    if (createStartPosition) {
      // We expect to run until sequence point 5, just before the call to
      // fourthMethod. Then the second thread will run.  Note that some of the
      // sequences are identical, because when InterleavedRunner is asked to
      // start at a given position, it will still repeat the test for every line
      // in the target method. (Hence some runs will be identical.) This is
      // slightly inefficient, and if we ever chage this we need to update the
      // test expectation

      assertEquals(MAIN_RUNNER + "5", sequence.get(0));
      assertEquals(SECOND_RUNNER + "5", sequence.get(1));
      assertEquals(SECOND_RUNNER + "6", sequence.get(2));
      assertEquals(SECOND_RUNNER + "7", sequence.get(3));
      assertEquals(MAIN_RUNNER + "6", sequence.get(4));
      assertEquals(MAIN_RUNNER + "7", sequence.get(5));

      assertEquals(MAIN_RUNNER + "5", sequence.get(6));
      assertEquals(SECOND_RUNNER + "5", sequence.get(7));
      assertEquals(SECOND_RUNNER + "6", sequence.get(8));
      assertEquals(SECOND_RUNNER + "7", sequence.get(9));
      assertEquals(MAIN_RUNNER + "6", sequence.get(10));
      assertEquals(MAIN_RUNNER + "7", sequence.get(11));

      assertEquals(MAIN_RUNNER + "5", sequence.get(12));
      assertEquals(SECOND_RUNNER + "5", sequence.get(13));
      assertEquals(SECOND_RUNNER + "6", sequence.get(14));
      assertEquals(SECOND_RUNNER + "7", sequence.get(15));
      assertEquals(MAIN_RUNNER + "6", sequence.get(16));
      assertEquals(MAIN_RUNNER + "7", sequence.get(17));

      assertEquals(MAIN_RUNNER + "5", sequence.get(18));
      assertEquals(MAIN_RUNNER + "6", sequence.get(19));
      assertEquals(SECOND_RUNNER + "5", sequence.get(20));
      assertEquals(SECOND_RUNNER + "6", sequence.get(21));
      assertEquals(SECOND_RUNNER + "7", sequence.get(22));
      assertEquals(MAIN_RUNNER + "7", sequence.get(23));

      assertEquals(MAIN_RUNNER + "5", sequence.get(24));
      assertEquals(MAIN_RUNNER + "6", sequence.get(25));
      assertEquals(SECOND_RUNNER + "5", sequence.get(26));
      assertEquals(SECOND_RUNNER + "6", sequence.get(27));
      assertEquals(SECOND_RUNNER + "7", sequence.get(28));
      assertEquals(MAIN_RUNNER + "7", sequence.get(29));

      assertEquals(MAIN_RUNNER + "5", sequence.get(30));
      assertEquals(MAIN_RUNNER + "6", sequence.get(31));
      assertEquals(MAIN_RUNNER + "7", sequence.get(32));
      assertEquals(SECOND_RUNNER + "5", sequence.get(33));
      assertEquals(SECOND_RUNNER + "6", sequence.get(34));
      assertEquals(SECOND_RUNNER + "7", sequence.get(35));
    } else {
      // We expect the first thread to block at the beginning of the method, so
      // the second thread will run before the first thread updates the sequence
      // count. In subsequent runs, the main thread will run further.
      assertEquals(SECOND_RUNNER + "5", sequence.get(0));
      assertEquals(SECOND_RUNNER + "6", sequence.get(1));
      assertEquals(SECOND_RUNNER + "7", sequence.get(2));
      assertEquals(MAIN_RUNNER + "5", sequence.get(3));
      assertEquals(MAIN_RUNNER + "6", sequence.get(4));
      assertEquals(MAIN_RUNNER + "7", sequence.get(5));

      assertEquals(MAIN_RUNNER + "5", sequence.get(6));
      assertEquals(SECOND_RUNNER + "5", sequence.get(7));
      assertEquals(SECOND_RUNNER + "6", sequence.get(8));
      assertEquals(SECOND_RUNNER + "7", sequence.get(9));
      assertEquals(MAIN_RUNNER + "6", sequence.get(10));
      assertEquals(MAIN_RUNNER + "7", sequence.get(11));

      assertEquals(MAIN_RUNNER + "5", sequence.get(12));
      assertEquals(SECOND_RUNNER + "5", sequence.get(13));
      assertEquals(SECOND_RUNNER + "6", sequence.get(14));
      assertEquals(SECOND_RUNNER + "7", sequence.get(15));
      assertEquals(MAIN_RUNNER + "6", sequence.get(16));
      assertEquals(MAIN_RUNNER + "7", sequence.get(17));

      assertEquals(MAIN_RUNNER + "5", sequence.get(18));
      assertEquals(MAIN_RUNNER + "6", sequence.get(19));
      assertEquals(SECOND_RUNNER + "5", sequence.get(20));
      assertEquals(SECOND_RUNNER + "6", sequence.get(21));
      assertEquals(SECOND_RUNNER + "7", sequence.get(22));
      assertEquals(MAIN_RUNNER + "7", sequence.get(23));

      assertEquals(MAIN_RUNNER + "5", sequence.get(24));
      assertEquals(MAIN_RUNNER + "6", sequence.get(25));
      assertEquals(SECOND_RUNNER + "5", sequence.get(26));
      assertEquals(SECOND_RUNNER + "6", sequence.get(27));
      assertEquals(SECOND_RUNNER + "7", sequence.get(28));
      assertEquals(MAIN_RUNNER + "7", sequence.get(29));

      assertEquals(MAIN_RUNNER + "5", sequence.get(30));
      assertEquals(MAIN_RUNNER + "6", sequence.get(31));
      assertEquals(MAIN_RUNNER + "7", sequence.get(32));
      assertEquals(SECOND_RUNNER + "5", sequence.get(33));
      assertEquals(SECOND_RUNNER + "6", sequence.get(34));
      assertEquals(SECOND_RUNNER + "7", sequence.get(35));
    }
  }

  @ThreadedTest
  public void checkInterleaveAtCountedPosition() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass5.class);
    SimpleMainRunnable main = new SimpleMainRunnable() {
        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass5.class.getDeclaredMethod("thirdMethod", String.class);
        }

        @Override
        public void run() {
          simple.thirdMethod(MAIN_RUNNER);
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.thirdMethod(SECOND_RUNNER);
        }
      };
    CodePosition cp = instr.beforeCall("thirdMethod", "fourthMethod");
    RunResult result = InterleavedRunner.interleaveAfter(main, secondary, cp, 2);
    result.throwExceptionsIfAny();
    List<String> sequence = SimpleClass5.getAndResetSequence();

    // We expect to run the first thread until just before the second time we
    // call fourthMethod. (I.e. to sequence number 9.) Then we will start the
    // second thread. Because the interleaved runner will try one interleaving
    // for every line in the target method, we expect five duplicate patterns,
    // and the sixth final pattern where the first thread runs until the last
    // line.
    assertEquals(MAIN_RUNNER + "8", sequence.get(0));
    assertEquals(MAIN_RUNNER + "9", sequence.get(1));
    assertEquals(SECOND_RUNNER + "8", sequence.get(2));
    assertEquals(SECOND_RUNNER + "9", sequence.get(3));
    assertEquals(SECOND_RUNNER + "10", sequence.get(4));
    assertEquals(MAIN_RUNNER + "10", sequence.get(5));

    assertEquals(MAIN_RUNNER + "8", sequence.get(6));
    assertEquals(MAIN_RUNNER + "9", sequence.get(7));
    assertEquals(SECOND_RUNNER + "8", sequence.get(8));
    assertEquals(SECOND_RUNNER + "9", sequence.get(9));
    assertEquals(SECOND_RUNNER + "10", sequence.get(10));
    assertEquals(MAIN_RUNNER + "10", sequence.get(11));

    assertEquals(MAIN_RUNNER + "8", sequence.get(12));
    assertEquals(MAIN_RUNNER + "9", sequence.get(13));
    assertEquals(SECOND_RUNNER + "8", sequence.get(14));
    assertEquals(SECOND_RUNNER + "9", sequence.get(15));
    assertEquals(SECOND_RUNNER + "10", sequence.get(16));
    assertEquals(MAIN_RUNNER + "10", sequence.get(17));

    assertEquals(MAIN_RUNNER + "8", sequence.get(18));
    assertEquals(MAIN_RUNNER + "9", sequence.get(19));
    assertEquals(SECOND_RUNNER + "8", sequence.get(20));
    assertEquals(SECOND_RUNNER + "9", sequence.get(21));
    assertEquals(SECOND_RUNNER + "10", sequence.get(22));
    assertEquals(MAIN_RUNNER + "10", sequence.get(23));

    assertEquals(MAIN_RUNNER + "8", sequence.get(24));
    assertEquals(MAIN_RUNNER + "9", sequence.get(25));
    assertEquals(SECOND_RUNNER + "8", sequence.get(26));
    assertEquals(SECOND_RUNNER + "9", sequence.get(27));
    assertEquals(SECOND_RUNNER + "10", sequence.get(28));
    assertEquals(MAIN_RUNNER + "10", sequence.get(29));

    assertEquals(MAIN_RUNNER + "8", sequence.get(30));
    assertEquals(MAIN_RUNNER + "9", sequence.get(31));
    assertEquals(MAIN_RUNNER + "10", sequence.get(32));
    assertEquals(SECOND_RUNNER + "8", sequence.get(33));
    assertEquals(SECOND_RUNNER + "9", sequence.get(34));
    assertEquals(SECOND_RUNNER + "10", sequence.get(35));

  }

  @ThreadedTest
  public void checkInterleaveAtPositions() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass5.class);
    SimpleMainRunnable main = new SimpleMainRunnable() {
        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass5.class.getDeclaredMethod("secondMethod", String.class);
        }

        @Override
        public void run() {
          simple.secondMethod(MAIN_RUNNER);
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.secondMethod(SECOND_RUNNER);
        }
      };
    List<CodePosition> positions = new ArrayList<CodePosition>();
    positions.add(instr.beforeCall("secondMethod", "fourthMethod"));
    positions.add(instr.afterCall("secondMethod", "fifthMethod"));
    RunResult result = InterleavedRunner.interleave(main, secondary, positions);

    result.throwExceptionsIfAny();
    List<String> sequence = SimpleClass5.getAndResetSequence();
    // We expect two runs, one with the main thread stopping just before calling
    // fourthMethod, and one stopping just beofre calling fifthMethod.

    assertEquals(MAIN_RUNNER + "5", sequence.get(0));
    assertEquals(SECOND_RUNNER + "5", sequence.get(1));
    assertEquals(SECOND_RUNNER + "6", sequence.get(2));
    assertEquals(SECOND_RUNNER + "7", sequence.get(3));
    assertEquals(MAIN_RUNNER + "6", sequence.get(4));
    assertEquals(MAIN_RUNNER + "7", sequence.get(5));

    assertEquals(MAIN_RUNNER + "5", sequence.get(6));
    assertEquals(MAIN_RUNNER + "6", sequence.get(7));
    assertEquals(SECOND_RUNNER + "5", sequence.get(8));
    assertEquals(SECOND_RUNNER + "6", sequence.get(9));
    assertEquals(SECOND_RUNNER + "7", sequence.get(10));
    assertEquals(MAIN_RUNNER + "7", sequence.get(11));
  }

  @ThreadedTest
  public void checkRecursive() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass5.class);
    SimpleMainRunnable main = new SimpleMainRunnable() {
        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass5.class.getDeclaredMethod("recursive", String.class, int.class);
        }

        @Override
        public void run() {
          simple.recursive(MAIN_RUNNER, 2);
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.recursive(SECOND_RUNNER, 2);
        }
      };
    RunResult result = InterleavedRunner.interleave(main, secondary);
    result.throwExceptionsIfAny();
    List<String> sequence = SimpleClass5.getAndResetSequence();
    for (int i = 0; i < sequence.size(); i++) {
      System.out.printf("assertEquals(\"%s\", sequence.get(%d));\n", sequence.get(i), i);
    }

    /*
Second 22
Second 21
Second 20
Main 22
Main 21
Main 20

Main 22
Second 22
Second 21
Second 20
Main 21
Main 20

Main 22
Second 22
Main 21
Main 20
Second 21
Second 20

Main 22
Second 22
Main 21
Main 20
Second 21
Second 20

Main 22
Main 21
Main 20
Second 22
Second 21
Second 20
    */











  }
}
