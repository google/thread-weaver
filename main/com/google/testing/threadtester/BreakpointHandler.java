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
 * A handler that can be added to a {@link Breakpoint}. When the breakpoint is
 * reached, the handler is invoked. The handler may simply be used as a logger,
 * but it also has the option of replacing the breakpoint logic entirely, by
 * returning <code>true</code> from {@link #handleBreakpoint(Breakpoint)}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface BreakpointHandler {

  /**
   * Invoked when the breakpoint is reached. This method will be called in the
   * thread that executed the code that triggered the breakpoint.
   * <p>
   * Implementations of this method should return true if the breakpoint has
   * been entirely processed by this handler. Returning true means that no
   * further action will be taken, and the thread that hit the Breakpoint will
   * not stop.  Returning false means that normal Breakpoint processing should
   * continue, and that the thread that hit the Breakpoint should block. Note that
   * if another thread is blocked waiting on this breakpoint (via a call to
   * {@link Breakpoint#await()} that that blocked thread will not be woken.
   * <p>
   * Note that a handler may set up its own internal blocking mechanism, waiting
   * for a particular signal before continuing. In such a setup, the handler
   * will normally return false when it continues, in order to avoid the
   * Breakpoint's own blocking mechanism from being used.
   *
   * @return  true if the breakpoint has been entirely processed by this handler,
   * and no further action should be taken.
   */
  boolean handleBreakpoint(Breakpoint breakpoint);
}
