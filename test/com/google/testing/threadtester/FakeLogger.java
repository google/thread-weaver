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
import java.util.ArrayList;
import java.util.List;

/**
 * Fake implementation of CallLogger. Used for capturing the calls made by
 * an object instrumented by the TestInstrumenter.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class FakeLogger implements CallLogger {

  enum Type {
    START,
    END,
    BEGIN_CALL,
    END_CALL,
    AT_LINE
  }

  class Record {
    Type type;
    Method caller;
    Method called;
    int line;

    Record(Type type, Method main) {
      this.type = type;
      this.caller =  main;
    }

    Record(Type type, Method caller, int line, Method called) {
      this.type = type;
      this.caller =  caller;
      this.called = called;
      this.line = line;
    }

    Record(int line) {
      this.type = Type.AT_LINE;
      this.line = line;
    }
  }

  private List<Record> records = new ArrayList<Record>();

  List<Record> getRecords() {
    return records;
  }

  public void start(Method method) {
    records.add(new Record(Type.START, method));
  }

  public void end(Method method) {
    records.add(new Record(Type.END, method));
  }

  public void beginCall(Method source, int line, Method target) {
    records.add(new Record(Type.BEGIN_CALL, source, line, target));
  }

  public void endCall(Method source, int line, Method target) {
    records.add(new Record(Type.END_CALL, source, line, target));
  }

  public void atLine(int line) {
    records.add(new Record(line));
  }
}
