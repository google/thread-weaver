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
 * Unit tests for LineCodePosition
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class LineCodePositionTest extends InstrumentedCodePositionTest {

  public void testConstructor_throwsExceptionWithNegativeLine() {
    try {
      LineCodePosition position = new LineCodePosition(-1);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

 @Override
  protected CodePosition getFirst() {
    return new LineCodePosition(1);
  }

  @Override
  protected CodePosition getSecond() {
    return new LineCodePosition(2);
  }

  @Override
  protected CodePosition getAny() {
    return new LineCodePosition(0);
  }
}
