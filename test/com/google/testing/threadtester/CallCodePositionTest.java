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
 * Base class for unit tests for CallCodePositions.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class CallCodePositionTest extends InstrumentedCodePositionTest {

  /**
   * The implementation should return a code position created using a Method
   * object.
   */
  @Override
  protected abstract CallCodePosition getFirst() throws Exception;

  /**
   * The implementation should return a code position created using a Method
   * object.
   */
  @Override
  protected abstract CallCodePosition getSecond() throws Exception;

  /** Equivalent of {@link #getFirst()}, but uses names rather than Method objects */
  protected abstract CallCodePosition getNamedFirst() throws Exception;

  /** Equivalent of {@link #getSecond()}, but uses names rather than Method objects */
  protected abstract CallCodePosition getNamedSecond() throws Exception;

  public void testDoesMatch_withNamedMethods() throws Exception {
    CallCodePosition first = getFirst();
    CallCodePosition namedFirst = getNamedFirst();
    CallCodePosition second = getSecond();
    CallCodePosition namedSecond = getNamedSecond();
    assertTrue(first.matches(namedFirst));
    assertTrue(namedFirst.matches(first));
    assertFalse(first.matches(namedSecond));
    assertFalse(namedSecond.matches(first));
  }
}
