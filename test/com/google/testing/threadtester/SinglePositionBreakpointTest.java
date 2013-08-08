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

import static org.easymock.EasyMock.createMock;


/**
 * Unit tests for SinglePositionBreakpoint.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SinglePositionBreakpointTest extends InstrumentedCodeBreakpointTest {

  @Override
  protected InstrumentedCodeBreakpoint createBreakPoint() {
    return new SinglePositionBreakpoint(Thread.currentThread(), mainPosition);
  }

  public void testConstructor_throwsExceptionWithNullThread() {
    try {
      SinglePositionBreakpoint bp = new SinglePositionBreakpoint(null, mainPosition);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testConstructor_throwsExceptionWithNullPosition() {
    try {
      SinglePositionBreakpoint bp = new SinglePositionBreakpoint(Thread.currentThread(), null);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testDoesMatch() {
    CodePosition other = createMock(CodePosition.class);
    SinglePositionBreakpoint bp = (SinglePositionBreakpoint) breakpoint;
    assertTrue(bp.doesMatch(mainPosition));
    assertFalse(bp.doesMatch(null));
    assertFalse(bp.doesMatch(other));
  }
}
