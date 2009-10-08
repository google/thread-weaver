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
 * Concrete implementation of Stepper. This class is used by (@link
 * ObjectInstrumentationImpl} to provide Stepper functionality. It should not
 * normally be used elsewhere.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class StepperImpl implements Stepper {

  private final ObjectInstrumentationImpl<?> object;
  private volatile Breakpoint breakPoint;
  private volatile boolean hasNext;

  /**
   * Creates a new StepperImpl for the given {@link ObjectInstrumentation}.
   */
  StepperImpl(ObjectInstrumentationImpl<?> object, Breakpoint breakpoint, boolean hasNext) {
    this.object = object;
    this.breakPoint = breakpoint;
    this.hasNext = hasNext;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  /**
   * Sets the state to determines whether this stepper can be stepped again.
   * @see #hasNext
   */
  void setHasNext(boolean newHasNext) {
    this.hasNext = newHasNext;
  }

  /**
   * Returns the current breakpoint where this stepper is currently stopped.
   *
   */
  Breakpoint getCurrentBreakpoint() {
    return this.breakPoint;
  }

  /** Sets the breakpoint where this stepper is currently stopped.*/
  void setCurrentBreakpoint(Breakpoint newBreakpoint) {
    this.breakPoint = newBreakpoint;
  }

  @Override
  public void step() throws TestTimeoutException {
    if (!hasNext) {
      resume();
    } else {
      object.stepFromStepper(this);
    }
  }

  @Override
  public void resume() {
    breakPoint.resume();
  }
}


