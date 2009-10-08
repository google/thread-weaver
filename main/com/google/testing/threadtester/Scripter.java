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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a sequence of {@link Script}s. One Script is defined as the main
 * script, and will be executed first. One or more secondary Scripts are
 * also defined. It is expected that the main script will had control to
 * one of the secondary scripts.  A Script consists of a sequence of one
 * or more {@link ScriptedTask}s. Each task typically performs an operation
 * on the object-under-test.
 *
 * @param <T> the type of object-under-test being scripted.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class Scripter<T> {

  /** The list of scripts being run. */
  private List<Script<T>> scripts = new ArrayList<Script<T>>();

  /** The list of threads corresponding to the scripts. */
  private List<TestThread> threads;

  /** Set to true when this scripter has started. */
  private volatile boolean started;

  /** Set to true when this scripter has finished. */
  private volatile boolean finished;

  /** Represents  */
  private static class ReleaseEvent<T> {
    static AtomicInteger scount = new AtomicInteger(0);

    /** An id, used for diagnostics */
    final int count;

    /**
     * The script that released control. Will be null when the first
     * script starts (becasue no-one released control to the first script)
     * and at the end, when when the sequence of scripts has terminated.
     */
    final Script<T> fromScript;

    /**
     * The script that control was released to. Will be null when the
     * sequence of scripts has terminated.
     */
    final Script<T> toScript;

    ReleaseEvent(Script<T> fromScript, Script<T> toScript) {
      this.fromScript = fromScript;
      this.toScript = toScript;
      this.count = scount.incrementAndGet();
    }
  }

  /**
   * Queue of events. Every time control is released from one script
   * to another, we add a a new event to the queue. When the sequence
   * finishes, a final event with null values will be added.
   */
  private LinkedBlockingQueue<ReleaseEvent<T>> releaseQueue =
      new LinkedBlockingQueue<ReleaseEvent<T>>();

  /**
   * Creates a new Scripter that will run the given main script and
   * the given secondary script.
   */
  public Scripter(Script<T> main, Script<T> secondary) {
    if (main == null) {
      throw new IllegalArgumentException("Main script cannot be null");
    }
    if (secondary == null) {
      throw new IllegalArgumentException("Secondary script cannot be null");
    }
    scripts.add(main);
    scripts.add(secondary);
  }

  /**
   * Creates a new Scripter that will run the given main and secondary scripts.
   */
  public Scripter(Script<T> main, Collection<Script<T>> secondaries) {
    if (main == null) {
      throw new IllegalArgumentException("Main thread cannot be null");
    }
    if (secondaries.size() == 0) {
      throw new IllegalArgumentException("Must specify secondary script(s)");
    }
    for (Script secondary : secondaries) {
      if (secondary == null) {
        throw new IllegalArgumentException("Secondary scripts cannot be null");
      }
    }
    scripts.add(main);
    scripts.addAll(secondaries);
  }

  /**
   * Executes the scripts defined in the constructor. The tasks belonging to the
   * main script will be executed first. When the main script releases to
   * another script, that other script will run.
   * <p>
   * This method will wait until the first script finishes all of its tasks.
   * Once one script has finished,
   */
  public void execute() throws Exception {
    if(started) {
      throw new IllegalStateException("Can only execute once");
    }
    started = true;
    // Create a new Thread for each Script.
    threads = new ArrayList<TestThread>(scripts.size());
    for (int i = 0; i < scripts.size(); i++) {
      final Script<T> script = scripts.get(i);
      ThrowingRunnable runnable = new ThrowingRunnable() {
        @Override
        public void run() throws Exception{
          script.runTasks();
        }
      };
      TestThread thread = new TestThread(runnable, "Script " + (i + 1));
      threads.add(thread);
      script.prepare(this, thread);
      thread.start();
    }

    // Release the first script, which will start it running.
    Options.debugPrint("Scripter: Starting main thread\n");
    Script<T> main = scripts.get(0);
    main.resume();

    // Monitor the releases from script to script. This will return when all
    // scripts have finished.
    monitorReleases(new ReleaseEvent<T>(null, main));
    Options.debugPrint("Scripter: Done all steps\n");

    // Ensure that all other scripts have finished, by allowing them to
    // restart if they are blocked.
    for (Script<T> script : scripts) {
      script.resume();
    }

    // Wait for all script threads to finish.
    for (Script<T> script : scripts) {
      TestThread scriptThread = script.getThread();
      scriptThread.finish();
    }

    for (TestThread thread : threads) {
      thread.throwExceptionsIfAny();
    }
  }

  /**
   * Tracks the releases from one script to another. Keeps monitoring
   * releases until it encounters an event with null entries, indicating
   * that the sequence has terminated.
   */
  private void monitorReleases(ReleaseEvent<T> event) throws InterruptedException,
      TestTimeoutException {
    while (true) {
      event = monitorReleaseEvent(event);
      Options.debugPrint("Scripter: Got release from %s to %s\n", event.fromScript, event.toScript);
      if (event.toScript == null) {
        break;
      }
    }
  }

  /**
   * Monitors an individual {@link ReleaseEvent}. Waits until a new release
   * event has been added to the queue. While waiting, monitors the scripts
   * in the current event to see if a script is blocking the script that it
   * has just released control to. If so, then it steps the blocking script
   * line by line until it releases the lock. Returns the next event in the
   * queue.
   */
  private ReleaseEvent<T> monitorReleaseEvent(ReleaseEvent<T> event)
      throws InterruptedException, TestTimeoutException {
    boolean stepping = false;
    while (true) {
      Options.debugPrint("Scripter: Waiting for next release\n");
      ReleaseEvent<T> nextEvent = releaseQueue.poll(10, TimeUnit.MILLISECONDS);
      if (nextEvent != null) {
        Options.debugPrint("Scripter: Found a release (%d), stepping = %s, to = %s, from = %s\n",
            nextEvent.count, stepping, nextEvent.toScript, event.fromScript);
        if (stepping) {
          if (nextEvent.toScript != event.fromScript) {
            Options.debugPrint("Scripter: to = %s, from = %s\n",
                nextEvent.toScript, event.fromScript);
             throw new IllegalStateException("Got release to " + nextEvent.toScript +
                 " while stepping through " + event.fromScript);
          }
          event.fromScript.finishStepping();
        }
        return nextEvent;
      }

      // We have not found another release event on the queue. This means that
      // the toScript is still running. Check to see if it is not blocked
      // be the script that just released it.
      if (isBlockedOnOther(event.fromScript, event.toScript)) {
        // Thread is blocked by the thread it released to. We need to step
        // through the fromScript thread until it releases the lock.
        Options.debugPrint("Scripter: Thread is blocked by script that released us...\n");
        boolean unblocked = false;
        event.fromScript.startStepping();
        while (releaseQueue.peek() == null && isBlockedOnOther(event.fromScript, event.toScript)) {
          if (!event.fromScript.canStep()) {
            throw new IllegalStateException("Failed to unblock script " + event.fromScript);
          }
          Options.debugPrint("Scripter: Stepping...\n");
          event.fromScript.step();
          stepping = true;
          Options.debugPrint("Scripter: Stepped...\n");
        }
      }
    }
  }

  /**
   * Returns true if a script is blocked by another script due to a
   * monitor lock. This is used to determine whether a script that
   * has just released control to another script is causing that
   * second script to block.
   *
   * @param fromScript the script that has released control, and may be
   *        blocking. This will be null when the first script has started,
   *        and has not yet hande control to another script.
   * @param toScript the script that has been released, and may be blocked
   *        by <code>fromScript</code>
   *
   * @return true if <code>toScript</code> is blocked by <code>fromScript</code>
   */
  private boolean isBlockedOnOther(Script<T> fromScript, Script<T> toScript) {
    long blocker = ThreadMonitor.getBlockerId(toScript.getThread());
    Options.debugPrint("Scripter: toScript " + toScript + " is blocked by %d\n", blocker);
    if (blocker == -1L) {
      // Thread is not blocked - we're happy.
      return false;
    } else if (blocker == Thread.currentThread().getId()) {
      Options.debugPrint("Scripter: Thread is blocked by us...\n");
      return false;
    } else if (fromScript == null) {
      throw new IllegalStateException("First script is blocked by " + blocker);
    } else if (blocker == fromScript.getThread().getId()) {
      return true;
    } else {
      throw new IllegalStateException("Script " + toScript + " is blocked by " + blocker);
    }
  }

  /**
   * Releases control. Invoked by a {@link Script} to release control to
   * another Script. Verifies that the release is valid, and then blocks
   * the first script and allows the other script to resume.
   */
  void release(Script<T> fromScript, Script<T> toScript) {
    Options.debugPrint("****************************\n");
    Options.debugPrint("Releasing from %s to %s\n", fromScript, toScript);

    if (!scripts.contains(toScript)) {
      throw new IllegalArgumentException("Release to unknown " + toScript);
    }
    if (finished) {
      throw new IllegalStateException("Cannot release after a thread has finished");
    }
    toScript.resume();
    try {
      ReleaseEvent<T> evt = new ReleaseEvent<T>(fromScript, toScript);
      Options.debugPrint("Scripter: adding event %d to queue\n", evt.count);

      releaseQueue.put(evt);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Marks a script as finished. Invoked by a script when it has finished
   * executing all its tasks. Adds a special event to the queue.
   */
  void onFinished(Script<T> finishedScript) {
    System.out.printf("Finishing %s\n", finishedScript);
    try {
      releaseQueue.put(new ReleaseEvent<T>(null, null));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
