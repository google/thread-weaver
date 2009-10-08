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


import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that tests two interleaved threads. The majority of methods in
 * this class are designed to be used with {@link ClassInstrumentation instrumented
 * classes}, although there is one version of the {@link
 * #interleave(MainRunnable, SecondaryRunnable, List)} method that can be used
 * with non-instrumented classes.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */

/*
 * A note on exception handling. There are several places here where we catch
 * Throwable. This is normally frowned upon, but in each case we record the
 * Throwable, and return it in the RunResult, so that the caller can determine
 * what went wrong during the test. We catch Throwable rather than Exception
 * becasue assertion failures are subclasses of Error, not of Exception, and it
 * is quite likely that a test failure in one of the interleaved threads will
 * result in an assertion error.
 */
public class InterleavedRunner {

  private InterleavedRunner() {
    // All methods are static, so no public constructor
  }

  /**
   * Invokes two runnable instances, interleaving the execution. The main
   * runnable instance specifes an instrumented method that will be called. (See
   * {@link MainRunnable#getMethod}. If N is the number of executable lines in
   * this method, then the main runnable will be executed N times, stopping at a
   * different line each time. When the main runnable is stopped, the secondary
   * runnable will be executed until it completes, and then the main runnable
   * will continue. This allows a test to verify that the main method behaves
   * correctly even if another method is called part way through its
   * execution. Note that the framework will correctly handle the case where the
   * second thread is blocked because of synchronization and/or locks in the
   * first method.
   * <p>
   * Due to the structure of the method being tested, not every executable line
   * may be reached. (E.g. conditional or error-handling blocks may not be
   * entered.)  The InterleavedRunner will attempt to break execution at every
   * line, but if a given line is not reached, the runner will continue until
   * the end of the test method.
   * <p>
   * Note that the method being tested must be instrumented. See {@link
   * ClassInstrumentation}.
   *
   * @param main the main runnable
   * @param secondary the secondary runnable
   *
   * @return a RunResult indicating any exceptions thrown by the two runnables.
   *
   * @throws IllegalArgumentException if the main runnable does not specify a
   * valid instrumented class/method.
   */
  public static <M extends MainRunnable<T>, T> RunResult interleave(
      M main, SecondaryRunnable<T, M> secondary) {
    return doInterleave(main, secondary, null, 0);
  }

  /**
   * Invokes two runnable instances, interleaving the execution. This is
   * identical to {@link #interleave}, except that the main method will not stop
   * until the given code position is reached the given number of times. This
   * allows a test to simulate cases when a second thread is invoked after a
   * certain condition. (E.g. an event is received after the main method
   * registers a listener, but not before.)
   * <p>
   * Note that this method can only be invoked when the given runnables are
   * calling a method in an instrumented class. See {@link ClassInstrumentation}.
   *
   * @param main the main runnable
   * @param secondary the secondary runnable
   * @param position the secondary runnable will not start until the first
   *        runnable has at least reached this position .
   * @param posCount the number of time the first runnable must reach the given
   *        position
   *
   * @return a RunResult indicating any exceptions thrown by the two runnables.
   *
   * @throws IllegalArgumentException if the main runnable does not specify a
   * valid instrumented class/method, or if the code position does not specify a
   * position within the main method.
   */
  public static <M extends MainRunnable<T>, T> RunResult interleaveAfter(
      M main, SecondaryRunnable<T, M> secondary, CodePosition position, int posCount) {
    return doInterleave(main, secondary, position, posCount);
  }

  private static MethodInstrumentation getMainMethod(ClassInstrumentation clss,
      MainRunnable<?> main) {
    try {
      if (main.getMethod() != null) {
        return clss.getMethod(main.getMethod());
      } else if (main.getMethodName() != null) {
        return clss.getMethod(main.getMethodName());
      } else {
        throw new IllegalArgumentException("No main method defined");
      }
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("Invalid main method defined", e);
    }
  }

  private static  <M extends MainRunnable<T>, T> RunResult doInterleave(
      M main, SecondaryRunnable<T, M> secondary, CodePosition startPosition, int posCount) {
    CallLoggerFactory logger = CallLoggerFactory.getFactory();
    ClassInstrumentation instrClss = logger.getClassInstrumentation(main.getClassUnderTest());
    MethodInstrumentation method = getMainMethod(instrClss, main);
    int numLines = method.getNumLines();
    for (int i = 0; i < numLines; i++) {
      Throwable mainException = null;
      Throwable secondaryException = null;
      try {
        main.initialize();
      } catch (Throwable e) {
        return new RunResult(e, null);
      }
      try {
        secondary.initialize(main);
      } catch (Throwable e) {
        return new RunResult(null, e);
      }
      ObjectInstrumentationImpl<T> instr =
          ObjectInstrumentationImpl.getObject(main.getMainObject());
      SteppedRunResult result = instr.interleave(main, method, i, secondary, secondary.canBlock(),
          startPosition, posCount);
      if (result.hadException()) {
        return result;
      }
      try {
        main.terminate();
      } catch (Throwable e) {
        mainException = e;
      }
      try {
        secondary.terminate();
      } catch (Throwable e) {
        secondaryException = e;
      }
      if (mainException != null || secondaryException != null) {
        return new RunResult(mainException, secondaryException);
      }
    }
    return new RunResult();
  }

  /**
   * Invokes two runnable instances, interleaving the execution. The main
   * runnable will be executed once for each CodePosition in the list, stopping
   * at each position in turn. When the main runnable is stopped, the secondary
   * runnable will be executed until it completes, and then the main runnable
   * will continue. This allows a test to verify that the main method behaves
   * correctly even if another method is called part way through its
   * execution. Note that the framework will correctly handle the case where the
   * second thread is blocked because of synchronization and/or locks in the
   * first method.
   * <p>
   * It is the caller's responsibility to ensure that the main runnable will
   * reach the given positions when executing.
   * <p>
   * Note that the method being tested must be instrumented. See {@link
   * ClassInstrumentation}.
   *
   * @param main the main runnable
   * @param secondary the secondary runnable
   * @param positions a list of code positions where the main runnable will stop
   *
   * @return a RunResult indicating any exceptions thrown by the two runnables.
   *
   * @throws IllegalArgumentException if the main runnable does not specify a
   * valid instrumented class/method.
   */
  public static <M extends MainRunnable<T>, T> RunResult interleave(
      M main, SecondaryRunnable<T, M> secondary, List<CodePosition> positions) {
    List<WrappedBreakpoint> wrappers = new ArrayList<WrappedBreakpoint>(positions.size());
    for (CodePosition position : positions) {
      wrappers.add(new PositionWrapper(position));
    }
    return interleaveAtWrappedBreakpoints(main, secondary, wrappers);
 }

  /**
   * Version of {@link #interleave(MainRunnable, SecondaryRunnable, List)} that
   * uses ordinary Runnables, and takes a single ReusableBreakpoint. Because there is
   * only a single ReusableBreakpoint, the main and secondary Runnables will only be
   * executed once, and hence do not need to go through the full lifecycle
   * defined by the {@link MainRunnable} class.
   */
  public static RunResult interleaveAtBreakpoint(final Runnable mainRunnable,
      final Runnable secondaryRunnable, ReusableBreakpoint breakpoint) {

    MainRunnableImpl<Object> main = new MainRunnableImpl<Object>() {
      @Override
      public void run() {
        mainRunnable.run();
      }
    };

    SecondaryRunnableImpl<Object, MainRunnableImpl<Object>> secondary =
        new SecondaryRunnableImpl<Object, MainRunnableImpl<Object>>() {
      @Override
      public void run() {
        secondaryRunnable.run();
      }
    };
    ArrayList<ReusableBreakpoint> breakpoints = new ArrayList<ReusableBreakpoint>();
    breakpoints.add(breakpoint);
    return interleaveAtReusableBreakpoints(main, secondary, breakpoints);
  }

  /**
   * Version of {@link #interleave(MainRunnable, SecondaryRunnable, List)}
   * that takes a single ReusableBreakpoint
   */
  public static <M extends MainRunnable<T>, T> RunResult interleaveAtBreakpoint(
      M main, SecondaryRunnable<T, M> secondary, ReusableBreakpoint breakpoint) {
    ArrayList<ReusableBreakpoint> breakpoints = new ArrayList<ReusableBreakpoint>();
    breakpoints.add(breakpoint);
    return interleaveAtReusableBreakpoints(main, secondary, breakpoints);
  }

  /**
   * Invokes two runnable instances, interleaving the execution using the
   * supplied list of {@link ReusableBreakpoint}s. The main runnable will be executed
   * once for every ReusableBreakpoint in the list, stopping at each ReusableBreakpoint in turn.
   * When stopped, the secondary runnable will be executed until it completes,
   * and then the main runnable will continue. This allows a test to verify that
   * the main method behaves correctly even if another method is called part way
   * through its execution. Note that the framework will correctly handle the
   * case where the second thread is blocked because of synchronization and/or
   * locks in the first method.
   * <p>
   * Internally, this method will create new Threads, and will invoke
   * {@link ReusableBreakpoint#setThread} on the given ReusableBreakpoints, to ensure
   * that the main runnable stops correctly.
   * <p>
   * Note that the methodName and class defined by the main runnable will be
   * ignored. This method can be invoked even when the runnables are not calling
   * into an instrumented method, provided that the given breakpoints do not
   * require instrumentation. In general, this method is designed to be used
   * with non-instrumented classes.
   *
   * @return a RunResult indicating any exceptions thrown by the two runnables.
   */
  public static <M extends MainRunnable<T>, T> RunResult interleaveAtReusableBreakpoints(
      M main, SecondaryRunnable<T, M> secondary, List<ReusableBreakpoint> breakpoints) {
    List<WrappedBreakpoint> wrappers = new ArrayList<WrappedBreakpoint>(breakpoints.size());
    for (ReusableBreakpoint breakpoint : breakpoints) {
      wrappers.add(new ReusableWrapper(breakpoint));
    }
    return interleaveAtWrappedBreakpoints(main, secondary, wrappers);
  }

  /**
   * A wrapper around a logical breakpoint. This is used by {@link
   * InterleavedRunner#interleaveAtWrappedBreakpoints}, and allows us to use the
   * same method to handle both ReusableBreakpoints and CodePositions.
   */
  private static abstract class WrappedBreakpoint {
    /**
     * Resets this object so that it can be used with the given runnable in the
     * given thread.
     */
    abstract Breakpoint reset(MainRunnable<?> main, Thread t);

    /**
     * When using a list of WrappedBreakpoints, this method will be invoked on
     * every breakpoint except the current one. See {@link
     * InterleavedRunner#disableWrappers}/
     */
    void disable() {
    }

    /**
     * Enables a breakpoint after it has been disabled.
     */
    void enable() {
    }
  }

  /**
   * Implements WrappedBreakpoint using a ReusableBreakpoint
   */
  private static class ReusableWrapper extends WrappedBreakpoint {

    private ReusableBreakpoint breakpoint;

    ReusableWrapper(ReusableBreakpoint breakpoint) {
      this.breakpoint = breakpoint;
    }

    @Override
    public Breakpoint reset(MainRunnable<?> main, Thread t) {
      breakpoint.setThread(t);
      return breakpoint;
    }

    @Override
    public void enable() {
      breakpoint.enable();
    }

    @Override
    public void disable() {
      breakpoint.disable();
    }
  }

  /**
   * Implements WrappedBreakpoint using a CodePosition. Every time this object
   * is reset, it creates a new Breakpoint based on the undelying CodePosition.
   */
  private static class PositionWrapper extends WrappedBreakpoint {
    private CodePosition position;

    PositionWrapper(CodePosition position) {
      this.position = position;
    }

    @Override
    public Breakpoint reset(MainRunnable<?> main, Thread t) {
      ObjectInstrumentationImpl<?> instr =
          ObjectInstrumentationImpl.getObject(main.getMainObject());
      return instr.createBreakpoint(position, t);
    }
  }

  /**
   * Interleaves two runnables, using the list of wrapped breakpoints.
   */
  private static <M extends MainRunnable<T>, T> RunResult interleaveAtWrappedBreakpoints(
      M main, SecondaryRunnable<T, M> secondary, List<WrappedBreakpoint> wrappers) {
    try {
      for (int i = 0; i < wrappers.size(); i++) {
        disableWrappers(wrappers, i);
        WrappedBreakpoint wrapper = wrappers.get(i);

        Throwable mainException = null;
        Throwable secondaryException = null;
        try {
          main.initialize();
        } catch (Throwable e) {
          return new RunResult(e, null);
        }
        try {
          secondary.initialize(main);
        } catch (Throwable e) {
          return new RunResult(null, e);
        }
        TestThread mainThread = new TestThread(main, "Main Test Thread");
        TestThread secondThread = new TestThread(secondary, "Second Test Thread");
        Breakpoint breakpoint = wrapper.reset(main, mainThread);
        mainThread.start();
        try {
          breakpoint.await();
        } catch (TestTimeoutException e) {
          return new RunResult(e, null);
        }
        secondThread.start();
        boolean secondFinished;
        try {
          secondFinished = new ThreadMonitor(secondThread, mainThread).waitForThread();
        } catch (InterruptedException e) {
          return new RunResult(null, e);
        } catch (TestTimeoutException e) {
          return new RunResult(null, e);
        }
        if (!secondFinished && !secondary.canBlock()) {
          return new RunResult(null,
              new IllegalThreadStateException("Second thread blocked at " + breakpoint));
        }
        breakpoint.resume();
        try {
          mainThread.finish();
        } catch (IllegalThreadStateException e) {
          return new RunResult(e, null);
        } catch (TestTimeoutException e) {
          return new RunResult(e, null);
        }
        if (!secondFinished) {
          try {
            secondThread.finish();
          } catch (IllegalThreadStateException e) {
            return new RunResult(null, e);
          } catch (TestTimeoutException e) {
            return new RunResult(null, e);
          }
        }
        if (mainThread.getException() != null || secondThread.getException() != null) {
          return new RunResult(mainThread.getException(), secondThread.getException());
        }
        try {
          main.terminate();
        } catch (Throwable e) {
          mainException = new Exception("Error at breakpoint " + i, e);
        }
        try {
          secondary.terminate();
        } catch (Throwable e) {
          secondaryException = new Exception("Error at breakpoint " + i, e);
        }
        if (mainException != null || secondaryException != null) {
          return new RunResult(mainException, secondaryException);
        }
      }
      return new RunResult();
    } catch (InterruptedException e) {
      throw new RuntimeException("Runner interrupted", e);
    }
  }

  private static void disableWrappers(List<WrappedBreakpoint> wrappers, int notDisabled) {
    for (int i = 0; i < wrappers.size(); i++) {
      WrappedBreakpoint wrapper = wrappers.get(i);
      if (i == notDisabled) {
        wrapper.enable();
      } else {
        wrapper.disable();
      }
    }
  }
}
