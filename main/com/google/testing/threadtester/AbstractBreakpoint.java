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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for implementations of {@link Breakpoint}. Provides the basic
 * functionality for pausing and resuming, but does not provide any mechanism
 * for recognising when a break point has been reached. Concrete subclasses must
 * provide a mechanism for recognising when a breakpoint has been reached. Upon
 * reaching a breakpoint, they should call {@link #hitBreakpoint}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class AbstractBreakpoint implements Breakpoint {

  /** Generates a unique internal ID for each breakpoint. */
  private static AtomicInteger atomicId = new AtomicInteger(1);

  /** The internal ID for this breakpoint. See {@link #getId}. */
  private final int id;

  /** The optional handler. See {@link #setHandler}. */
  private volatile BreakpointHandler handler;

  /** Latch used for implementing {@link #wait}. */
  private volatile CountDownLatch stopLatch;

  /** Latch used for implementing {@link #resume}. */
  private volatile CountDownLatch restartLatch;

  /** Set to true if this breakpoint is curently enabled. Defaults to true. */
  protected volatile boolean enabled;

  /**
   * The number of times this breakpoint can be hit before activating. Default
   * value is 1.
   */
  protected volatile int limit;

  /** The number of times this breakpoint has been hit. */
  protected volatile int numHits;

  /** Set to true if the thread breakpoint is curently blocked. */
  protected volatile boolean blocked;

  /** The thread associated with this breakpoint. See {@link #getThread}.*/
  protected volatile Thread thread;

  /**
   * Creates a new breakpoint associated with the given thread.
   */
  protected AbstractBreakpoint(Thread thread) {
    this();
    if (thread == null) {
      throw new IllegalArgumentException("thread cannot be null");
    }
    this.thread = thread;
  }

  /**
   * Creates a new breakpoint that is not initially associated with a thread.
   * The thread should be set with {@link #setThreadImpl(Thread)} before using
   * this breakpoint.
   */
  protected AbstractBreakpoint() {
    this.id = atomicId.getAndIncrement();
    initialize();
  }

  private final void initialize() {
    this.enabled = true;
    this.stopLatch = new CountDownLatch(1);
    this.restartLatch = new CountDownLatch(1);
    this.numHits = 0;
    this.limit = 1;
  }

  /**
   * Resets the breakpoint. After resetting, the breakpoint will be enabled,
   * with a limit of 1, and with the number of hits set to 0.
   *
   * @throws IllegalStateException if this breakpoint has already been hit.
   */
  protected void reset() {
    if (stopLatch.getCount() == 0 && restartLatch.getCount() > 0) {
      throw new IllegalStateException("Cannot reset until resume is called");
    }
    initialize();
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  /**
   * Sets the thread associated with this breakpoint, and calls {@link
   * #reset()}. May be used by implementations of {@link ReusableBreakpoint}.
   * Note that the thread cannot be reset if the original thread is blocked at
   * this breakpoint. (I.e. if the breakpoint has been hit, but {@link #resume}
   * has not been called.)
   */
  protected void setThreadImpl(Thread newThread) {
    reset();
    thread = newThread;
  }

  @Override
  public void setHandler(BreakpointHandler handler) {
    this.handler = handler;
  }

  /**
   * Gets the number of times that this Breakpoint has been hit. A Breakpoint
   * with a limit > 1 will be hit more than once before finally blocking.  Note
   * that a Breakpoint that is not enabled will not be considered to have been
   * hit.
   *
   * @see Breakpoint#getLimit
   * @see Breakpoint#isEnabled
   */
  public int getHits() {
    return numHits;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public void setLimit(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("Invalid count " + count);
    }
    if (blocked) {
      throw new IllegalStateException("Cannot set limit when breakpoint is already blocked");
    }

    this.limit = count;
  }

  @Override
  public final boolean isEnabled() {
    return enabled;
  }

  @Override
  public void disable() {
    this.enabled = false;
  }

  @Override
  public void enable() {
    this.enabled = true;
  }

  @Override
  public boolean isBlocked() {
    return blocked;
  }

  @Override
  public void await() throws TestTimeoutException {
    Options.debugPrint("Breakpoint.await %s\n" , this);
    try {
      if (!stopLatch.await(Options.timeout(), TimeUnit.MILLISECONDS)) {
        throw new TestTimeoutException("Did not reach " + this, getThread());
      }
    } catch (InterruptedException e) {
      throw new IllegalThreadStateException("Interrupted");
    }
  }

  @Override
  public void resume(Breakpoint nextBreak) throws TestTimeoutException {
    if (nextBreak == null) {
      throw new NullPointerException("nextBreak cannot be null");
    }
    doResume(nextBreak);
    nextBreak.await();
  }

  @Override
  public void resume() {
    doResume(null);
  }

  private void doResume(Breakpoint next) {
    Options.debugPrint("Breakpoint.resume %s\n" , this);
    if (!blocked) {
      throw new IllegalStateException("Cannot resume when not blocked");
    }
    restartLatch.countDown();
  }

  protected final void finishWaiting() {
    stopLatch.countDown();
  }

  /**
   * This method should be invoked by subclasses when the Breakpoint has been
   * hit. It will block the current thread until or unless {@link #resume} is
   * or has been called. Note that calling this method will have no effect if
   * the breakpoint is disabled.
   *
   * @see #isEnabled()
   */
  protected void hitBreakpoint() {
    Options.debugPrint("Breakpoint.at %s, enabled %s\n" , this, enabled);
    if (!enabled) {
      return;
    }
    boolean handled = false;
    if (handler != null) {
      handled = handler.handleBreakpoint(this);
    }
    if (!handled) {
      blocked = true;
      stopLatch.countDown();
      try {
        restartLatch.await();
      } catch (InterruptedException e) {
        throw new IllegalThreadStateException("Interrupted");
      }
    }
  }

  /**
   * Returns a unique integer specifying this breakpoint. Used for debugging, and
   * for implementing toString()
   */
  protected int getId() {
    return id;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + id + ")";
  }
}
