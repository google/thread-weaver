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

import java.util.IdentityHashMap;

/**
 * Factory for creating FakeLogger instances for instrumented objects.
 * See {@link TestInstrumenterTest}
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class FakeLoggerFactory {

  private static IdentityHashMap<Object, FakeLogger> loggerMap =
      new IdentityHashMap<Object, FakeLogger>();
  private static IdentityHashMap<Object, Integer> numCallsMap =
      new IdentityHashMap<Object, Integer>();

  static FakeLogger getLogger(Object object) {
    FakeLogger logger = loggerMap.get(object);
    if (logger == null) {
      throw new IllegalArgumentException("No logger for " + System.identityHashCode(object));
    }
    return logger;
  }

  static Integer getNumCalls(Object object) {
    Integer result = numCallsMap.get(object);
    if (result == null) {
      throw new IllegalArgumentException("No result for " + System.identityHashCode(object));
    }
    return result;
  }

  public static CallLogger createLoggerForNewObject(Object newObject) {
    FakeLogger logger = loggerMap.get(newObject);
    if (logger == null) {
      logger = new FakeLogger();
      loggerMap.put(newObject, logger);
      numCallsMap.put(newObject, Integer.valueOf(0));
    }
    Integer numCalls = numCallsMap.get(newObject);
    numCallsMap.put(newObject, Integer.valueOf(numCalls + 1));
    return logger;
  }
}
