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

package com.google.testing.threadtester.utils;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.makeThreadSafe;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.testing.threadtester.TestThread;
import com.google.testing.threadtester.TestTimeoutException;
import com.google.testing.threadtester.ThrowingRunnable;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests BlockingAnswer.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class BlockingAnswerTest extends TestCase {

  private static final int VALUE = 42;

  /**
   * Mock instance of an InputStream, so that we can create an expectation and a
   * corresponding IAnswer. The InputStream itself has nothing to do with the
   * test - it's just an interface that we can implement.
   */
  InputStream mockStream = createMock(InputStream.class);

  /** Position counter incremented by the test thread. */
  AtomicInteger progress = new AtomicInteger(0);

  /** Test task executed in a separate thread. */
  ThrowingRunnable task;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    mockStream = createMock(InputStream.class);
    makeThreadSafe(mockStream, true);

    task = new ThrowingRunnable() {
      @Override
      public void run() throws Exception {
        progress.incrementAndGet(); // Indicate 1st position
        assertEquals(VALUE, mockStream.available());
        progress.incrementAndGet(); // Indicate 2nd position
      }
    };
  }

  public void testBlockingAnswer_specificThread() throws IOException, TestTimeoutException,
      InterruptedException {

    // Create a thread that will execute the task, and create a BlockingAnswer
    // associated with the thread.
    TestThread thread = new TestThread(task, "BlockingAnswerTest");
    BlockingAnswer<Integer> answer = new BlockingAnswer<Integer>(VALUE, thread);
    try {
      expect(mockStream.available()).andAnswer(answer).times(2);
      replay(mockStream);

      thread.start();
      answer.await();
      assertEquals("Did not reach first position", 1, progress.get());

      answer.resume();
      thread.finish();
      assertEquals("Did not reach second position", 2, progress.get());
    } finally {
      thread.throwExceptionsIfAny();
    }

    try {
      // Now reset the breakpoint, and verify that it can be used again.
      thread = new TestThread(task, "BlockingAnswerTest - reset");
      answer.setThread(thread);
      thread.start();
      answer.await();
      assertEquals("Did not reach first position", 3, progress.get());

      answer.resume();
      thread.finish();
      assertEquals("Did not reach second position", 4, progress.get());
      verify(mockStream);
    } finally {
      thread.throwExceptionsIfAny();
    }
  }

  public void testBlockingAnswer_doesNotBlockOnOtherThread() throws Exception {

    // Create a thread that will be used to execute the task, and another that
    // will not. Set the answer to block in the thread that will not be used.
    TestThread thread = new TestThread(task, "BlockingAnswerTest - right thread");
    TestThread wrongThread = new TestThread(task, "BlockingAnswerTest - wrong thread");
    BlockingAnswer<Integer> answer = new BlockingAnswer<Integer>(VALUE, wrongThread);

    expect(mockStream.available()).andAnswer(answer);
    replay(mockStream);

    // Execute the task in the correct thread. It should not block.
    try {
      thread.start();
      thread.finish();
      assertEquals("Did not reach second position", 2, progress.get());
      verify(mockStream);
    } finally {
      thread.throwExceptionsIfAny();
    }
  }

  public void testBlockingAnswer_doesNotBlockWhenDisabled() throws Exception {

    TestThread thread = new TestThread(task, "BlockingAnswerTest - right thread");
    BlockingAnswer<Integer> answer = new BlockingAnswer<Integer>(VALUE, thread);
    answer.disable();
    expect(mockStream.available()).andAnswer(answer);
    replay(mockStream);

    // Execute the task in the thread. It should not block, as the answer has
    // been disabled.
    try {
      thread.start();
      thread.finish();
      assertEquals("Did not reach second position", 2, progress.get());
      verify(mockStream);
    } finally {
      thread.throwExceptionsIfAny();
    }
  }
}
