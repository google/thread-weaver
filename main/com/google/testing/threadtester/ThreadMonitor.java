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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Utility class that monitors two threads, and waits for one of them to finish.
 * If it determines that the first thread will not finish because of a monitor
 * lock held by the second thread, it will return a status code to indicate that
 * fact.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ThreadMonitor {
  private static final long JOIN_TIME = 50;
  private static final long MIN_NUM_TRIES = 5;
  private final long numTries;

  private Thread toWait;
  private Thread other;

  private static ThreadMXBean threadBean;
  static {
    threadBean = ManagementFactory.getThreadMXBean();
    threadBean.setThreadContentionMonitoringEnabled(true);
  }

  /**
   * Creates a new ThreadMonitor for the given threads.
   * @param toWait the thread to wait for.
   * @param other the other thread that may hold locks that will prevent the
   * first thread from completing.
   */
  public ThreadMonitor(Thread toWait, Thread other) {
    this.toWait = toWait;
    this.other = other;
    this.numTries = Math.max(Options.timeout() / JOIN_TIME, MIN_NUM_TRIES);
  }

  /**
   * Returns the thread id of the thread that is blocking the input thread. If
   * the input thread is not blocked, returns -1.
   */
  public static long getBlockerId(Thread thread) {
    long blocker = -1;
    Thread.State state = thread.getState();
    if (state == Thread.State.BLOCKED) {

      // Threads are not always accurately marked as blocked. If the thread
      // claims that it is blocked, make sure that it is really waiting on
      // another thread. Sometimes a thread can be marked as bocked waiting in
      // itself, or on nothing.
      //
      // TODO(alasdair): add a mechanism that allows us to specify the desired
      // behaviour.  We could probably remove the call to isAlive() at the
      // beginning of waitForThread(), at the cost of some additional waiting
      // time. If isBlocked() proves flaky, this might be a workaround.
      Options.debugPrint("Thread %s (%d) is %s,\n", thread, thread.getId(), state);
      ThreadInfo info = threadBean.getThreadInfo(thread.getId());
      if (info != null) {
        long lockOwner = info.getLockOwnerId();
        if (lockOwner != -1 && lockOwner != thread.getId()) {
          blocker = lockOwner;
        }
      }
    }
    return blocker;
  }

  private boolean isBlocked(Thread thread) {
    boolean blocked = false;
    Thread.State state = thread.getState();
    if (state == Thread.State.BLOCKED || state == Thread.State.WAITING) {

      // Threads are not always accurately marked as blocked. If the thread
      // claims that it is blocked, make sure that it is really waiting on
      // anothe thread. Sometimes a thread can be marked as bocked waiting in
      // itself, or on nothing.
      //
      // TODO(alasdair): add a mechanism that allows us to specify the desired
      // behaviour.  We could probably remove the call to isAlive() at the
      // beginning of waitForThread(), at the cost of some additional waiting
      // time. If isBlocked() proves flaky, this might be a workaround.
      blocked = true;
      Options.debugPrint("Thread %s (%d) is %s, other = %s\n", thread, thread.getId(), state,
          other.getState());
      if (state == Thread.State.BLOCKED) {
        ThreadInfo info = threadBean.getThreadInfo(thread.getId());
        if (info != null) {
          long lockOwner = info.getLockOwnerId();
          if (lockOwner == -1 || lockOwner == thread.getId()) {
            blocked = false;
          } else if (lockOwner != other.getId()) {
            System.out.printf("WARNING - %s blocked on %s\n", thread, info.getLockName());
          }
        }
      }
    }
    return blocked;
  }

  /**
   * Waits for the first thread to terminate. Returns true if it ran to
   * completion, and false because of a synchronized lock held by the second
   * thread.
   *
   * @throws TestTimeoutException if the thread was not blocked by the seond thread,
   * but still failed to finish.
   */
  public boolean waitForThread() throws InterruptedException, TestTimeoutException {
    if (toWait.getState() == Thread.State.NEW) {
      throw new IllegalThreadStateException("Cannot wait for non-started thread.");
    }
    if (other.isAlive()) {
      if (isBlocked(toWait)) {
        return false;
      }
    }
    for (int tries = 0; tries < numTries; tries++) {
      toWait.join(JOIN_TIME);
      if (!toWait.isAlive()) {
        return true;
      }
      Thread.State state = toWait.getState();
      if (isBlocked(toWait)) {
        return false;
      }
    }
    throw new TestTimeoutException("Thread will not finish", toWait);
  }
}
