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
 * Represents a method within an instrumented class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class MethodInstrumentationImpl implements MethodInstrumentation {

  private Method method;
  private List<LineInstrumentation> lines;

  public MethodInstrumentationImpl(Method method,  List<LineInstrumentation> lines) {
    this.method = method;
    this.lines = lines;
  }


  @Override
  public Method getUnderlyingMethod() {
    return method;
  }

  @Override
  public String getName() {
    return method.getName();
  }

  @Override
  public List<LineInstrumentation> getLines() {
    return lines;
  }

  @Override
  public int getNumLines() {
    return lines.size();
  }
}
