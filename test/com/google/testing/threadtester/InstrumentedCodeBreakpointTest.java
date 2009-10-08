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

import junit.framework.TestCase;

/**
 * Unit test for InstrumentedCodeBreakpoint. Note that in addition to the unit
 * test there are a number of integration tests that verify that breakpoints can
 * be used to control thread execution.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InstrumentedCodeBreakpointTest extends TestCase {

  /**
   * The main code position. This is created by calling
   * {@link #createCodePosition} during {@link #setUp}.
   */
  protected CodePosition mainPosition;

  /**
   * The breakpoint under test. This is created by calling
   * {@link #createBreakPoint} during {@link #setUp}.
   */
  protected InstrumentedCodeBreakpoint breakpoint;

  /**
   * Provides a concrete implementation of InstrumentedCodeBreakpoint.
   */
  private class FakeBreakpoint extends InstrumentedCodeBreakpoint {

    private CodePosition position;

    public FakeBreakpoint(Thread thread, CodePosition cp) {
      super(thread);
      this.position = cp;
    }

    @Override
    protected boolean doesMatch(CodePosition otherPosition) {
      return position != null && position.equals(otherPosition);
    }

    @Override
    protected String getPositionDescription() {
      return "";
    }
  }

  private class FakePosition extends CodePosition {

    @Override
    protected boolean matches(CodePosition other) {
      return this.equals(other);
    }
  }

  /**
   * Initialises {@link #mainPosition} and {@link #breakpoint}.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    mainPosition = createCodePosition();
    breakpoint = createBreakPoint();
  }

  /** Creates the main CodePosition associated with the breakpoint under test */
  protected final CodePosition createCodePosition() {
    return new FakePosition();

  }

  /** Creates the breakpoint under test. */
  protected InstrumentedCodeBreakpoint createBreakPoint() {
    return new FakeBreakpoint(Thread.currentThread(), mainPosition);
  }

  public void testMatches_returnsFalseWhenNotEnabled() {
    breakpoint.disable();
    assertFalse(breakpoint.matches(mainPosition));
  }
}
