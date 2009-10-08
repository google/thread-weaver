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
 * Implementation of {@link SecondaryRunnable} with empty methods. Provided as a
 * convenience class, so that subclasses only need to implement the methods that
 * they need. Note that the implementation of {@link #canBlock} returns true.
 *
 * @param <T> the type under test. It is expected that the runnable will invoke a
 * method on this class.
 * @param <M> the typed MainRunnable corresponding to this secondary runnable.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class  SecondaryRunnableImpl<T, M extends MainRunnable<T>> implements
    SecondaryRunnable<T, M>  {

  @Override
  public void initialize(M main) throws Exception {
    // do nothing
  }

  @Override
  public void terminate() throws Exception {
    // do nothing
  }

  /**
   * Implements {@link SecondaryRunnableImpl#canBlock}.
   *
   * @return true. Subclasses should override this if necessary, although in
   * most cases it is correct for the secondary runnable to block if the main
   * runnable is in a synchronized method.
   */
  @Override
  public boolean canBlock() {
    return true;
  }
}


