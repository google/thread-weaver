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


/**
 * Contains information about a class that has been instrumented. This allows
 * {@link CodePosition} objects to be created for the class.
 * <p>
 * CodePositions are often specified relative to methods. For example, a
 * CodePosition can be created at the beginning of a particular method within
 * the instrumented class. When referring to methods, two approaches are
 * offered: by name, and by a Method object. Names are simpler, but can be
 * ambiguous when overloading is used. Given the following code:
 * <pre>
 * public void printMessage();
 * public void printMessage(PrintStream output);
 * </pre>
 * a simple reference to the method "printMessage" is ambiguous. In order to
 * specify which version of <code>printMessage</code> is being referred to,
 * specify a CodePosition using a Method object. See {@link
 * #atMethodStart(String)} and {@link #atMethodStart(Method)}.
 * <p>
 * If an ambiguous method is referred to by name, then an
 * IllegalArgumentException will be thrown. In addition, if a reference is made
 * to a non-existent or invalid method, an IllegalArgumentException will be
 * thrown.
 *
 * Note that CodePositions can only be created for methods directly defined in
 * the corresponding Class. Given a class <code>Foo</code>, the ClassInstrumentation
 * corresponding to <code>Foo</code> can only be used to create breakpoints in
 * methods defined directly in <code>Foo</code>. (I.e. for methods that can be
 * obtained by invoking <code>Foo.class.getDeclaredMethod()</code>.)
 * CodePositions cannot be created in constructors.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface ClassInstrumentation {

  /**
   * Returns a CodePosition representing the start of a method, before any of
   * the code in the method executes.  If the method is synchronized, this
   * represents a position before the synchronized block is entered.
   */
  CodePosition atMethodStart(String methodName);

  /**
   * Returns a CodePosition representing the start of a method, before any of
   * the code in the method executes. If the method is synchronized, this
   * represents a position before the synchronized block is entered.
   */
  CodePosition atMethodStart(Method method);

  /**
   * Returns a CodePosition representing the end of a method, after all of the
   * code in the method has executed. If the method throws an exception, then
   * a {@link Breakpoint} created at this position will still be hit.
   */
  CodePosition atMethodEnd(String methodName);

  /**
   * Returns a CodePosition representing the end of a method, after all of the
   * code in the method has executed. If the method throws an exception, then
   * a {@link Breakpoint} created at this position will still be hit.
   */
  CodePosition atMethodEnd(Method method);

  /**
   * Returns a CodePosition just before a call to a target method from within
   * the main method. A Breakpoint created at this position will be hit the
   * first time the target method is called. (If there are multiple calls to
   * the same target method, only the first call is considered.)
   * <p>
   * Given the following code in the class:
   *
   * <pre>
   * public void printSomething() {
   *   System.out.printf(&quot;Hello\n&quot;);
   * }
   * </pre>
   *
   * Then we can get the CodePoint representing a position just before the call
   * to "printf" made within the method "printSomething" by:
   *
   * <pre>
   * CodePosition cp = beforeCall(&quot;printSomething&quot;, &quot;printf&quot;,...);
   * </pre>
   *
   * @param caller the name of the method in which the position should be created.
   * @param called the name of the target method.
   *
   * @return the new code position
   */
  CodePosition beforeCall(String caller, String called);

  /**
   * Returns a CodePosition just before a call made within a method. See
   * {@link #beforeCall(Method, Method)}.
   *
   * @param caller the method in which the position should be created.
   * @param called the target method.
   *
   * @return the new code position
   *
   */
  CodePosition beforeCall(Method caller, Method called);


  /**
   * Returns a CodePosition just after a call made within a method. A Breakpoint
   * created at this position will be hit on returning from the first invocation
   * of the the target method. (If there are multiple calls to the same target
   * method, only the first call is considered.)
   * <p>
   * Given the following code in the class:
   *
   * <pre>
   * public void printSomething() {
   *   System.out.printf(&quot;Hello\n&quot;);
   * }
   * </pre>
   *
   * Then we can get the CodePoint representing a position just after the call
   * to "printf" made within the method "printSomething" by:
   *
   * <pre>
   * CodePosition cp = afterCall(&quot;printSomething&quot;, &quot;printf&quot;,...);
   * </pre>
   *
   * @param caller the name of the method in which the position should be created
   * @param called the name of the target method
   *
   * @return the new code position
   */
  CodePosition afterCall(String caller, String called);


  /**
   * Returns a CodePosition just after a call made within a method. See
   * {@link #afterCall(Method, Method)}.
   *
   * @param caller the method in which the position should be created.
   * @param called the target method.
   *
   * @return the new code position
   *
   */
  CodePosition afterCall(Method caller, Method called);

  /**
   * Returns a CodePosition representing the start of a synchronized block,
   * before the lock has beebn taken.
   *
   * @param methodName the name of the method containing the synchronized block.
   * @param syncTarget the object being synchronized.
   *
   * @return the code position
   */
  CodePosition beforeSync(String methodName, Object syncTarget);

  /**
   * Returns a CodePosition representing the start of a synchronized block,
   * before the lock has beebn taken.
   *
   * @param method the method containing the synchronized block.
   * @param syncTarget the object being synchronized.
   *
   * @return the code position
   */
  CodePosition beforeSync(Method method, Object syncTarget);

  /**
   * Returns a CodePosition representing the end of a synchronized block, after
   * the lock has been released
   *
   * @param methodName the name of the method containing the synchronized block.
   * @param syncTarget the object being synchronized.
   *
   * @return the code position
   */
  CodePosition afterSync(String methodName, Object syncTarget);

  /**
   * Returns a CodePosition representing the end of a synchronized block, after
   * the lock has been released
   *
   * @param method the method containing the synchronized block.
   * @param syncTarget the object being synchronized.
   *
   * @return the code position
   */
  CodePosition afterSync(Method method, Object syncTarget);

  /**
   * Returns the instrumented methods in this class.
   *
   * @return the methods
   */
  Collection<MethodInstrumentation> getMethods();

  /**
   * Returns the instrumented method for the given method name.
   */
  MethodInstrumentation getMethod(String methodName);

  /**
   * Returns the instrumented method for the given method.
   */
  MethodInstrumentation getMethod(Method method);

}
