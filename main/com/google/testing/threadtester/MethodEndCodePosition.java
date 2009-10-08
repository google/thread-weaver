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
 * A CodePosition at the end of a specific method in an instrumented class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class MethodEndCodePosition extends MethodCodePosition {

  MethodEndCodePosition(Method method) {
    super(method);
  }

  @Override
  boolean matches(CodePosition p) {
    if (p instanceof MethodEndCodePosition) {
      MethodEndCodePosition position = (MethodEndCodePosition) p;
      return this.method.equals(position.method);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
   return "at end of " + method.getName();
  }
}
