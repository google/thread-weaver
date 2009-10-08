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
 * Base class for unit tests for InstrumentedCodePositions
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class InstrumentedCodePositionTest extends TestCase {

  /** Concrete implementation that should not be equal to other code positions. */
  protected static class FakeCodePosition extends InstrumentedCodePosition {
    @Override
    boolean matches(CodePosition p) {
      return false;
    }
  }

  /** Utility test class with some dummy methods */
  protected static class DummyClassWithMethods {
    void method1() {
      // Empty
    }

    void method2() {
      // Empty
    }
  }

  /**
   * Gets a new instance of the class under test. All objects returned
   * by this method should be match.
   */
  protected abstract CodePosition getFirst() throws Exception;

   /**
   * Gets a different new instance of the class under test. All objects returned
   * by this method should match, but should not match the ones returned by
   * {@link #getFirst}.
   */
  protected abstract CodePosition getSecond() throws Exception;

  /**
   * Returns a new instance which should match any other instance of the
   * class under test. May return null if there is no such instance that can be
   * created.
   */
  protected abstract CodePosition getAny() throws Exception;

  public void testDoesMatch() throws Exception {
    CodePosition firstPosition = getFirst();
    CodePosition anotherFirstPosition = getFirst();
    CodePosition secondPosition = getSecond();
    CodePosition anyPosition = getAny();
    FakeCodePosition dummy = new FakeCodePosition();

    assertTrue(firstPosition.matches(firstPosition));
    assertTrue(firstPosition.matches(anotherFirstPosition));
    assertFalse(firstPosition.matches(secondPosition));
    assertFalse(firstPosition.matches(dummy));
    assertFalse(firstPosition.matches(null));
    if (anyPosition != null) {
      assertTrue(firstPosition.matches(anyPosition));
      assertTrue(anyPosition.matches(firstPosition));
      assertFalse(anyPosition.matches(dummy));
    }
  }
}
