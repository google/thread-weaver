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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Concrete implementation of {@link ObjectInstrumentation} and {@link
 * CallLogger}.  A new instance of ObjectInstrumentationImpl is created every
 * time a new instrumented object is constructed. This is achieved by adding a
 * special method call into the instrumented constructor. The other methods are
 * similarly instrumented to invoke CallLogger methods, so that execution can
 * stop at {@link Breakpoint}s. Test code uses the ObjectInstrumentation
 * interface to create Breakpoints.
 *
 * @see CallLoggerFactory
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class ObjectInstrumentationImpl<T> implements CallLogger, ObjectInstrumentation<T> {

  private T baseObject;
  private ClassInstrumentation instrumentedClass;

  /** Maintains information about the state of a given thread */
  private static class ThreadInfo {
    /**
     * The depth of the current call stack in this thread. Note that this only
     * counts calls made within instrumented methods.
     */
    int callDepth;

    /**
     * The current line of instrumented code being executed, or -1 if there
     * is no such line;
     */
    int currentLine = -1;

    /**
     * The current set of active Breakpoints for this thread.
     */
    Set<InstrumentedCodeBreakpoint> breakPoints = new HashSet<InstrumentedCodeBreakpoint>();
  }

  /** Maps a Thread onto the related ThreadInfo object */
  private static final WeakHashMap<Thread, ThreadInfo> threadMap
    = new WeakHashMap<Thread, ThreadInfo>();

  /**
   * Gets the ObjectInstrumentation representing the given base object. The test
   * environment must have been set up so that the base object's class has been
   * correctly instrumented.
   *
   * @param baseObject to object under test
   * @throws IllegalArgumentException if the base object has not been instrumented
   */
  @SuppressWarnings("unchecked")
  public static <T> ObjectInstrumentationImpl<T> getObject(T baseObject) {
    return CallLoggerFactory.getFactory().getObjectInstrumentation(baseObject);
  }

  /**
   * Creates a new ObjectInstrumentation for the corresponding base object. This
   * method is invoked from within the instrumented test code. It should not be
   * called from elsewhere.
   */
  ObjectInstrumentationImpl(T baseObject) {
    this.baseObject = baseObject;
    CallLoggerFactory factory = CallLoggerFactory.getFactory();
    this.instrumentedClass = factory.getClassInstrumentation(baseObject.getClass());
  }

  @Override
  public T getUnderlyingObject() {
    return baseObject;
  }

  @Override
  public Breakpoint createBreakpoint(CodePosition position, Thread thread) {
    return createBreakpointImpl(position, thread);
  }

  private InstrumentedCodeBreakpoint createBreakpointImpl(CodePosition position, Thread thread) {
    InstrumentedCodePosition codePos = (InstrumentedCodePosition) position;
    InstrumentedCodeBreakpoint breakPoint = codePos.createBreakpoint(thread);
    addBreakpoint(thread, breakPoint);
    return breakPoint;
  }

  /**
   * Registers a breakpoint with this instrumented object. This is used
   * internally by {@link Script}s in order to handle preregistered
   * breakpoints. Normally, breakpoints should be created using {@link
   * #createBreakpoint(CodePosition, Thread)}.
   */
  void registerBreakpoint(InstrumentedCodeBreakpoint breakPoint) {
    addBreakpoint(breakPoint.getThread(), breakPoint);
  }

  private ThreadInfo getThreadInfoTolerant(Thread thread) {
    ThreadInfo info = threadMap.get(thread);
    if (info == null) {
      info = new ThreadInfo();
      threadMap.put(thread, info);
    }
    return info;
  }

  private ThreadInfo getThreadInfo(Thread thread) {
    ThreadInfo info = threadMap.get(thread);
    if (info == null) {
      throw new IllegalStateException("Unknown thread " + thread);
    }
    return info;
  }

  private void addBreakpoint(Thread thread, InstrumentedCodeBreakpoint breakPoint) {
    synchronized (threadMap) {
      ThreadInfo info = getThreadInfoTolerant(thread);
      info.breakPoints.add(breakPoint);
      breakPoint.setOwner(this);
    }
  }

  private void setCurrentLineNumber(Thread thread, int lineNumber) {
    synchronized (threadMap) {
      getThreadInfo(thread).currentLine = lineNumber;
    }
  }

  private int getAndClearCurrentLineNumber(Thread thread) {
    synchronized (threadMap) {
      ThreadInfo info = getThreadInfo(thread);
      int result = info.currentLine;
      info.currentLine = -1;
      return result;
    }
  }

  /** Gets the call depth (in instrumented code) for the current thread */
  private int getCallDepth(Thread thread) {
    synchronized (threadMap) {
      return getThreadInfo(thread).callDepth;
    }
  }

  /** Increments the call depth (in instrumented code) for the current thread */
  private void incrementCallDepth(Thread thread) {
    synchronized (threadMap) {
      getThreadInfoTolerant(thread).callDepth++;
    }
  }

  /** Decrements the call depth (in instrumented code) for the current thread */
  private void decrementCallDepth(Thread thread) {
    synchronized (threadMap) {
      getThreadInfo(thread).callDepth--;
    }
  }

  @Override
  public Stepper step(Breakpoint bp) {
    if (!bp.isBlocked()) {
      throw new IllegalStateException("Cannot step unless waiting at breakpoint");
    }
    boolean hasNext = getCallDepth(bp.getThread()) > 0;
    return new StepperImpl(this, bp, hasNext);
  }

  /**
   * Called from a {@link StepperImpl} in order to step to the next
   * line. Updates the internal state of the StepperImpl.
   */
  void stepFromStepper(StepperImpl stepper) throws TestTimeoutException {
    Breakpoint toStepFrom = stepper.getCurrentBreakpoint();
    Thread thread = toStepFrom.getThread();
    if (getCallDepth(thread) == 0) {
      throw new IllegalStateException("Cannot step when call depth is 0");
    }

    AnyPositionBreakpoint next = new AnyPositionBreakpoint(thread);
    registerBreakpoint(next);
    toStepFrom.resume(next);
    stepper.setCurrentBreakpoint(next);
    stepper.setHasNext(getCallDepth(thread) > 0);
  }

  /**
   * Tests two interleaved threads. The main Runnable is assumed to call the
   * named method in the base object. The system will run the main Runnable,
   * stopping after lineCount executable lines. Once stopped, it will then
   * invoke the secondary Runnable, and allow it to complete. It will then let
   * the main runnable continue.
   * <p>
   * This method is normally called from an {@link InterleavedRunner}.
   *
   * @param main              the main runnable
   * @param mainMethod        the method invoked by the main runnable
   * @param lineCount         the line in the main method where the main
   *                          runnable should stop to let the second runnable
   *                          execute. The lines are numbered from 0
   *                          within the method. May be -1 if startPosition is
   *                          specified
   *
   * @param secondary         the secondary runnable
   * @param secondaryCanBlock if false, then throw an exception if the secondary
   *                          is blocked when trying to run becasue of a
   *                          synchronized lock held by the main runnable
   * @param startPosition     if non-null, then the main runnable should run to
   *                          here before letting the secondary run. Overrides
   *                          the lineCount
   * @param startCount        the number of times the startPosition must be
   *                          hit before the secondary is allowed to run.
   *
   * @return                  the stepped run result
   */
  SteppedRunResult interleave(ThrowingRunnable main, MethodInstrumentation mainMethod,
      int lineCount, ThrowingRunnable secondary, boolean secondaryCanBlock,
      CodePosition startPosition,
      int startCount) {

    List<LineInstrumentation> lines = mainMethod.getLines();
    int currLine = lines.get(0).getLineNumber();
    int targetLine;

    if (lineCount < 0) {
      if (startPosition == null) {
        throw new IllegalArgumentException("Must specify startPosition if lineCount < 0");
      }
      targetLine = -1;
    } else {
      if (!(lineCount >= 0 && lineCount < lines.size())) {
        throw new IllegalArgumentException("Invalid lineCount " + lineCount);
      }
      if (startPosition == null) {
        if (startCount != 0) {
          throw new IllegalArgumentException("Cannot specify startCount without startPosition");
        }
      }
      targetLine = lines.get(lineCount).getLineNumber();
    }
    Options.debugPrint("interleave to line %d from %s %d\n", targetLine, startPosition, startCount);

    String name = mainMethod.toString();
    TestThread mainThread = new TestThread(main, "Main Test Thread " + name);
    TestThread secondThread = new TestThread(secondary, "Second Test Thread " + name);
    boolean atEndOfMethod = false;
    Throwable mainException = null;
    try {
      InstrumentedCodeBreakpoint startBreakpoint;

      // If startPosition was not specified, start at the beginning of the
      // method.
      if (startPosition == null) {
        startPosition = instrumentedClass.atMethodStart(mainMethod.getUnderlyingMethod());
      }
      startBreakpoint = createBreakpointImpl(startPosition, mainThread);
      Options.debugPrint("Using start breakpoint %s\n", startBreakpoint);

      InstrumentedCodeBreakpoint continueBreakpoint = startBreakpoint;
      startBreakpoint.setLimit(startCount == 0 ? 1 : startCount);
      Stepper stepper = null;

      mainThread.start();
      startBreakpoint.await();
      currLine = getAndClearCurrentLineNumber(mainThread);
      Options.debugPrint("Reached start point at line %d\n", currLine);

      boolean atEnd = false;
      // If we haven't yet passed the target line, then we need to run until we
      // reach it. Note that a line number of -1 means that we haven't yet
      // reached any instrumented line. This happens if we are at the start of
      // the method.
      if (currLine < targetLine) {
        LineCodePosition targetLinePos = new LineCodePosition(targetLine);
        CodePosition endPosition = instrumentedClass.atMethodEnd(mainMethod.getUnderlyingMethod());
        MultiPositionBreakpoint targetBreakpoint =
            new MultiPositionBreakpoint(mainThread, targetLinePos, endPosition);
        addBreakpoint(mainThread, targetBreakpoint);
        continueBreakpoint = targetBreakpoint;
        startBreakpoint.resume(targetBreakpoint);
        atEnd = targetBreakpoint.getMatchers().contains(endPosition);
      }

      currLine = getAndClearCurrentLineNumber(mainThread);
      Options.debugPrint("Reached line %d, atEnd %s\n", currLine, atEnd);

      Options.debugPrint("Starting second thread\n");
      secondThread.start();

      boolean secondFinished = new ThreadMonitor(secondThread, mainThread).waitForThread();
      Options.debugPrint("secondFinished = %s\n", secondFinished);

      if (!secondFinished && !secondaryCanBlock) {
        throw new TestTimeoutException("Second thread blocked", secondThread);
      }
      // If the second thread didn't run because it was blocked, then step
      // through the first thread, trying to run the second thread after each
      // line.
      if (!secondFinished) {
        if (stepper == null) {
          stepper = step(continueBreakpoint);
        }
        while (stepper.hasNext() && !secondFinished) {
          Options.debugPrint("  stepping - secondFinished = %s\n", secondFinished);
          secondFinished = new ThreadMonitor(secondThread, mainThread).waitForThread();
          stepper.step();
        }
      }
      // Tell the main thread to continue to completion. If we have never
      // stepped through the code, then we can just continue from the initial
      // breakpoint. Otherwise we ask the stepper to continue
      if (stepper == null) {
        continueBreakpoint.resume();
      } else {
        stepper.resume();
      }
      mainThread.finish();

      if (!secondFinished) {
        secondFinished = new ThreadMonitor(secondThread, mainThread).waitForThread();
      }
      if (!secondFinished) {
        throw new TestTimeoutException("Main thread has finished but second thread has not",
            secondThread);
      }
    } catch (TestTimeoutException e) {
      if (e.getThread() == mainThread) {
        Throwable threadException = mainThread.getException();
        if (threadException == null) {
          threadException = e;
        }
        return new SteppedRunResult(threadException, null, currLine);
      } else {
        Throwable threadException = secondThread.getException();
        if (threadException == null) {
          threadException = e;
        }
        return new SteppedRunResult(null, threadException, currLine);
      }
    } catch (InterruptedException e) {
      mainException = e;
    }
    if (mainException == null) {
      mainException = mainThread.getException();
    }
    return new SteppedRunResult(mainException, secondThread.getException(), currLine);
  }

  /**
   * Checks to see if there is a Breakpoint set for the given thread and
   * position, and returns true if there is, and if the Breakpoint's count has
   * been hit. Note that calling this method will increment the number of times
   * that the Breakpoint has been hit.
   */
  private  void checkBreakpoint(Thread thread, CodePosition position) {
    List<InstrumentedCodeBreakpoint> hitPoints = new ArrayList<InstrumentedCodeBreakpoint>();

    synchronized (threadMap) {
      ThreadInfo info = getThreadInfo(thread);
      Iterator<InstrumentedCodeBreakpoint> it = info.breakPoints.iterator();
      while (it.hasNext()) {
        InstrumentedCodeBreakpoint breakPoint = it.next();
        if (breakPoint.matches(position)) {
          it.remove();
          hitPoints.add(breakPoint);
        }
      }
    }
    for (InstrumentedCodeBreakpoint hitPoint : hitPoints) {
      hitPoint.atBreakpoint(this);
    }
  }

  //==========================================================================================
  // Implementation of CallLogger methods. These methods are called by the instrumented class.
  //==========================================================================================

  @Override
  public void atLine(int line) {
    Options.debugPrint("atLine %d in %s\n", line, Thread.currentThread());
    setCurrentLineNumber(Thread.currentThread(), line);
    CodePosition position = new LineCodePosition(line);
    checkBreakpoint(Thread.currentThread(), position);
  }

  @Override
  public void start(Method method) {
    Options.debugPrint("start %s in %s\n", method.toGenericString(), Thread.currentThread());
    incrementCallDepth(Thread.currentThread());
    CodePosition position = instrumentedClass.atMethodStart(method);
    checkBreakpoint(Thread.currentThread(), position);
  }

  @Override
  public void end(Method method) {
    Options.debugPrint("end %s in %s\n", method.getName(), Thread.currentThread());
    decrementCallDepth(Thread.currentThread());
    CodePosition position = instrumentedClass.atMethodEnd(method);
    checkBreakpoint(Thread.currentThread(), position);
  }

  @Override
  public void beginCall(Method source, int line, Method target) {
    Options.debugPrint("  begin call %s->%s in %s\n", source.getName(), target.getName(),
        Thread.currentThread());
    CodePosition position = instrumentedClass.beforeCall(source, target);
    checkBreakpoint(Thread.currentThread(), position);
  }

  @Override
  public void endCall(Method source, int line, Method target) {
    Options.debugPrint("  end call %s->%s in %s\n", source.getName(), target.getName(),
        Thread.currentThread());
    CodePosition position = instrumentedClass.afterCall(source, target);
    checkBreakpoint(Thread.currentThread(), position);
  }
}
