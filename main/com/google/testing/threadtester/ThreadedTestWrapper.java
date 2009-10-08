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
 * Implementation of BaseTestWrapper designed for use with {@link
 * ThreadedTestRunner}. Runs all test methods annotated with the
 * {@link ThreadedTest} annotation defined in the main test class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ThreadedTestWrapper implements BaseTestWrapper {

  @Override
  public void runTests(Class<?> testClass, List<String> instrumentedClasses) {
    Object testObject = MethodCaller.newInstance(testClass);
    Method beforeMethod = null;
    Method afterMethod = null;
    String name = testClass.getSimpleName();

    Method[] methods = testClass.getMethods();
    for (Method method : methods) {
      if (method.getAnnotation(ThreadedBefore.class) != null) {
        if (beforeMethod != null) {
          throw new IllegalArgumentException("Method " + method + " and " + beforeMethod +
              " have the same annotation");
        }
        beforeMethod = method;
      }
      if (method.getAnnotation(ThreadedAfter.class) != null) {
        if (afterMethod != null) {
          throw new IllegalArgumentException("Method " + method + " and " + beforeMethod +
              " have the same annotation");
        }
        afterMethod = method;
      }
    }
    boolean foundMatch = false;
    for (Method method : methods) {
      ThreadedTest annotation = method.getAnnotation(ThreadedTest.class);
      if (annotation != null) {
        foundMatch = true;

        // Find the exception (if any) that we expect the test method to
        // throw. Note that we can't have a null default for an annotation, so
        // we have to use the NoException class to indicate that the expected
        // exception is null.
        Class<? extends Throwable> expectedThrown = annotation.expected();
        if (expectedThrown == ThreadedTest.NoException.class) {
          expectedThrown = null;
        }
        if (beforeMethod != null) {
          Options.debugPrint("\nInvoking \"before\" method %s.%s\n", name, beforeMethod.getName());
          MethodCaller.invoke(beforeMethod, testObject);
        }

        Options.debugPrint("\nInvoking test method %s.%s\n", name, method.getName());
        MethodCaller.invokeAndThrow(method, testObject, expectedThrown);

        if (afterMethod != null) {
          Options.debugPrint("\nInvoking \"after\" method %s.%s\n", name, afterMethod.getName());
          MethodCaller.invoke(afterMethod, testObject);
        }
      }
    }
    if (!foundMatch) {
      throw new IllegalArgumentException("No @ThreadedTest annotations in " + testClass);
    }
  }

}
