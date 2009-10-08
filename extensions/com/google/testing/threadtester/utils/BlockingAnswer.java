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

import com.google.testing.threadtester.AbstractBreakpoint;
import com.google.testing.threadtester.ReusableBreakpoint;

import org.easymock.IAnswer;

/**
 * An implementation of the EasyMock {@link org.easymock.IAnswer} interface that
 * can be used as a {@link com.google.testing.threadtester.Breakpoint}. When the
 * answer is used, the calling thread will break.
 * <p>
 * Sample usage:
 * <pre>
 *   BlockingAnswer<Boolean> answer = new BlockingAnswer<Boolean>(true);
 *   expect(mock.isActive()).andAnswer(answer);
 *   ...
 *   answer.await(); // Thread will block at 'isActive()'
 * </pre>
 *
 * Note that when using a mock that will be accessed in more than on thread,
 * you must invoke {@link org.easymock.EasyMock#makeThreadSafe};
 *
 * @param <T> the type returned from this answer.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class BlockingAnswer<T> extends AbstractBreakpoint implements ReusableBreakpoint,
    IAnswer<T> {

  private T result;

  /**
   * Creates a BlockingAnswer that returns the given value. The underlying
   * BreakPoint will be active for all threads. Note that if a thread is
   * subsequently set by calling {@link #setThread} then the breakpoint will
   * become active for that specific thread.
   */
  public BlockingAnswer(T result) {
    this.result = result;
  }

  /**
   * Creates a BlockingAnswer that returns the given value. The underlying
   * BreakPoint will be active only for the given thread.
   */
  public BlockingAnswer(T result, Thread thread) {
    super(thread);
    this.result = result;
  }

  @Override
  public T answer() {
    if (thread == null || thread.equals(Thread.currentThread())) {
      hitBreakpoint();
    }
    return result;
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  @Override
  public void setThread(Thread thread) {
    setThreadImpl(thread);
  }
}
