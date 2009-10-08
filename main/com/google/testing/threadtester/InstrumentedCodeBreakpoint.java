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
  * A Breakpoint based on a {@link CodePosition} defined within a block of
  * instrumented code.  The Breakpoint will stop when the indicated CodePosition
  * is reached. Concrete subclasses must implement the {@link #doesMatch} method.
  *
  * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
  */
abstract class InstrumentedCodeBreakpoint extends AbstractBreakpoint {

  /** The instrumented object that owns this breakpoint, and for which this breakpoint is valid */
  private volatile ObjectInstrumentationImpl<?> owner = null;

  InstrumentedCodeBreakpoint(Thread thread) {
    super(thread);
  }

  /**
   * Returns true if this Breakpoint matches the given position, and if the
   * breakpoint count has been reached. Calling this method may increment the
   * number of hits. This method is invoked by the instrumented runtime when
   * a potential break position is reached.
   *
   * @see Breakpoint#getLimit
   * @see #getHits
   *
   */
  boolean matches(CodePosition otherPosition) {
    if (enabled && doesMatch(otherPosition)) {
      numHits++;
      return numHits >= limit;
    } else {
      return false;
    }
  }

  /**
   * Returns true if this Breakpoint matches the given CodePosition. This method
   * should be overridden by subclasses.
   */
  protected abstract boolean doesMatch(CodePosition otherPosition);

  /**
   * Gets a String describing the position of this Breakpoint. Used for debugging,
   * and for implementing toString.
   */
  protected abstract String getPositionDescription();


  /**
   * Invoked by the instrumented runtime when this breakpoint is reached.
   * Invoking this method blocks the calling thread until {@link #resume} is
   * called. Note that this method will not be called until {@link #matches}
   * returns true.
   */
  void atBreakpoint(ObjectInstrumentationImpl<?> breakpointOwner) {
    hitBreakpoint();
  }

  /**
   * Sets the owner. This method is invoked by the instrumented object
   * when a new breakpoint is created, and registers the link between the
   * breakpoint and the object that the breakpoint applies to. Do not
   * call this method anywhere else.
   *
   * @See {@link ObjectInstrumentationImpl#registerBreakpoint(InstrumentedCodeBreakpoint)}
   */
  void setOwner(ObjectInstrumentationImpl<?> owner) {
    this.owner = owner;
  }

  /**
   * Returns the owner of this breakpoint.
   * @see #setOwner
   */
  ObjectInstrumentationImpl<?> getOwner() {
    return owner;
  }

  @Override
  public String toString() {
    return "Breakpoint(" + getId() + ") @ " + getPositionDescription();
  }
}
