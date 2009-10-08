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

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class used by ScripterTest.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SimpleClass3 {

  private List<String> sequence = new ArrayList<String>();
  SimpleClass4 second;

  SimpleClass3() {
    second = new SimpleClass4();
  }

  synchronized List<String> getSequence() {
    return sequence;
  }

  int mainMethod(String arg) {
    sequence.add(arg + 1);
    second.method1();
    sequence.add(arg + 2);
    second.method2();
    sequence.add(arg + 3);
    second.method3();
    sequence.add(arg + 4);
    return 0;
  }

  int mainMethodWithSyncBlock(String arg) {
    sequence.add(arg + 1);
    synchronized (this) {
      int x = 5;
      second.method1();
      int y = 6;
      int z = x + y;
    }
    sequence.add(arg + 2);
    second.method2();
    sequence.add(arg + 3);
    second.method3();
    sequence.add(arg + 4);
    return 0;
  }


  synchronized int mainMethodSynchronized(String arg) {
    sequence.add(arg + 1);
    int x = 5;
    second.method1();
    int y = 6;
    int z = x + y;
    sequence.add(arg + 2);
    //second.methodThatThrows();
    sequence.add(arg + 3);
    second.method4();
    sequence.add(arg + 4);
    return 0;
  }


  int secondMethod(String arg) {
    sequence.add(arg + 5);
    second.method1();
    sequence.add(arg + 6);
    second.method2();
    sequence.add(arg + 7);
    second.method3();
    sequence.add(arg + 8);
    return 0;
  }
}
