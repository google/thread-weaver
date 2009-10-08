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
 * Represents a point of execution within the instrumented code of a given
 * class. A {@link Breakpoint} can be created which will cause a thread to stop
 * at a given CodePosition.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
abstract class InstrumentedCodePosition extends CodePosition {

  /**
   * Creates a new breakpoint at this position, associated with the given thread.
   * This is used by {@link ObjectInstrumentationImpl} when creating new breakpoints
   * for an instrumented object.
   */
  InstrumentedCodeBreakpoint createBreakpoint(Thread thread) {
    return new SinglePositionBreakpoint(thread, this);
  }
}
