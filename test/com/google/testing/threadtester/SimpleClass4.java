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
 * Simple class used by ScripterTest.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SimpleClass4 {

  volatile int position = 0;

  SimpleClass4() {
  }

  int method1() {
    return 1;
  }

  int method2() {
    return 2;
  }

  int method3() {
    return 3;
  }

  int method4() {
    return 4;
  }

  int methodThatThrows() {
    int x = 1;
    int y = 0;
    int result = x / y;
    return result;
  }
}
