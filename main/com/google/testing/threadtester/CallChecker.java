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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Analyses the method calls made by a given class. Used by {@link
 * AnnotatedTestWrapper} to determine which instrumented methods are invoked by
 * a test case.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
class CallChecker {

  /**
   * Gets a list of the method calls made by a given class. See {@link #getCallers(Class, List)}
   */
  Map<Method, Method> getCallers(Class<?> caller, Class<?>... targets) {
    ArrayList<Class<?>> target_list = new ArrayList<Class<?>>();
    Collections.addAll(target_list, targets);
    return getCallers(caller, target_list);
  }

  /**
   * Gets the method calls made by a given class (or its superclasses) to
   * methods defined in the target classes. Returns a map, keyed on the methods
   * in the calling class.  Each calling method maps onto the first target
   * method invoked. For example, given:
   * <pre>
   *   public void doSomething() {
   *     target.method1();
   *     target.method2();
   *   }
   * </pre>
   *
   * Then the returned map will contain a mapping from <code>doSomething</code>
   * to <code>method1</code>.
   * <p>
   * A method in the calling class that does not invoke any methods in any of
   * the target classes will not be present in the map.
   */
  Map<Method, Method> getCallers(final Class<?> caller, List<Class<?>> targets) {
    Set<String> targetNames = new HashSet<String>();
    for (Class<?> target : targets) {
      targetNames.add(target.getName());
    }
    Map<Method, Method> result = new HashMap<Method, Method>();

    // If a subclass overrides a superclass' methods, then we only want to
    // examine calls made by the subclass. To implement this, we create a set of
    // all of the methods defined in the subclass. This will include inherited
    // methods, but it will not include overridden ones. Then, in getCallers(),
    // as we move up the class hierarchy, we only look at those methods which are
    // members of this set.
    Set<Method> methodsToProcess = new HashSet<Method>();
    for (Method m : caller.getMethods()) {
      methodsToProcess.add(m);
    }

    getCallers(caller, targetNames, methodsToProcess, result);
    return result;
  }

  /**
   * Gets the method calls made by the given caller to the given target classes.
   */
  private void getCallers(final Class<?> caller, final Set<String> targetNames,
      final Set<Method> methodsToProcess, final Map<Method, Method> result ) {
    CtClass cl = null;
    try {
      cl = ClassPool.getDefault().get(caller.getName());
      cl.instrument(new ExprEditor() {
          @Override
          public void edit(MethodCall called) {
            try {
              if (targetNames.contains(called.getClassName())) {
                Method callingMethod = getMethod(caller.getName(), called.where());
                if (callingMethod != null && !result.containsKey(callingMethod) &&
                    methodsToProcess.contains(callingMethod)) {
                  Method calledMethod = getMethod(called.getClassName(),  called.getMethod());
                  result.put(callingMethod, calledMethod);
                }
              }
            } catch (NotFoundException e) {
              throw new RuntimeException(e);
            }
          }
        });
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    } catch (CannotCompileException e) {
      throw new RuntimeException(e);
    } finally {
      if (cl != null) {
        cl.detach();
      }
    }
    Class<?> superclass = caller.getSuperclass();
    if (superclass != Object.class) {
      getCallers(superclass, targetNames, methodsToProcess, result);
    }
  }

  /**
   * Returns the method object defined by the given behaviour in the given
   * class, or null if there is no such method. A return value of null
   * indicates that although the bytecode contains a reference to a method,
   * that method is actually defined by the superclass of the target. E.g.
   * given the following class:
   * <pre>
   * public class MyList extends ArrayList {
   *   // no methods
   * }
   * <pre>
   * then a class that uses <code>MyList></code> may have a reference to
   * <code>MyList.size()</code>, even though the actual implementation
   * of the <code>size()</code> method is defined in <code>ArrayList</code>.
   */
  private Method getMethod(String className, CtBehavior behavior) {
    Class<?> definingClass = MethodCaller.getClass(className);
    try {
      CtClass[] ctParams =  behavior.getParameterTypes();
      Class<?>[] params = new Class<?>[ctParams.length];
      for (int i = 0; i < ctParams.length; i++) {
        params[i] = TestInstrumenter.getClass(ctParams[i]);
      }
      return definingClass.getDeclaredMethod(behavior.getName(), params);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
