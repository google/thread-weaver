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
 * A Breakpoint based on a single CodePosition.
 *
 * @see MultiPositionBreakpoint
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SinglePositionBreakpoint extends InstrumentedCodeBreakpoint {

  private final CodePosition position;

  public SinglePositionBreakpoint(Thread thread, CodePosition position) {
    super(thread);
    if (position == null) {
      throw new IllegalArgumentException("position canot be null");
    }
    this.position = position;
  }

  @Override
  protected boolean doesMatch(CodePosition otherPosition) {
    return position.matches(otherPosition);
  }

  @Override
  protected String getPositionDescription() {
    return position.toString();
  }
}
