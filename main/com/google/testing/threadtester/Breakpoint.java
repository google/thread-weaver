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

/**
 * Represents a request for execution to block at a particular
 * position. Breakpoints are normally obtained by calling {@link
 * ObjectInstrumentation#createBreakpoint(CodePosition, Thread)}, but other classes
 * may also implement the Breakpoint interface.
 *
 * A Breakpoint is a single-shot object. Once execution blocks at a particular
 * Breakpoint, it will not block again when that position is reached a second
 * time. A new Breakpoint must be created.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface Breakpoint {

  /**
   * Gets the thread associated with this Breakpoint, if any. Some Breakpoints
   * may only be valid for a given thread, and will not block when reached by
   * another thread. Consult the documentation for the specific Breakpoint type
   * for details. If this Breakpoint is not associated with a thread, returns
   * null.
   */
  Thread getThread();

  /**
   * Sets a handler for this Breakpoint. If a handler is set, then when the
   * breakpoint is reached it will invoke the handler's {@link
   * BreakpointHandler#handleBreakpoint} method.
   */
  void setHandler(BreakpointHandler handler);

  /**
   * Sets the limit. This determines how many times the breakpoint must be hit
   * before it will stop. The default limit is 1, meaning that the breakpoint
   * will stop as soon as it is reached. Setting the limit to N will mean that
   * the breakpoint will not stop the first N-1 times it is reached.
   * <p>
   * Note that once a breakpoint has stopped, it is no longer active, and will
   * not stop again.
   *
   * @throws IllegalArgumentException if {@code limit < 1}
   * @throws IllegalStateException if this breakpoint has already been reached.
   */
  void setLimit(int limit);

  /**
   * Gets the limit. This is the initial limit value that was set in the call to
   * {@link #setLimit}.
   */
  int getLimit();

  /**
   * Returns true if this breakpoint is enabled. By default, a Breakpoint is
   * active when it is created, but can be deactivated by {@link #disable}. A
   * Breakpoint's thread will only stop if the Breakpoint is enabled.
   */
  boolean isEnabled();

  /**
   * Disables the Breakpoint. If a breakpoint is not enabled, then its thread
   * will not stop when the breakpoint is reached.
   */
  void disable();

  /**
   * Enables the Breakpoint. If a breakpoint is enabled, then its thread will
   * stop when the breakpoint is reached. Breakpoints are enabled when created,
   * but may be disabled by a call to {@link #disable}.
   */
  void enable();

  /**
   * Returns true if the associated thread is currently blocked at this
   * breakpoint.
   */
  boolean isBlocked();

  /**
   * Waits for this breakpoint to be reached. The caller thread will block
   * until the breakpoint is reached. If the executing thread is already
   * stopped at this breakpoint, then this method will return immediately.
   * <p>
   * It is the caller's responsibility to ensure that this breakpoint is
   * reachable in the current environment. Waiting for a breakpoint that is not
   * reached will eventually throw a TestTimeoutException.
   *
   * @throws IllegalStateException if this breakpoint is not currently active
   *
   * @throws TestTimeoutException if this breakpoint is not reached within the
   * time limit defined by {@link Options#timeout}. The thread of the timeout
   * exception will be set to the thread of this breakpoint, not to the thread
   * that called <code>await</code>.
   */
  void await() throws TestTimeoutException;


  /**
   * Tells the blocked thread to continue. This method can only be called after
   * {@link #await} has been successfully called.
   *
   * @throws IllegalStateException if await has not been called.
   */
  void resume();

  /**
   * Tells the blocked thread to continue, and then wait for the given
   * breakpoint. This is a shorthand for:
   * <pre>
   *   breakPoint.resume();
   *   nextBreak.await();
   * </pre>
   */
  void resume(Breakpoint nextBreak) throws TestTimeoutException;

}
