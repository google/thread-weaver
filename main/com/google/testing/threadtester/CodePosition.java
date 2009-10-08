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
 * Represents a point of execution within the code of a given class. A {@link
 * Breakpoint} can be created which will cause a thread to stop at a given
 * CodePosition. A CodePosition is typically created by a factory of some kind.
 *
 * @see ClassInstrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public abstract class CodePosition {

  /**
   * Returns true if this position is the same as the other position. Note that
   * in general, positions are only considered the same if they are generated in
   * the same way, and are of the same class. For example, a position that
   * represents a specific source line will not match a position that represents
   * a call to a specific method, even if they occur at the same place in the
   * source code. A position will never match a null <code>other</code> position.
   * <p>
   * This method is used by the framework when determining whether a breakpoint
   * has been hit. See {@link InstrumentedCodeBreakpoint}.
   */
  abstract boolean matches(CodePosition other);
}
