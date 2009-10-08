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

import java.lang.reflect.Method;

/**
 * Represents a position at the point where an instrumented method calls another
 * method.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
abstract class CallCodePosition extends InstrumentedCodePosition {

  /** The method making the call */
  protected final Method method;

  /** The method being called. Null if {@link #calledMethodName} is defined */
  protected final Method calledMethod;

  /** The name of the method being called. Null if {@link #calledMethod} is defined */
  protected final String calledMethodName;

  CallCodePosition(Method method, String calledMethodName) {
    if (method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if (calledMethodName == null) {
      throw new IllegalArgumentException("calledMethodName cannot be null");
    }
    this.method = method;
    this.calledMethodName = calledMethodName;
    this.calledMethod = null;
  }

  CallCodePosition(Method method, Method calledMethod) {
    if (method == null) {
      throw new IllegalArgumentException("method cannot be null");
    }
    if (calledMethod == null) {
      throw new IllegalArgumentException("calledMethod cannot be null");
    }
    this.method = method;
    this.calledMethod = calledMethod;
    this.calledMethodName = null;
  }

  @Override
  boolean matches(CodePosition p) {

    // This method is inherited by AfterCallCodePosition and
    // BeforeCallCodePosition. For both classes, the match returns true if the
    // methods and/or method names match, but an AfterCallCodePosition
    // does not match a BeforeCallCodePosition. Hence we use the
    // isAssignableFrom() test.
    if (p != null && this.getClass().isAssignableFrom(p.getClass())) {
      CallCodePosition position = (CallCodePosition) p;
      if (this.method.equals(position.method)) {
        if (this.calledMethod != null) {
          return this.calledMethod.equals(position.calledMethod) ||
          this.calledMethod.getName().equals(position.calledMethodName);
        } else {
          return this.calledMethodName.equals(position.calledMethodName) ||
          (position.calledMethod != null &&
              this.calledMethodName.equals(position.calledMethod.getName()));
        }
      }
    }
    return false;
  }

  /** Returns the name of the emthod being called */
  protected String getCalledMethodName() {
    return calledMethodName != null ? calledMethodName : calledMethod.getName();
  }
}
