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
 * A CodePosition that matches a specific line of code in the instrumented
 * class.
 */
class LineCodePosition extends InstrumentedCodePosition {

  private final int lineNumber;

  /**
   * Creates a new LineCodePosition for the given line. If the line is
   * <code>0</code>, this position is considered to match any line of code,
   * and the {@link InstrumentedCodeBreakpoint} created by this class will
   * stop as soon as any line of code is executed.
   */
  LineCodePosition(int lineNumber) {
    if (lineNumber < 0) {
      throw new IllegalArgumentException("Invalid line number " + lineNumber);
    }
    this.lineNumber = lineNumber;
  }

  @Override
  boolean matches(CodePosition p) {
    if (p instanceof LineCodePosition) {
      LineCodePosition position = (LineCodePosition) p;
      return lineNumber == position.lineNumber || lineNumber == 0 || position.lineNumber == 0;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
   return "at line " + lineNumber;
  }
}
