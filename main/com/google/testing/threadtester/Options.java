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

import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

/**
 * Defines internal options for the test runners. This class is static, which is
 * normally considered a bad thing. However, as each test runner will create its
 * own custom class loader to load the test classes (and the test framework),
 * each instance of the Options class is private to the test that owns it. Hence
 * one set of tests will not upset the options of another set of tests that uses
 * a different test runner.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class Options {

  /**
   * The default value for the timeout.
   * @see #timeout
   */
  static final long DEFAULT_TIMEOUT = 1000L;

  /**
   * The default value for the debug flag
   * @see #debug
   */
  static final boolean DEFAULT_DEBUG = false;

  static final MethodOption DEFAULT_METHOD_OPTION = MethodOption.MAIN_METHOD;

  private static long timeout = DEFAULT_TIMEOUT;
  private static boolean debug = DEFAULT_DEBUG;
  private static MethodOption method_option = DEFAULT_METHOD_OPTION;
  private static Set<String> methodNames;

  private Options() {
    // Only static methods
  }

  /**
   * Sets the option that determine which methods are run. Note that optionVal is an
   * integer that must correspond to a valid {@link MethodOption}. Iff the option is
   * {@link MethodOption#LISTED_METHODS}, newMethodNames must be a non-null non-empty set.
   * The set of method names is in the format "classname.methodname", e.g.
   * "com.google.project.MyClass.myMethod". <p> If not explicitly set, the default value
   * is {@link MethodOption#MAIN_METHOD}.
   */
  static void setMethodOption(int optionVal, Set<String> newMethodNames) {
    MethodOption option = MethodOption.fromInt(optionVal);
    boolean namesSpecified = (newMethodNames != null && newMethodNames.size() > 0);
    if (option == MethodOption.LISTED_METHODS) {
      if (!namesSpecified) {
        throw new IllegalArgumentException(
            "Must specify at least one method when using LISTED_METHODS");
      }
      methodNames = new HashSet<String>(newMethodNames);
    } else {
      if (namesSpecified) {
        throw new IllegalArgumentException(
            "Cannot specify method names except when using LISTED_METHODS");
      }
    }
    method_option = option;
  }

  /**
   * Gets the {@link MethodOption} used by the {@link AnnotatedTestRunner}
   * that is running the current test. 
   */
  static MethodOption getMethodOption() {
    return method_option;
  }

  /**
   * If #getMethodOption returns {@link MethodOption.LISTED_METHODS}, returns
   * the set of methods to test, in the form "classname.methodname". Otherwise
   * returns null.
   */
  static Set<String> methodsToTest() {
    if (method_option == MethodOption.LISTED_METHODS) {
      return Collections.unmodifiableSet(methodNames);
    } else {
      return null;
    }
  }

  /**
   * Gets the default wait time in millseconds. Used for all thread waits and
   * joins.
   */
  static long timeout() {
    return timeout;
  }

  /**
   * Sets the timeout. Note that this method is called by reflection in {@link
   * BaseThreadedTestRunner}. Do not change the name without updating the
   * reference.
   *
   * @see #timeout
   */
  static void setTimeout(long newTimeout) {
    timeout = newTimeout;
  }

  /**
   * Sets the debug mode flag. Note that this method is called by reflection in {@link
   * BaseThreadedTestRunner}. Do not change the name without updating the
   * reference.
   *
   * @see #debug
   */
  static void setDebug(boolean newDebug) {
    debug = newDebug;
  }

  /**
   * Return true if debug mode is on.
   */
  static boolean debug() {
    return debug;
  }

  /**
   * Prints the formatted message if debug mode is true. If debug mode is not
   * true, does nothing.
   *
   * @see #debug
   * @see java.lang.String#format
   */
  static void debugPrint(String format, Object... args) {
    if (debug) {
      System.out.printf(format, args);
    }
  }

  /**
   * Prints the message (plus a newline) if debug mode is true. If debug mode is
   * not true, does nothing.
   *
   * @see #debug
   */
  static void debugPrint(String message) {
    if (debug) {
      System.out.println(message);
    }
  }

  /**
   * Prints the stack trace from a given Throwable if debug mode is true. If
   * debug mode is not true, does nothing.
   *
   * @see #debug
   */
  static void debugPrintStackTrace(Throwable t) {
    if (debug) {
      t.printStackTrace();
    }
  }
}
