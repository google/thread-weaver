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
 * Interface passed to the {@link InterleavedRunner} that represents the
 * secondary thread of execution.
 *
 * @param <T> the type under test. It is expected that the runnable will invoke a
 * method on this class.
 *
 * @param <M> the typed MainRunnable corresponding to this secondary runnable.
 *
 * @see InterleavedRunner
 * @see MainRunnable
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface SecondaryRunnable<T, M extends MainRunnable<T>> extends ThrowingRunnable {

  /**
   * Invoked by the framework before running this runnable. This method will be
   * called once at the start of each interation. Any object creation or
   * initialization code can be placed here. Note that this method is invoked
   * after the initialize method of the corresponding {@link MainRunnable}. Any
   * exceptions thrown will be passed back to the caller of {@link
   * InterleavedRunner#interleave}.
   */
  public void initialize(M main) throws Exception;

  /**
   * Invoked by the framework after running this runnable. Any test verification
   * code can be placed here. Note that any exceptions thrown will be passed back
   * to the caller of {@link InterleavedRunner#interleave}.
   *
   * @see RunResult
   */
  public void terminate() throws Exception;

  /**
   * Determines whether this runnable is allowed to block during the test. On
   * many tests, the secondary runnable can be blocked if the main runnable is
   * in a synchronized section of code. This is normally the desired behaviour,
   * and the InterleavedRunner will handle this. However, if this method returns
   * false, then the secondary is expected to run to completion without
   * blocking, and the InterleavedRunner will throw an exception if the
   * secondary blocks.
   */
  public boolean canBlock();

}


