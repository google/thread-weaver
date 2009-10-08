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
import java.util.concurrent.CountDownLatch;

/**
 * A Script represents a sequence of operations carried out on an
 * object-under-test. Several different Scripts can be created, and combined
 * together by a {@link Scripter}. Each Script will run in a separate thread,
 * and will have the option of yielding control to other Scripts using the
 * {@link #releaseTo} method. This allows a series of operations to be
 * co-ordinated across multiple threads.
 *
 * @param <T> the type of the object-under-test.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class Script<T> {

  /** The main object-under-test for this Script */
  private final T mainObject;

  /** The MethodRecorder used to create CodePositions for a release */
  private final MethodRecorder<T> recorder;

  /** The set of tasks to be executed for this script */
  private List<ScriptedTask<T>> tasks = new ArrayList<ScriptedTask<T>>();

  /**
   * Represents the target of a release. Used to represent the position where we
   * will release once the script has started executing.
   */
  static class ScriptTarget<T> {
    final CodePosition codePosition;
    final Script<T> script;

    ScriptTarget(CodePosition codePosition, Script<T> targetScript) {
      this.codePosition = codePosition;
      this.script = targetScript;
    }
  }

  /** The list of release targets used when executing this script. */
  private List<ScriptTarget<T>> futureTargets = new ArrayList<ScriptTarget<T>>();

  /** The Scripter that is executing this Script. */
  private volatile Scripter<T> scripter = null;

  /** The Thread in which this script is executing */
  private volatile TestThread thread;

  /**
   * The breakpoints where this script will stop and release control to another
   * script.
   */
  private List<Breakpoint> breakPoints;

  /** Index into the breakPoints list */
  @SuppressWarnings("unused")
  private volatile int breakPointIndex;

  /** Latch where the script blocks until released. */
  private volatile CountDownLatch blockingLatch = new CountDownLatch(1);

  private volatile Stepper stepper;

  /**
   * Creates a new Script that will operate on the given object-under-test. The
   * tasks added to this script will normally invoke methods on this object.
   * Note that the object's class must be instrumented.
   */
  public Script(T object) {
    this.mainObject = object;
    this.recorder = new MethodRecorder<T>(object);
  }

  /**
   * Creates a new Script that will operate on the same object-under-test as
   * another Script. This is normally used to create secondary scripts.
   *
   * @see Scripter#Scripter(Script, Script)
   */
  public Script(Script<T> other) {
    this(other.mainObject);
  }

  /**
   * Returns a control object that can be used to define release points in the
   * object-under-test. This is a dummy object, of the same type as the
   * object-under-test, but whose methods do nothing.
   */
  public T object() {
    return recorder.getControl();
  }

  public <T> T createTarget(Class<T> clss) {
    return recorder.createTarget(clss);
  }

  /**
   * Adds a task to this Script. Tasks will be executed when {@link
   * Scripter#execute} is called. Note that this method should not be invoked
   * after {@link Scripter#execute} has been called.
   */
  public void addTask(ScriptedTask<T> task) {
    if (running()) {
      throw new IllegalStateException("Cannot add more tasks when running");
    }
    tasks.add(task);
  }

  /**
   * Prepares this ScriptedThread to be executed by the given scripter in the
   * given thread. Creates Breakpoints for all of the release positions that
   * have been predefined. This method is invoked by the owning Scripter before
   * calling {@link #runTasks}.
   */
  void prepare(Scripter<T> theScripter, TestThread theThread) {
    this.scripter = theScripter;
    ObjectInstrumentation<T> instrumented = getInstrumentedObject();
    breakPoints = new ArrayList<Breakpoint>(futureTargets.size());
    for (ScriptTarget<T> target : futureTargets) {
      Breakpoint breakPoint = instrumented.createBreakpoint(target.codePosition, theThread);
      breakPoint.setHandler(new ReleasingHandler(target.script));
      breakPoints.add(breakPoint);
    }
    this.thread = theThread;
  }

  /** Returns true if the script has been started by the Scripter. */
  private boolean running() {
    return thread != null;
  }

  /**
   * Executes the tasks in this Script. Invoked by the owning Scripter, in the
   * Script's thread. This method will block initially until {@link #releaseTo} is
   * called.
   */
  void runTasks() throws Exception {
    Options.debugPrint("Starting %s\n", this);
    block();
    try {
      for (ScriptedTask<T> task : tasks) {
        Options.debugPrint("%s starting task %s\n", this, task);
        task.setOwner(this);
        task.execute();
        Options.debugPrint("%s finished task %s\n", this, task);
      }
    } catch (Exception e) {
      Options.debugPrintStackTrace(e);
      throw e;
    } finally {
      Options.debugPrint("Finished %s\n", this);
      scripter.onFinished(this);
    }
  }

  /**
   * Declares an intent to release control to another thread in the future, or
   * performs the actual release now. The behaviour depends on whether a method
   * has been invoked on the control object. (See {@link #object}.) If it has,
   * then the release is scheduled when the position represented by the control
   * call has been reached, and the state of the control object is
   * cleared. Otherwise, the release is performed immediately.
   * <p>
   * Releasing control will block this script, and allow the released script to
   * start, or to resume execution. This script will remain blocked until
   * another script releases control back to it, or until the first Script
   * belonging to the {@link Scripter} is released.
   * <p>
   * Note that it is only possible to release to another Script that is being
   * run by the same {@link Scripter}.
   */
  public void releaseTo(Script<T> other) {
    if (this == other) {
      throw new IllegalArgumentException("Cannot release control to the same script");
    }
    if (!running()) {
      // If we're not running, then we must be scheduling a future
      // release. (Calling recorder.position() will throw an
      // IllegalStateException if a valid position has not been specified, so no
      // need to check here.
      //
      futureTargets.add(new ScriptTarget<T>(recorder.position(), other));
    } else {
      // If the scripter has been set, then we are either releasing control or
      // preparing to do so, depending on whether a position has been specified.
      CodePosition cp = recorder.getPositionIfAny();
      if (cp == null) {
        doRelease(other);
      } else {
        Breakpoint breakPoint = getInstrumentedObject().createBreakpoint(cp, thread);
        breakPoint.setHandler(new ReleasingHandler(other));
        synchronized (breakPoints) {
          breakPoints.add(breakPoint);
        }
      }
    }
  }

  /**
   * Releases control to another Script, and then blocks waiting until resume()
   * is called. This method is called from {@link #releaseTo}, or when we hit a
   * predefined breakpoint.
   */
  private void doRelease(Script<T> other) {
    if (thread != Thread.currentThread()) {
      throw new IllegalStateException("Can only release from the script's own thread");
    }
    blockingLatch = new CountDownLatch(1);
    scripter.release(this, other);
    block();
  }

  /** Gets the instrumented object associated with the object-under-test */
  private ObjectInstrumentation<T> getInstrumentedObject() {
    return recorder.getInstrumentedObject();
  }

  /** Gets the TestThread in which this Script is running its tasks. */
  TestThread getThread() {
    return thread;
  }

  /**
   * Breakpoint handler that releases control to another script when
   * a breakpoint is reached.
   */
  private class ReleasingHandler implements BreakpointHandler {
    private Script<T> other;
    ReleasingHandler(Script<T> script) {
      this.other = script;
    }

    @Override
    public boolean handleBreakpoint(Breakpoint breakPoint) {
      doRelease(other);
      // Return true to indicate that the breakpoint has been consumed here, and
      // that normal breakpoint handling should not take place. The logic for
      // blocking at a release point is handled internally, in block().
      return true;
    }
  }

  /**
   * Blocks this script. Execution will not continue until {@link #doRelease}
   * is called.
   */
  private void block() {
    try {
      blockingLatch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Tells this script to begin stepping, one line at a time. Used by
   * {@link Scripter} when it detects that one script is blocked by
   * another. Tells the blocking script to continue stepping, with
   * the intent of releasing the monitor lock.
   */
  Stepper startStepping() throws TestTimeoutException {
    AnyPositionBreakpoint breakpoint = new AnyPositionBreakpoint(getThread());
    ObjectInstrumentationImpl<T> instrumented =
        (ObjectInstrumentationImpl<T>) getInstrumentedObject();
    instrumented.registerBreakpoint(breakpoint);
    resume();
    breakpoint.await();
    this.stepper = instrumented.step(breakpoint);
    return this.stepper;
  }

  /** Returns true if {@link #step} can safely be called. */
  boolean canStep() {
    return this.stepper != null && this.stepper.hasNext();
  }

  /** Tells the script to step through another executable line. */
  void step() throws TestTimeoutException {
    this.stepper.step();
  }

  /** Tells the script to stop stepping, and resume normal execution. */
  void finishStepping() throws TestTimeoutException {
    if (stepper != null) {
      stepper.resume();
      stepper = null;
    }
  }

  /**
   * Tells a blocked script to continue to execute. A script is blocked
   * when {@link #releaseTo} is called.
   */
  void resume() {
    blockingLatch.countDown();
  }

  @Override
  public String toString() {
    if (thread == null) {
      return "Script " + mainObject.getClass().getName() + " (not started)";
    } else {
      return thread.getName() + " - scripting " + mainObject.getClass().getName();
    }
  }

  // The following methods delegate to the MethodRecorder

  public Script<T> afterCalling(Object result) {
    recorder.afterCalling(result);
    return this;
  }

  public Script<T> afterCallingLastMethod() {
    recorder.afterCallingLastMethod();
    return this;
  }

  public Script<T> atEndOf(Object result) {
    recorder.atEndOf(result);
    return this;
  }

  public Script<T> atEndOfLastMethod() {
    recorder.atEndOfLastMethod();
    return this;
  }

  public Script<T> atStartOf(Object result) {
    recorder.atStartOf(result);
    return this;
  }

  public Script<T> atStartOfLastMethod() {
    recorder.atStartOfLastMethod();
    return this;
  }

  public Script<T> beforeCalling(Object result) {
    recorder.beforeCalling(result);
    return this;
  }

  public Script<T> beforeCallingLastMethod() {
    recorder.beforeCallingLastMethod();
    return this;
  }

  public Script<T> inLastMethod() {
    recorder.inLastMethod();
    return this;
  }

  public Script<T> in(Object result) {
    recorder.in(result);
    return this;
  }
}
