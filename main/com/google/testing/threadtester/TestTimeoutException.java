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
 * Exception throw by the test framework if a test operation does not complete
 * during the time specified by {@link Options#timeout}. A TestTimoutException
 * is associated with a given thread. This is the thread that was performing
 * the operation that timed out, not the thread in which the exception is thrown.
 * For example, if a call to {@link TestThread#finish()} throws a
 * TestTimeoutException, the associated thread will be the TestThread that
 * failed to finish, not the thread that called <code>finish()</code>.
 *
 * @see TestThread#finish
 * @see Breakpoint#await
 * @see Stepper#step
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class TestTimeoutException extends Exception {

  private final Thread thread;

  /**
   * Creates a new exception after a timeout in the given thread.
   */
  public TestTimeoutException(Thread thread) {
    super();
    if (thread == null) {
      throw new IllegalArgumentException("thread cannot be null");
    }
    this.thread = thread;
  }

  /**
   * Creates a new exception after a timeout in the given thread, with the given
   * message.
   */
  public TestTimeoutException(String message, Thread thread) {
    super(message);
    if (thread == null) {
      throw new IllegalArgumentException("thread cannot be null");
    }
    this.thread = thread;
  }

  /**
   * Gets the thread that was executing the operation that timed out.
   */
  public Thread getThread() {
    return thread;
  }
}
