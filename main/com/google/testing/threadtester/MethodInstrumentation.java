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
import java.util.List;

/**
 * Contains information about a method that has been instrumented.
 *
 * @see ClassInstrumentation
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface MethodInstrumentation {

  /**
   * Returns the name of the method.
   */
  String getName();

  /**
   * Returns the Java Method object corresponding to this method.
   */
  Method getUnderlyingMethod();

  /**
   * Returns the lines in this method.
   */
  List<LineInstrumentation> getLines();

  /**
   * Returns the number of executable lines in the method.
   */
  int getNumLines();
}
