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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete implementation of ClassInstrumentation. Maintains information
 * about a class that has been {@link Instrumentation instrumented}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class ClassInstrumentationImpl implements ClassInstrumentation {

  /** The instrumented class that this object represents. */
  private final Class<?> instrumentedClass;

  /**
   * The ClassInstrumentationImpl corresponding to the superclass
   * of the {@link #instrumentedClass}.
   */
  private final ClassInstrumentationImpl superclass;

  /**
   * Maps a method in this class onto the corresponding
   * MethodInstrumentation.
   */
  private Map<Method, MethodInstrumentation> methodMap = new HashMap<Method, MethodInstrumentation>();


  /**
   * Maps the name of method in this class onto the corresponding
   * MethodInstrumentation.
   */
  private Map<String, Method> namedMethods = new HashMap<String,Method>();

  /**
   * List of names of overloaded methods. Used to check that a
   * Method has a unique name.
   */
  private Set<String> overloadedMethods = new HashSet<String>();

  /**
   * Creates a new ClassInstrumentationImpl instance.
   *
   * @param clss the instrumented class.
   * @param superclass the ClassInstrumentationImpl corresponding to the
   *     superclass of the instrumented class.
   * @param instrumentedMethods the list of methods in the instrumented class.
   */
  ClassInstrumentationImpl(Class<?> clss, ClassInstrumentationImpl superclass,
      List<MethodInstrumentationImpl> instrumentedMethods) {
    if (clss == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    this.instrumentedClass = clss;
    this.superclass = superclass;
    for (MethodInstrumentationImpl instrumentedMethod : instrumentedMethods) {
      String name = instrumentedMethod.getName();
      if (name.equals("<init>")) {
        continue;
      }

      Method method = instrumentedMethod.getUnderlyingMethod();
      methodMap.put(method, instrumentedMethod);
      if (namedMethods.containsKey(name)) {
        overloadedMethods.add(name);
      } else {
        namedMethods.put(name, method);
      }
    }
  }

  @Override
  public Collection<MethodInstrumentation> getMethods() {
    return Collections.unmodifiableCollection(methodMap.values());
  }

  @Override
  public MethodInstrumentation getMethod(String methodName) {
    return getMethod(getMethodFromName(methodName));
  }

  @Override
  public MethodInstrumentation getMethod(Method method) {
    MethodInstrumentation result = methodMap.get(method);
    if (result == null) {
      if (superclass != null) {
        return superclass.getMethod(method);
      } else {
        throw new IllegalArgumentException("Cannot find method " + method);
      }
    }
    return result;
  }

  private Method getMethodFromName(String name) {
    if (overloadedMethods.contains(name)) {
      throw new IllegalArgumentException("name " + name + " is not unique");
    }
    Method result = namedMethods.get(name);
    if(result == null) {
      throw new IllegalArgumentException("Cannot find " + name);
    }
    return result;
  }

  private Method checkMethod(Method method) {
    if (methodMap.containsKey(method)) {
      return method;
    } else {
      if (superclass != null) {
        return superclass.checkMethod(method);
      } else {
        throw new IllegalArgumentException("Cannot find method " + method);
      }
    }
  }

  @Override
  public CodePosition atMethodStart(Method method) {
    return new MethodStartCodePosition(checkMethod(method));
  }

  @Override
  public CodePosition atMethodEnd(Method method) {
    return new MethodEndCodePosition(checkMethod(method));
  }

  @Override
  public CodePosition atMethodStart(String methodName) {
    return new MethodStartCodePosition(getMethodFromName(methodName));
  }

  @Override
  public CodePosition atMethodEnd(String methodName) {
    return new MethodEndCodePosition(getMethodFromName(methodName));
  }

  @Override
  public CodePosition beforeCall(String methodName, String calledMethodName) {
    return new BeforeCallCodePosition(getMethodFromName(methodName), calledMethodName);
  }

  @Override
  public CodePosition beforeCall(Method method, Method calledMethod) {
    // TODO(alasdair): validate the called method
    return new BeforeCallCodePosition(checkMethod(method), calledMethod);
  }

  @Override
  public CodePosition afterCall(String methodName, String calledMethodName) {
    return new AfterCallCodePosition(getMethodFromName(methodName), calledMethodName);
  }

  @Override
  public CodePosition afterCall(Method method, Method calledMethod) {
    // TODO(alasdair): validate the called method
    return new AfterCallCodePosition(checkMethod(method), calledMethod);
  }

  @Override
  public CodePosition beforeSync(String methodName, Object syncTarget) {
    // TODO(alasdair): add implementation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public CodePosition afterSync(String methodName, Object syncTarget) {
    // TODO(alasdair): add implementation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public CodePosition beforeSync(Method method, Object syncTarget) {
    // TODO(alasdair): add implementation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public CodePosition afterSync(Method method, Object syncTarget) {
    // TODO(alasdair): add implementation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public String toString() {
    return "Instrumented Class for " + instrumentedClass.getName();
  }
}
