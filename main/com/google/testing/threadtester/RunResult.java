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
 * Represents the result of running an {@link InterleavedRunner}.
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 *
 */
public class RunResult {
  private final Throwable mainException;
  private final Throwable secondaryException;

  public RunResult(){
    this(null, null);
  }

  public RunResult(Throwable main, Throwable secondary){
    this.mainException = main;
    this.secondaryException = secondary;
  }

  public boolean hadException() {
    return mainException != null || secondaryException != null;
  }

  /**
   * Throws the main and/or secondary exceptions if they are non-null. If both exceptions
   * are null, does nothing. The exceptions are wrapped in a RuntimeException.
   */
  public void throwExceptionsIfAny() throws RuntimeException {
    if (mainException != null) {
      propagate(mainException);
    }
    if (secondaryException != null) {
      propagate(secondaryException);
    }
  }

  /** Throws the given exception, wrapped in a RuntimeException if necessary */
  private void propagate(Throwable exception) {
    if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    } else if (exception instanceof Error) {
      throw (Error) exception;
    } else {
      throw new RuntimeException(exception);
    }
  }

  /** Gets the exception thrown by the {@link MainRunnable} */
  public Throwable getMainException() {
    return mainException;
  }

  /** Gets the exception thrown by the {@link SecondaryRunnable} */
  public Throwable getSecondaryException() {
    return secondaryException;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " main=" + mainException + ", second=" + secondaryException;
  }
}
