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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A Breakpoint that matches any one of a set of {@link CodePosition}s. This
 * class can be used to allow the executing code to stop at one of several
 * different positions.
 */
class MultiPositionBreakpoint extends InstrumentedCodeBreakpoint {

  private List<CodePosition> positions;
  private List<CodePosition> matchers = new ArrayList<CodePosition>();


  MultiPositionBreakpoint(Thread thread, Collection<CodePosition> positions) {
    super(thread);
    for (CodePosition position : positions) {
      if (position == null) {
        throw new IllegalArgumentException("Cannot add null position");
      }
    }
    this.positions = new ArrayList<CodePosition>(positions);
  }

  MultiPositionBreakpoint(Thread thread, CodePosition pos1, CodePosition pos2) {
    super(thread);
    if (pos1 == null || pos2 == null) {
      throw new IllegalArgumentException("Cannot add null position");
    }
    this.positions = new ArrayList<CodePosition>(2);
    Collections.addAll(this.positions, pos1, pos2);
  }

  @Override
  protected synchronized boolean doesMatch(CodePosition otherPosition) {
    matchers.clear();
    for (CodePosition position : positions) {
      if (position.matches(otherPosition)) {
        matchers.add(position);
      }
    }
    return matchers.size() > 0;
  }

  /**
   * Returns a list of positions that caused this breakpoint to stop. This will
   * normally be a single position, but if several positions were hit simultaneously,
   * they will all be returned. Note that this method should only be called after
   * this breakpoint has been hit.
   */
  synchronized List<CodePosition> getMatchers() {
    return new ArrayList<CodePosition>(matchers);
  }

  @Override
  protected String getPositionDescription() {
    String result = new String();
    for (CodePosition position : positions) {
      result += position;
      result += " ";
    }
    return result;
  }
}
