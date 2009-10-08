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

/**
 * Interface passed to the {@link InterleavedRunner} that represents the main
 * thread of execution.
 *
 * @param <T> the type under test. It is expected that the runnable will invoke
 * a method on this class.
 *
 * @see InterleavedRunner
 * @see SecondaryRunnable
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface MainRunnable<T> extends ThrowingRunnable {

  /**
   * Returns the class being tested by this runnable. The class should be
   * instrumented. If this runnable is not testing an instrumented class, then
   * this method may return null.
   *
   * @see ClassInstrumentation
   * @see Instrumentation
   */
  public Class<T> getClassUnderTest();

  /**
   * Gets the name of the method being tested by this runnable. The runnable is
   * expected to call this method during its own {@link java.lang.Runnable#run}
   * method. If this runnable is not testing an instrumented class, then this
   * method may return null. May return null if {@link #getMethod()} does not.
   */
  public String getMethodName();

  /**
   * Gets the method being tested by this runnable. The runnable is expected to
   * call this method during its own {@link TestThread#run} method. If
   * this runnable is not testing an instrumented class, then this method may
   * return null.
   * <p>
   * May throw a NoSuchMethodException if the method cannot be found. This will
   * cause the InterleavedRunner to throw an IllegalArgumentException. May
   * return null if {@link #getMethodName()} does not.
   */
  public Method getMethod() throws NoSuchMethodException;

  /**
   * Invoked by the framework before running this runnable. This method will be
   * called once at the start of each iteration. Any object creation or
   * initialization code can be placed here. Any exceptions thrown will be
   * passed back to the caller of {@link InterleavedRunner#interleave}.
   */
  public void initialize() throws Exception;

  /**
   * Gets the main object being tested. The runnable is expected to invoke the
   * test method (see {@link #getMethod} on this object. This method will not be
   * called until {@link #initialize} has been called. Typical implementations
   * will create a new instance of the test object in {@link #initialize}, and
   * return it here.
   *
   * @see SecondaryRunnable#initialize
   */
  public T getMainObject();

  /**
   * Invoked by the framework after running this runnable. Any test verification
   * code can be placed here. Note that any exceptions thrown will be passed
   * back to the caller of {@link InterleavedRunner#interleave}.
   *
   * @see RunResult
   */
  public void terminate() throws Exception;

}


