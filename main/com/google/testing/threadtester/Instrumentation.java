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
 * Factory for obtaining instrumented classes and objects. In order for
 * instrumentation to be enabled, the classes must have been loaded
 * via a special classloader, so that the necessary data can be added
 * to the class' bytecode. The easiest way to do this is to run all
 * tests via a {@link BaseThreadedTestRunner threaded test runner}.
 *
 * @param <T> the class being instrumented
 *
 * @see ClassInstrumentation
 * @see ObjectInstrumentation
 * @see BaseThreadedTestRunner
 * @see TestInstrumenter
 * @see com.google.testing.instrumentation.InstrumentedClassLoader
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class Instrumentation <T> {

  private Instrumentation() {
    // Factory class - no constructor
  }

  /**
   * Returns the {@link ObjectInstrumentation} corresponding to a given
   * object. Note that this method always returns the same result for a given
   * object - i.e. there is a one-to-one mapping from ObjectInstrumentation to
   * underlying object.
   *
   * @throws IllegalArgumentException if the object's class has not been
   * instrumented.
   */
  @SuppressWarnings("unchecked")
  public static <T> ObjectInstrumentation<T> getObjectInstrumentation(T underlyingObject) {
    return ObjectInstrumentationImpl.getObject(underlyingObject);
  }

  /**
   * Returns the {@link ClassInstrumentation} corresponding to a given
   * class. Note that this method always returns the same result for a given
   * class - i.e. there is a one-to-one mapping from a ClassInstrumentation to a
   * class.
   *
   * @throws IllegalArgumentException if the class has not been instrumented.
   */
  public static <T> ClassInstrumentation getClassInstrumentation(Class<T> clss) {
    return CallLoggerFactory.getFactory().getClassInstrumentation(clss);
  }

  /**
   * Returns the {@link ClassInstrumentation} corresponding to a given
   * classname. Note that this method always returns the same result for a given
   * class - i.e. there is a one-to-one mapping from ClassInstrumentation to
   * class.
   *
   * @throws IllegalArgumentException if the class has not been instrumented.
   */
  public static <T> ClassInstrumentation getNamedClassInstrumentation(String classname) {
    try {
      Class<?> clss = Class.forName(classname);
      return getClassInstrumentation(clss);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Cannot find class " + classname);
    }
  }

  /**
   * Returns the {@link ClassInstrumentation} corresponding to the class of the
   * given object.  Note that this method always returns the same result for a
   * given class - i.e. there is a one-to-one mapping from ClassInstrumentation
   * to class.
   *
   * @throws IllegalArgumentException if the class has not been instrumented.
   */
  public static <T> ClassInstrumentation getClassInstrumentationForObject(T underlyingObject) {
    return getClassInstrumentation(underlyingObject.getClass());
  }
}
