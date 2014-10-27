/*
 * Copyright 2014 Weaver authors
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
 * Defines which methods are used when runnng tests via an {@link AnnotatedTestRunner}.
 * See {@link BaseThreadedTestRunner#setMethodOption}.
 */
public enum MethodOption {
  /**
   * Only the method called directly by the &#064;ThreadedMain method is tested.
   * This is the normal mod of operation.
   */
  MAIN_METHOD (0),

  /**
   * All methods in the instrumented classes are tested.
   */
  ALL_METHODS (1),

  /**
   * An explicit list of methods is tested.
   */
  LISTED_METHODS (2);

  public final int value;

  MethodOption(int v) {
    this.value = v;
  }

  /**
   * Creates a MethodOption from its integer equivalent.
   */
  static MethodOption fromInt(int v){
    for (MethodOption m: MethodOption.values()) {
      if (m.value == v)
        return m;
    }
    throw new IllegalArgumentException("Invalid value " + v);
  }
}

