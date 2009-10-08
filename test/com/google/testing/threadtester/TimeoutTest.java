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
/**
 * Tests that the BaseThreadedTestRunner timeout options are handled correctly.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class TimeoutTest extends TestCase {

  private static final long TIMEOUT = 100;
  private static final long FACTOR = 4;

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    runner.setTimeout(TIMEOUT);
    runner.runTests(getClass(), SimpleClass.class);
  }

  private abstract class SimpleMainRunnable extends MainRunnableImpl<SimpleClass> {
    protected SimpleClass simple;
    @Override
    public Class<SimpleClass> getClassUnderTest() {
      return SimpleClass.class;
    }

    @Override
    public void initialize() {
      simple = new SimpleClass();
    }

    @Override
    public SimpleClass getMainObject() {
      return simple;
    }
  }

  private class SimpleSecondaryRunnable extends
      SecondaryRunnableImpl<SimpleClass, SimpleMainRunnable> {
    protected SimpleClass simple;

    @Override
    public void initialize(SimpleMainRunnable main) {
      simple = main.getMainObject();
    }

    @Override
    public void run() throws Exception {
      simple.nonBlocking();
    }
  }

  private enum BlockMode {
    BEFORE_CALL,
    DURING_CALL,
    AFTER_CALL
  }

  private class BlockingMainRunnable extends SimpleMainRunnable {
    private final BlockMode mode;

    BlockingMainRunnable(BlockMode mode) {
      this.mode = mode;
    }

    @Override
    public Method getMethod() throws NoSuchMethodException {
      if (mode == BlockMode.DURING_CALL) {
        return SimpleClass.class.getDeclaredMethod("blocking");
      } else {
        return SimpleClass.class.getDeclaredMethod("nonBlocking");
      }
    }

    @Override
    public void run() throws Exception {

      if (mode == BlockMode.DURING_CALL) {
        simple.blocking();
      } else {
        if (mode == BlockMode.BEFORE_CALL) {
          Thread.sleep(TIMEOUT * FACTOR);
        }
        simple.nonBlocking();
        if (mode == BlockMode.AFTER_CALL) {
          Thread.sleep(TIMEOUT * FACTOR);
        }
      }
      throw new IllegalStateException("Should have terminated test already");
    }
  }


  private class BlockingSecondaryRunnable extends SimpleSecondaryRunnable {
    @Override
    public void run() throws Exception {
      simple.blocking();
    }
  }

  /**
   * Tests an InterleavedRunner where the main thread blocks before calling the
   * instrumented method
   */
  @ThreadedTest
  public void checkInterleavedRunnerMainThreadBlocksBeforeCall() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    BlockingMainRunnable main = new BlockingMainRunnable(BlockMode.BEFORE_CALL);
    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable();
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable mainException = result.getMainException();
    assertNotNull(mainException);
    assertTrue("Main exception = " + mainException, mainException instanceof TestTimeoutException);
    assertNull(result.getSecondaryException());
  }

  /**
   * Tests an InterleavedRunner where the main thread blocks while calling the
   * instrumented method
   */
  @ThreadedTest
  public void checkInterleavedRunnerMainThreadBlocksDuringCall() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    BlockingMainRunnable main = new BlockingMainRunnable(BlockMode.DURING_CALL);
    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable();
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable mainException = result.getMainException();
    assertNotNull(mainException);
    assertTrue("Main exception = " + mainException, mainException instanceof TestTimeoutException);
    assertNull(result.getSecondaryException());
  }

  /**
   * Tests an InterleavedRunner where the main thread blocks after calling the
   * instrumented method
   */
  @ThreadedTest
  public void checkInterleavedRunnerMainThreadBlocksAfterCall() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    BlockingMainRunnable main = new BlockingMainRunnable(BlockMode.AFTER_CALL);
    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable();
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable mainException = result.getMainException();
    assertNotNull(mainException);
    assertTrue("Main exception = " + mainException, mainException instanceof TestTimeoutException);
    assertNull(result.getSecondaryException());
  }

  /**
   * Tests an InterleavedRunner where the secondary thread blocks.
   */
  @ThreadedTest
  public void checkInterleavedRunnerSecondaryThreadBlocks() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);
    BlockingMainRunnable main = new BlockingMainRunnable(BlockMode.AFTER_CALL);
    BlockingSecondaryRunnable secondary = new BlockingSecondaryRunnable();
    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable secondaryException = result.getSecondaryException();
    assertNotNull(secondaryException);
    assertTrue("Secondary exception = " + secondaryException,
        secondaryException instanceof TestTimeoutException);
    assertNull(result.getMainException());
  }

  /**
   * Tests an InterleavedRunner where the secondary thread blocks becasue it is
   * waiting to enter a synchronized block that another thread will not release.
   */
  @ThreadedTest
  public void checkInterleavedRunnerSecondaryThreadBlocksOnFirstThread() {
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(SimpleClass.class);

    SimpleMainRunnable main = new SimpleMainRunnable() {
        Thread lockingThread;

        @Override
        public Method getMethod() throws NoSuchMethodException {
          return SimpleClass.class.getDeclaredMethod("nonBlocking");
        }

        @Override
        public void run() throws Exception {
          // Create an additional thread that will block the second thread. (We
          // can't just do it in the main thread, becasue the InterleavedRunner
          // will step throgh the main thread until it exits the synchronized
          // block.)
          lockingThread = new Thread() {
            @Override
            public void run() {
              try {
                simple.blockingInternally();
              } catch (InterruptedException e) {
                // expected - see terminate
              }
            }
          };
          lockingThread.start();
          long startTime = System.currentTimeMillis();
          while (lockingThread.getState() != Thread.State.TIMED_WAITING) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - startTime > TIMEOUT) {
              throw new RuntimeException("state = " + lockingThread.getState());
            }
          }
          simple.nonBlocking();
        }

        @Override
        public void terminate() {
          lockingThread.interrupt();
        }
      };

    SimpleSecondaryRunnable secondary = new SimpleSecondaryRunnable() {
        @Override
        public void run() {
          simple.synchronizedNonBlocking();
        }
      };

    RunResult result = InterleavedRunner.interleave(main, secondary);
    Throwable secondaryException = result.getSecondaryException();
    assertNotNull(secondaryException);
    assertTrue("Secondary exception = " + secondaryException,
        secondaryException instanceof TestTimeoutException);
    assertNull(result.getMainException());
  }

}
