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
 * Represents a position within an instrumented method, at the start of a call
 * to another method.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class BeforeCallCodePosition extends CallCodePosition {

  /**
   * Creates a position based on a called method object.
   *
   * @param method the method making a call
   * @param calledMethod the method object being called
   * @see ClassInstrumentation#beforeCall(Method, Method)
   */
  BeforeCallCodePosition(Method method, Method calledMethod) {
    super(method, calledMethod);
  }

  /**
   * Creates a position based on the name of a called method.
   *
   * @param method the method making a call
   * @param calledMethodName the name of the method being called
   * @see ClassInstrumentation#beforeCall(String, String)
   */
  BeforeCallCodePosition(Method method, String calledMethodName) {
    super(method, calledMethodName);
  }

  @Override
  public String toString() {
   return "in " + method.getName() + ", before calling " + getCalledMethodName();
  }
}
