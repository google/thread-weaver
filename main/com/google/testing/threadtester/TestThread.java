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
 * Implementation of Thread that catches and stores any Exception thrown by the
 * {@link Thread#run} method.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class TestThread extends Thread {
  private volatile Throwable threadException;

  private class ExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      threadException = e;
    }
  }

  private static class TestRunnableWrapper implements Runnable {
    private ThrowingRunnable target;
    public TestRunnableWrapper(ThrowingRunnable target) {
      this.target = target;
    }

    public void run() {
      try {
        target.run();
      } catch (Exception e) {
        // This is a bit hacky, but because the TestRunnableWrapper is created
        // in the constructor, it has to be static, and cannot have a reference
        // to the owning TestThread. However, we control the creation of
        // TestRunnableWrapper, and we know that it must always be executed by a
        // TestThread, so the cast is safe.
        ((TestThread) Thread.currentThread()).threadException = e;
      }
    }
  }

  /**
   * Creates a new TestThread that executes the given Runnable. Any
   * RuntimeExceptions thrown when the runnable runs will be caught.
   */
  public TestThread(Runnable target, String name) {
    super(target, name);
    setHandler();
  }

  /**
   * Creates a new TestThread that executes the given TestRunnable. Any
   * Exceptions thrown when the runnable's {@link ThrowingRunnable#run} method
   * is invoked will be caught.
   */
  public TestThread(ThrowingRunnable target, String name) {
    super(new TestRunnableWrapper(target), name);
    setHandler();
  }

  /**
   * Creates a new TestThread with the given name.
   */
  public TestThread(String name) {
    super(name);
    setHandler();
  }

  private void setHandler() {
    setUncaughtExceptionHandler(new ExceptionHandler());
  }


  /**
   * Waits for this thread to finish. Will not wait longer than {@link
   * Options#timeout}.  Throws an IllegalStateException if this thread has not
   * terminated by the specified time.
   */
  public void finish() throws InterruptedException, TestTimeoutException {
    join(Options.timeout());
    if (getState() != State.TERMINATED) {
      throw new TestTimeoutException("State = " + getState(), this);
    }
  }

  /**
   * Gets the Throwable thrown by the {@link #run} method, or null if there
   * is no such exception.
   */
  public Throwable getException() {
    return threadException;
  }

  /**
   * Rethrows the Throwable thrown by the {@link #run} method, wrapped in a
   * RuntimeException if necessary. If there was no exception thrown, does
   * nothing.
   */
  public void throwExceptionsIfAny() {
    if (threadException != null) {
      if (threadException instanceof RuntimeException) {
        throw (RuntimeException) threadException;
      } else if (threadException instanceof Error) {
        throw (Error) threadException;
      } else {
        throw new RuntimeException(threadException);
      }
    }
  }
}
