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
 * Simple class used by {@link InterleavedRunnerTest}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class SimpleClass5 {

  static List<String> sequence = new ArrayList<String>();

  SimpleClass5() {
  }

  static synchronized List<String> getAndResetSequence() {
    List<String> result = sequence;
    sequence = new ArrayList<String>();
    return result;
  }

  int mainMethod(String arg) {
    sequence.add(arg + 1);
    sequence.add(arg + 2);
    sequence.add(arg + 3);
    return 0;
  }

  int secondMethod(String arg) {
    sequence.add(arg + 5);
    fourthMethod();
    sequence.add(arg + 6);
    fifthMethod();
    sequence.add(arg + 7);
    return 0;
  }

  int thirdMethod(String arg) {
    sequence.add(arg + 8);
    fourthMethod();
    sequence.add(arg + 9);
    fourthMethod();
    sequence.add(arg + 10);
    return 0;
  }

  int fourthMethod() {
    return 0;
  }

  int fifthMethod() {
    return 0;
  }

  void recursive(String arg, int count) {
    sequence.add(arg + (20 + count));
    synchronized(this) {
      if (count > 0) {
        recursive(arg, count-1);
      }
    }
  }
}
