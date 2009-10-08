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

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Unit tests for MethodCaller.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class MethodCallerTest extends TestCase {

  private static final String ARG = "Hello";

  static class ClassWithSomeMethods {
    String methodArg = null;
    ClassWithSomeMethods() {
    }

    public String method1(String arg) {
      methodArg = arg;
      return arg;
    }

    public void method2() throws IOException {
      throw new IOException();
    }

    public void method3() throws NullPointerException {
      throw new NullPointerException();
    }
  }

  public void testGetClass() {
    assertEquals(MethodCallerTest.class, MethodCaller.getClass(MethodCallerTest.class.getName()));
  }

  public void testGetClass_notFound() {
    try {
      MethodCaller.getClass("");
      fail();
    } catch (RuntimeException e) {
      // expected
    }
  }

  public void testNewInstance() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    assertTrue(instance != null && instance.getClass() == ClassWithSomeMethods.class);
  }

  public void testInvoke() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method1", String.class);
    Object result = MethodCaller.invoke(m, instance, ARG);
    assertTrue(ARG.equals(result));
    assertTrue(ARG.equals(instance.methodArg));
  }

  public void testInvokeAndThrow_handlesNullException() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method1", String.class);
    // If the exception parameter is null, this is equivalent to no exception being expected.
    Object result = MethodCaller.invokeAndThrow(m, instance, null, ARG);
    assertTrue(ARG.equals(result));
    assertTrue(ARG.equals(instance.methodArg));
  }

  public void testInvokeAndThrow_throwsWhenNoExpectedExceptionThrown() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method1", String.class);
    try {
      // Use the version of invoke that takes an expected exception
      MethodCaller.invokeAndThrow(m, instance, ClassNotFoundException.class, ARG);
      fail();
    } catch (RuntimeException e) {
      // We expect to catch a RuntimeException if the invoked method did not
      // throw any exception.
    }
  }

  public void testInvokeAndThrow_throwsWhenWrongExceptionThrown() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method2");
    try {
      // Use the version of invoke that takes an expected exception
      MethodCaller.invokeAndThrow(m, instance, NoSuchMethodException.class);
      fail();
    } catch (RuntimeException e) {
      // We expect to catch a RuntimeException if the invoked method did not
      // throw the specified exception type
    }
  }

  public void testInvokeAndThrow_handlesExpectedException() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method2");
    // Use the version of invoke that takes an expected exception
    MethodCaller.invokeAndThrow(m, instance, IOException.class);
  }

  public void testInvokeAndThrow_handlesExpectedRuntimException() {
    ClassWithSomeMethods instance = MethodCaller.newInstance(ClassWithSomeMethods.class);
    Method m = MethodCaller.getDeclaredMethod(ClassWithSomeMethods.class, "method3");
    // Use the version of invoke that takes an expected exception
    MethodCaller.invokeAndThrow(m, instance, NullPointerException.class);
  }
}
