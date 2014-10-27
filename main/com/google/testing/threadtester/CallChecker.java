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
import javassist.CtMethod;
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
  Map<Method, Set<Method>> getCallers(Class<?> caller, Class<?>... targets) {
    ArrayList<Class<?>> target_list = new ArrayList<Class<?>>();
    Collections.addAll(target_list, targets);
    return getCallers(caller, target_list);
  }

  /**
   * Gets a list of the method calls made by a given class. See {@link #getAllCallers(Class, List)}
   */
  Map<Method, Set<Method>> getAllCallers(Class<?> caller, Class<?>... targets) {
    ArrayList<Class<?>> target_list = new ArrayList<Class<?>>();
    Collections.addAll(target_list, targets);
    return getAllCallers(caller, target_list);
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
   * to <code>{method1}</code>. (Note that a Map is returned, for compatibility
   * with {@link #getAllCallers}, but it will only contain xsone element.
   * <p>
   * A method in the calling class that does not invoke any methods in any of
   * the target classes will not be present in the map.
   */
  Map<Method, Set<Method>> getCallers(final Class<?> caller, List<Class<?>> targets) {
    return getCallersInternal(caller, targets, false);
  }  

  /**
   * Gets the method calls made by a given class (or its superclasses) to all methods
   * defined in the target classes, either directly or via recursive calls. Returns a map,
   * keyed on the methods in the calling class. Each calling method maps onto a set of
   * target methods invoked. For example, given:
   * <pre>
   *   public void doSomething() {
   *     target.method1();
   *     target.method2();
   *   }
   *
   *   class Target {
   *     public void method1() {
   *       this.privateImpl();
   *     }
   *     public int method2() {
   *       return 42;
   *     }
   *
   * </pre>
   *
   * Then the returned map will contain a mapping from <code>doSomething</code>
   * to <code>{method1, privateImpl, method2}</code>.
   * <p>
   * Note that the recursion stops when a class outside of <code>targets</code>
   * is encountered. For example, given:
   * <pre>
   *   class Target {
   *     public void method1() {
   *       target2.impl();
   *     }
   *     public void method3() {
   *     }
   *
   *   class Target2 {
   *     public void impl() {
   *       target1.method3();
   *     }
   * </pre>
   * A call to <code>Target.method1</code> will NOT be traced through 
   * <code>Target2.impl</code> to <code>Target.method3</code>.
   * <p>
   * A method in the calling class that does not invoke any methods in any of
   * the target classes will not be present in the map.
   */
  Map<Method, Set<Method>> getAllCallers(final Class<?> caller, List<Class<?>> targets) {
    return getCallersInternal(caller, targets, true);
  }

  private Map<Method, Set<Method>> getCallersInternal(final Class<?> caller,
                                                      List<Class<?>> targets,
                                                      boolean getRecursive) {
    Map<Method, Set<Method>> result = new HashMap<Method, Set<Method>>();

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

    getCallersImpl(caller, targets, methodsToProcess, getRecursive, result);
    return result;
  }

  /**
   * Gets the method calls made by the given caller to the given target classes.
   */
  private void getCallersImpl(final Class<?> caller, final List<Class<?>> targets,
                              final Set<Method> methodsToProcess, final boolean getRecursive,
                              final Map<Method, Set<Method>> result ) {
    final Set<String> targetNames = new HashSet<String>();
    for (Class<?> target : targets) {
      targetNames.add(target.getName());
    }
    final Set<CtClass> ctClasses = new HashSet<CtClass>();
    try {
      CtClass cl = ClassPool.getDefault().get(caller.getName());
      ctClasses.add(cl);
      cl.instrument(new ExprEditor() {
          @Override
          public void edit(MethodCall called) {
            try {
              if (targetNames.contains(called.getClassName())) {
                Method callingMethod = getMethod(caller.getName(), called.where());
                if (callingMethod != null &&
                    (getRecursive || !result.containsKey(callingMethod)) &&
                    methodsToProcess.contains(callingMethod)) {
                  Method calledMethod = getMethod(called.getClassName(),  called.getMethod());
                  // If we didn't find the called method in the target
                  // class, see if it's defined in one of the other
                  // target classes. (These may be superclasses of the
                  // target.)
                  if (calledMethod == null) {
                    for (String targetName : targetNames) {
                      if (!targetName.equals(called.getClassName())) {
                        calledMethod = getMethod(targetName,  called.getMethod());
                        if (calledMethod != null) {
                          break;
                        }
                      }
                    }
                  }
                  Set<Method> calledMethodSet = result.get(callingMethod);
                  if (calledMethodSet == null) {
                    calledMethodSet = new HashSet<Method>();
                    result.put(callingMethod, calledMethodSet);
                  }
                  calledMethodSet.add(calledMethod);
                  if (getRecursive) {
                    getCallTree(called.getClassName(), called.getMethod().getLongName(), 
                                targetNames, calledMethodSet, ctClasses);
                  }
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
      for (CtClass cl : ctClasses) {
        cl.detach();
      }
    }
    Class<?> superclass = caller.getSuperclass();
    if (superclass != Object.class) {
      getCallersImpl(superclass, targets, methodsToProcess, getRecursive, result);
    }
  }

  /**
   * Given a Class/Method, recursively adds all calls made within that method to other
   * methods Only considers methods invoked on classes defined in targetNames. Adds
   * results to allMethods. Adds any CtClasses used to ctClasses, for final cleanup.
   */
  private void getCallTree(String calledClassName, final String calledMethodName,
                           final Set<String> targetNames, final Set<Method> allMethods,
                           final Set<CtClass> ctClasses) {
    try {
      CtClass cl = ClassPool.getDefault().get(calledClassName);
      ctClasses.add(cl);
      for (CtMethod method : cl.getDeclaredMethods()) {
        if (method.getLongName().equals(calledMethodName)) {
          method.instrument(new ExprEditor() {
              @Override
                public void edit(MethodCall called) {
                try {
                  String calledMethodName = called.getMethod().getLongName();
                  if (targetNames.contains(called.getClassName())) {
                    allMethods.add(getMethod(called.getClassName(),  called.getMethod()));
                    getCallTree(called.getClassName(), calledMethodName,
                                targetNames, allMethods, ctClasses);
                  }
                } catch (NotFoundException e) {
                  throw new RuntimeException(e);
                }
              }
            });
        }
      }
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    } catch (CannotCompileException e) {
      throw new RuntimeException(e);
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
