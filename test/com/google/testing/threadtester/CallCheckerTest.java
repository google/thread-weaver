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


import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Tests {@link CallChecker} by analysing the calls made by {@link
 * CallCheckerClass}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class CallCheckerTest extends TestCase {

  public void testCallChecker() throws Exception {
    doCallChecker(CallCheckerClass.class, true);
  }

  static class CallCheckerClassSubclass extends CallCheckerClass {
    // Simple subclass of CallCheckerClass. We verify that CallChecker behaves
    // the same when processing this class as when processing its parent, except
    // for method3(), which is overridden.
    @Override
    public void method3() {
    }
  }

  public void testCallCheckerSubclass() throws Exception {
    doCallChecker(CallCheckerClassSubclass.class, false);
  }

  private void doCallChecker(Class<?> callingClass, boolean expectMethod3) throws Exception {
    CallChecker checker = new CallChecker();
    Map<Method, Method> calls =
      checker.getCallers(callingClass, SimpleClass.class, SimpleClass2.class);

    // We should record the first method in SimpleClass or SimpleClass2 called
    // by each of the methods in CallCheckerClass. We shouldn't record any calls
    // to SimpleClass3.
    assertEquals(expectMethod3 ? 4 : 3, calls.size());

    Method called1 = calls.get(CallCheckerClass.class.getDeclaredMethod("method1", int.class));
    assertEquals(SimpleClass.class.getDeclaredMethod("add", int.class, int.class), called1);

    Method called2 = calls.get(CallCheckerClass.class.getDeclaredMethod("method1"));
    assertEquals(SimpleClass.class.getDeclaredMethod("add", int.class, int.class), called2);

    Method called3 = calls.get(CallCheckerClass.class.getDeclaredMethod("method2"));
    assertEquals(SimpleClass2.class.getDeclaredMethod("getValue"), called3);
    if (expectMethod3) {
      Method called4 = calls.get(CallCheckerClass.class.getDeclaredMethod("method3"));
      assertEquals(SimpleClass2.class.getDeclaredMethod("getValue"), called4);
    }
  }

  /**  Overrides a single method in SimpleClass */
  static class SimpleSubclass extends SimpleClass {
    @Override
    public int add(int a, int b) {
      return a + b;
    }
  }

  /**  Invokes a single method in SimpleSublass */
  static class SimpleSubclassCaller {
    SimpleSubclass simple;

    public void method1(int arg) {
      simple.add(arg, arg); // This calls SimpleSubclass.add()
      simple.unique();      // This calls SimpleClass.unique()
    }
  }

  public void testCallChecker_handlesCallsToSubclasses() throws Exception {
    CallChecker checker = new CallChecker();
    Map<Method, Method> calls =
      checker.getCallers(SimpleSubclassCaller.class, SimpleSubclass.class, SimpleClass2.class);

    // We should only record the call to SimpleSubclass.add(). The call to unique() will
    // actually delegate to the superclass method.
    assertEquals(1, calls.size());

    Method called1 = calls.get(SimpleSubclassCaller.class.getDeclaredMethod("method1", int.class));
    assertEquals(SimpleSubclass.class.getDeclaredMethod("add", int.class, int.class), called1);
  }
}
