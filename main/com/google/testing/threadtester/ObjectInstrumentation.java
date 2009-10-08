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
 * Contains information about an instance of a class that has been instrumented.
 * {@link Breakpoint}s can be created in an instrumented object to control the
 * flow of execution.
 *
 * @param <T> the class that has been instrumented.
 *
 * @see ClassInstrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface ObjectInstrumentation<T> {

  /**
   * Gets the underlying object
   */
  public T getUnderlyingObject();

  /**
   * Creates a new Breakpoint for the given CodePosition and Thread, both of
   * which must be non-null. The Breakpoint will only be valid when the
   * CodePosition is reached within the given thread, while operating on the
   * underlying object. (In other words, when
   * 'this' == {@link #getUnderlyingObject}.
   */
  public Breakpoint createBreakpoint(CodePosition position, Thread thread);

  /**
   * Steps to the next executable line from a given breakpoint, assuming that
   * breakpoint has already been reached. Returns a Stepper that can be used to
   * continue stepping.
   *
   * @throws IllegalThreadStateException if the given breakpoint has not already
   * been reached.
   *
   * @see Breakpoint#await
   */
  public Stepper step(Breakpoint breakpoint);

}
