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
 * A Breakpoint designed for repeated use. The normal Breakpoints created by
 * {@link ObjectInstrumentation} are single-shot objects, which can only be used
 * once. An ReusableBreakpoint is designed to be used multiple times. Typical
 * implementations of ReusableBreakpoint will be based on mechanisms other than
 * bytecode instrumentation.
 * <p>
 * An ReusableBreakpoint is normally used in conjunction with an {@link
 * InterleavedRunner}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface ReusableBreakpoint extends Breakpoint {

  /**
   * Sets the thread associated with this breakpoint. Invoking this method will
   * also reset other internal state. This method may be invoked before a
   * breakpoint is used, or after a thread has called {@link #await()} followed
   * by {@link #resume()}.
   */
  void setThread(Thread thread);
}
