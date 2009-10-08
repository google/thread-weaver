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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility class that provides reflection operations on {@link
 * java.lang.reflect.Method}s and wraps any exceptions thrown in a
 * RuntimeException. The various parts of the test framework that use reflection
 * expect to succeed. Any failure to invoke a method is a test failure, so we
 * can just throw a RuntimeException.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class MethodCaller {

  private MethodCaller() {
    // Static class - no constructor
  }

  static Class<?> getClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    }
  }
  static <T> T newInstance(Class<T> clss) {
    try {
      return clss.newInstance();
    } catch (IllegalAccessException e) {
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    }
  }

  static Method getDeclaredMethod(Class<?> clss, String name, Class<?>... args) {
    try {
      return clss.getDeclaredMethod(name, args);
    } catch (NoSuchMethodException e) {
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    }
  }

  static Object invoke(Method method, Object target, Object... args) {
    try {
     return method.invoke(target, args);
    } catch (InvocationTargetException t) {
      Options.debugPrintStackTrace(t);
      throw new RuntimeException(t);
    } catch (IllegalAccessException e){
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    }
  }

  static Object invokeAndThrow(Method method, Object target,
      Class<? extends Throwable> expectedException, Object... args) {
    boolean caught = false;
    Object result = null;
    try {
      result = method.invoke(target, args);
    } catch (InvocationTargetException t) {
      if (expectedException == null) {
        Options.debugPrintStackTrace(t);
        throw new RuntimeException(t);
      }
      if (!expectedException.isAssignableFrom(t.getCause().getClass())) {
        Options.debugPrintStackTrace(t);
        throw new RuntimeException("Caught " + t + ", not " + expectedException, t);
      }
      caught = true;
    } catch (IllegalAccessException e){
      Options.debugPrintStackTrace(e);
      throw new RuntimeException(e);
    }
    if (!caught && expectedException != null) {
      throw new RuntimeException("Failed to catch " + expectedException);
    }
    return result;
  }
}
