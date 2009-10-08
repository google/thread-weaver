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
 * A Breakpoint that will break at any position inside an instrumented class.
 * Used for stepping through instrumented code a line at a time.
 *
 * @see Stepper
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class AnyPositionBreakpoint extends InstrumentedCodeBreakpoint {

  AnyPositionBreakpoint(Thread thread) {
    super(thread);
  }

  @Override
  protected boolean doesMatch(CodePosition otherPosition) {
    return otherPosition != null;
  }

  @Override
  protected String getPositionDescription() {
    return "any position";
  }
}
