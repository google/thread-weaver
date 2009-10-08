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
 * Allows an instrumented class to be stepped through line by line. A Stepper is
 * created by calling {@link ObjectInstrumentation#step}. Typical useage is:
 * <pre>
 *
 *  Breakpoint bp = ...
 *  bp.await();
 *  Stepper stepper = instrumentedObject.step(bp);
 *  while(stepper.hasNext()) {
 *    stepper.step();
 *  }
 *  stepper.resume();
 * </pre>
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface Stepper {

  /**
   * Returns true if this stepper can be stepped again. Typically, this method
   * will return false once the Stepper has advanced to the end of an
   * instrumented method, and is about to return into non-instrumented code.
   */
  boolean hasNext();

  /**
   * Advances a stepper a single line. If the stepper cannot be stepped again
   * (i.e. if {@link #hasNext} returns false) then this method is equivalent to
   * calling {@link #resume}.
   */
  void step() throws TestTimeoutException;

  /**
   * Causes the Stepper's thread to continue running. Note that this method
   * should only be used when {@link #hasNext} has returned false. Otherwise,
   * use {@link #step}.
   *
   * @see Breakpoint#resume
   */
  void resume() throws TestTimeoutException;
}


