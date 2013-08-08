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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;


import java.util.List;

/**
 * Unit tests for MultiPositionBreakpoint.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class MultiPositionBreakpointTest extends InstrumentedCodeBreakpointTest {

  private CodePosition secondPosition;

  @Override
  public void setUp() throws Exception {
    secondPosition = createCodePosition();
    super.setUp();
  }

  @Override
  protected InstrumentedCodeBreakpoint createBreakPoint() {
    return new MultiPositionBreakpoint(Thread.currentThread(), mainPosition, secondPosition);
  }

  public void testConstructor_throwsExceptionWithNullThread() {
    try {
      MultiPositionBreakpoint bp = new MultiPositionBreakpoint(null, mainPosition, secondPosition);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testConstructor_throwsExceptionWithNullPosition() {
    try {
      MultiPositionBreakpoint bp =
        new MultiPositionBreakpoint(Thread.currentThread(), null, secondPosition);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testDoesMatch() {
    CodePosition other = createMock(CodePosition.class);
    MultiPositionBreakpoint bp = (MultiPositionBreakpoint) breakpoint;
    assertTrue(bp.doesMatch(mainPosition));
    assertTrue(bp.getMatchers().contains(mainPosition));
    assertTrue(bp.doesMatch(secondPosition));
    assertFalse(bp.doesMatch(null));
    assertFalse(bp.doesMatch(other));
  }

  public void testDoesMatch_andMatchersContainsMatcher() {
    CodePosition other = createMock(CodePosition.class);
    MultiPositionBreakpoint bp = (MultiPositionBreakpoint) createBreakPoint();
    assertTrue(bp.doesMatch(mainPosition));
    assertTrue(bp.getMatchers().contains(mainPosition));
  }

  public void testDoesMatch_withMultipleMatchers() {
    // Create two CodePositions which are non-identical but which match each other.
    CodePosition first = createMock(InstrumentedCodePosition.class);
    expect(first.matches(isA(InstrumentedCodePosition.class))).andStubReturn(true);
    CodePosition second = createMock(InstrumentedCodePosition.class);
    expect(second.matches(isA(InstrumentedCodePosition.class))).andStubReturn(true);
    replay(first, second);

    MultiPositionBreakpoint bp =
        new MultiPositionBreakpoint(Thread.currentThread(), first, second);

    assertTrue(bp.doesMatch(first));
    List<CodePosition> matchers = bp.getMatchers();
    assertEquals(2, matchers.size());
    assertTrue(matchers.contains(first));
    assertTrue(matchers.contains(second));
  }

}
