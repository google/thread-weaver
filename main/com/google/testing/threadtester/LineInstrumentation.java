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

import java.util.Collections;
import java.util.List;

/**
 * Represents a single source line in an instrumented class. A line may have one
 * or more method invocations in it.
 */
public class LineInstrumentation {

  /* TODO(alasdair): determine what we need in this class. Address initial
   * review comments if we decide we need the called classes.
   */

  private int lineNumber;
  private List<String> calledClasses;
  private List<String> calledMethods;

  public LineInstrumentation(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public LineInstrumentation(int lineNumber, List<String> calledClasses,
      List<String> calledMethods) {
    this.lineNumber = lineNumber;
    this.calledClasses = calledClasses;
    this.calledMethods = calledMethods;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setCalledClasses(List<String> calledClasses) {
    this.calledClasses = calledClasses;
  }

  public List<String> getCalledClasses() {
    if (calledClasses == null) {
      return Collections.emptyList();
    } else {
      return calledClasses;
    }
  }

  public void setCalledMethods(List<String> calledMethods) {
    this.calledMethods = calledMethods;
  }

  public List<String> getCalledMethods() {
    if (calledMethods == null) {
      return Collections.emptyList();
    } else {
      return calledMethods;
    }
  }
}
