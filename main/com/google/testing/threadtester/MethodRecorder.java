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

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Method;

/**
 * Provides a mechanism for creating {@link CodePosition}s for a given
 * object-under-test, using direct method calls. Sample useage is as follows:
 * <pre>
 *
 *  ClassUnderTest object = new ClassUnderTest();
 *  MethodRecorder<ClassUnderTest> recorder =
 *      new MethodRecorder<ClassUnderTest>(object)
 *
 *  ClassUnderTest control = recorder.getControl();
 *
 *  // Create a CodePosition corresponding to the start
 *  // of the call to 'aMethod' in the test object.
 *  CodePosition cp = recorder.atStart(control.aMethod()).position();
 * </pre>
 *
 * Calling {@link #getControl} returns a dummy object that is of the same class as
 * the object-under-test. Methods invoked on the control object have no effect,
 * but the method calls are noted by the MethodRecorder, and can then be used to
 * create CodePositions. If the methods invoked require arguments, any argument
 * (including null) may be passed in.
 * <p>
 * A CodePosition can also be created relative to an internal method call. Given
 * the following implementation:
 *
 * <pre>
 *  public ClassUnderTest {
 *    public int aMethod() {
 *      int value = getValue();
 *      System.out.printf("Value = %d", value);
 *      return value;
 *    }
 * </pre>
 *
 * A CodePosition in the method <code>aMethod</code>, immediately after the
 * call to <code>printf</code>, could be created as follows:
 *
 * <pre>
 *  ClassUnderTest control = recorder.getControl();
 *  PrintStream stream = recorder.createTarget(PrintStream.class);
 *  CodePosition cp =
 *    recorder.in(control.aMethod()).afterCalling(stream.printf("")).position();
 * </pre>
 *
 * Again, the target object is a dummy instance, and any argument may be passed
 * in to the method calls.
 * <p>
 * The above examples can only be used when the method in question returns a
 * value. If it does not, then the "LastMethod" variations must be used. Given
 * the following class:
 *
 * <pre>
 *  public ClassUnderTest {
 *    public void voidMethod() {
 *      System.out.println("in void method");
 *    }
 * </pre>
 *
 * A CodePosition in the method <code>voidMethod</code>, immediately after the
 * call to <code>println</code>, could be created as follows:
 *
 * <pre>
 *  ClassUnderTest control = recorder.getControl();
 *  PrintStream stream = recorder.createTarget(PrintStream.class);
 *  control.voidMethod();
 *  recorder.inLastMethod();
 *  stream.println("");
 *  recorder.afterCallingLastMethod();
 *  CodePosition cp = recorder.position();
 * </pre>
 *
 * @param <T> the type for which we want to create positions.
 */
public class MethodRecorder<T> {

  /** The instrumented object corresponding to the main object. */
  private final ObjectInstrumentation<T> instrumentedObject;

  /** The instrumented class corresponding to the instrumented object. */
  private final ClassInstrumentation instrumentedClass;

  /** The control object. This is a dummy instance of the main object. */
  private T controlObject;

  /** The last control method invoked. */
  private volatile Method lastControlMethod;

  /** The last target method invoked. */
  private volatile Method lastTargetMethod;

  /** The Objenesis factory for creating new control objects */
  private Objenesis objenesis = new ObjenesisStd();

  /**
   * The internal state. Represents the last recorded position in the control
   * object.
   */
  private enum Position {

    /** At the start of a method call. */
    START,

    /** At the end of a method call. */
    END,

    /**
     * Within a method call. This is a transient state, and should normally
     * be followed by {@link #BEFORE_TARGET} or {@link #AFTER_TARGET}.
     */
    WITHIN,

    /** Just before a call to a method on another object. */
    BEFORE_TARGET,

    /** Just after a call to a method on another object. */
    AFTER_TARGET,

    /** The intitial state, before any calls have been recorded. */
    UNDEFINED
  }

  private volatile Position position = Position.UNDEFINED;

  /**
   * Intercepts any method invoked on the control or target objects, and records
   * the last method invoked.
   */
  private abstract class Interceptor implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
      // The invoked method doesn't actually do anything, it just records the
      // Method object.
      intercepted(method);
      return null;
    }

    /**
     * Intercepts a called method.
     */
    abstract void intercepted(Method method);
  }

  /**
   * Creates a new instance of the given class, using the supplied interceptor.
   * Uses the EasyMock ClassInstantiatorFactory in order to avoid the cglib
   * limitation that prevents us from creating instances of classes that do not
   * have public default constructors.
   */
  private Object create(Class<?> clss, Interceptor interceptor) {
    Enhancer e = new Enhancer();
    e.setSuperclass(clss);
    e.setCallbackType(interceptor.getClass());
    Class<?> controlClass = e.createClass();
    Enhancer.registerCallbacks(controlClass, new Callback[] { interceptor });

    Factory result = (Factory) objenesis.newInstance(controlClass);

    // This call is required to work around a cglib feature. See the comment in
    // org.easymock.classextension.internal.ClassProxyFactory, which uses the
    // same approach.
    result.getCallback(0);

    // And this call is required to work around a memory leak in cglib, which
    // sticks references to the class in a ThreadLocal that is never cleared.
    // See http://opensource.atlassian.com/projects/hibernate/browse/HHH-2481
    Enhancer.registerCallbacks(controlClass, null);

    return result;
  }

  /**
   * Creates a new MethodRecorder for the given object. Note that the object's
   * class must be Instrumented.
   *
   * @see Instrumentation
   */
  @SuppressWarnings("unchecked")
  public MethodRecorder(T object) {
    if (object == null) {
      throw new IllegalArgumentException("Main object cannot be null");
    }
    // Get the instrumented object for the main object. This will verify
    // that the object's class is instrumented.
    instrumentedObject = Instrumentation.getObjectInstrumentation(object);
    instrumentedClass = Instrumentation.getClassInstrumentationForObject(object);
    initialize((Class<T>) object.getClass());
  }

  /**
   * Creates a new MethodRecorder for the given class. Note that the
   * class must be Instrumented.
   *
   * @see Instrumentation
   */
  public MethodRecorder(Class<T> clss) {
    if (clss == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    instrumentedObject = null;
    instrumentedClass = Instrumentation.getClassInstrumentation(clss);
    initialize(clss);
  }

  @SuppressWarnings("unchecked")
  private void initialize(Class<T> clss) {
    Interceptor interceptor = new Interceptor() {
        @Override
        void intercepted(Method method) {
          lastControlMethod = method;
        }
      };
    controlObject = (T) create(clss, interceptor);
  }

  /**
   * Gets the control object. This is a dummy instance of the object passed into
   * the constructor. Method calls made on the control object can be recorded to
   * create {@link CodePosition}s.
   */
  @SuppressWarnings("unchecked")
  public T getControl() {
    return controlObject;
  }

  /**
   * Creates a target object of the given class. This is a dummy instance, and
   * can be used to record method calls.
   */
  @SuppressWarnings("unchecked")
  public <T> T createTarget(Class<T> clss) {
    if (clss == null) {
      throw new IllegalArgumentException("Class cannot be null");
    }
    Interceptor interceptor = new Interceptor() {
        @Override
        void intercepted(Method method) {
          lastTargetMethod = method;
        }
      };
    return (T) create(clss, interceptor);
  }

  /**
   * Gets the instrumented object corresponding to the main object passed in to
   * the constructor. Returns null if this recorder was constructed using a
   * class, not an object. See {@link MethodRecorder#MethodRecorder(Class)}
   */
  ObjectInstrumentation<T> getInstrumentedObject() {
    return instrumentedObject;
  }

  /**
   * Gets the instrumented class corresponding to the instrumented object.
   *
   * @see #getInstrumentedObject
   */
  ClassInstrumentation getInstrumentedClass() {
    return instrumentedClass;
  }

  /**
   * Sets the state of the recorder to represent a position within the last
   * method invoked on the control object. After calling this method, you must
   * call {@link #beforeCalling} or {@link #afterCalling} before calling {@link
   * #position}.
   *
   * @see #getControl
   */
  public MethodRecorder<T> in(Object result) {
    if (lastControlMethod == null) {
      throw new IllegalStateException("Must call a control method first");
    }
    lastTargetMethod = null;
    position = Position.WITHIN;
    return this;
  }

  /**
   * Sets the state of the recorder to represent a position within the last
   * method invoked on the control object. Used for void methods where chaining
   * is impossible.
   *
   * @see #in
   */
  public MethodRecorder<T> inLastMethod() {
    return in(null);
  }

  /**
   * Sets the state of the recorder to represent a position at the beginning of
   * the last method invoked on the control object.
   */
  public MethodRecorder<T> atStartOf(Object result) {
    if (lastControlMethod == null) {
      throw new IllegalStateException("Must call a control method first");
    }
    position = Position.START;
    return this;
  }

  /**
   * Sets the state of the recorder to represent a position at the beginning of
   * the last method invoked on the control object. Used for void methods where
   * chaining is impossible.
   */
  public MethodRecorder<T> atStartOfLastMethod() {
    return atStartOf(null);
  }

  /**
   * Sets the state of the recorder to represent a position at the end of
   * the last method invoked on the control object.
   */
  public MethodRecorder<T> atEndOf(Object result) {
    if (lastControlMethod == null) {
      throw new IllegalStateException("Must call a control method first");
    }
    position = Position.END;
    return this;
  }

  /**
   * Sets the state of the recorder to represent a position at the end of the
   * last method invoked on the control object. Used for void methods where
   * chaining is impossible.
   */
  public MethodRecorder<T> atEndOfLastMethod() {
    return atEndOf(null);
  }

  /**
   * Sets the state of the recorder to represent a position before a call to the
   * last method invoked on the target object. You must call {@link #in} before
   * calling this method.
   */
  public MethodRecorder<T> beforeCalling(Object result) {
    if (position != Position.WITHIN) {
      throw new IllegalStateException("Must call a control method first");
    }
    if (lastTargetMethod == null) {
      throw new IllegalStateException("Must call a target method first");
    }
    position = Position.BEFORE_TARGET;
    return this;
  }

  /**
   * Sets the state of the recorder to represent a position before a call to the
   * last method invoked on the target object. Used for void methods where
   * chaining is impossible.
   *
   * @see #beforeCalling
   */
  public MethodRecorder<T> beforeCallingLastMethod() {
    return beforeCalling(null);
  }

  /**
   * Sets the state of the recorder to represent a position after a call to the
   * last method invoked on the target object. You must call {@link #in} before
   * calling this method.
   */
  public MethodRecorder<T> afterCalling(Object result) {
    if (position != Position.WITHIN) {
      throw new IllegalStateException("Must call a control method first");
    }
    if (lastTargetMethod == null) {
      throw new IllegalStateException("Must call a target method first");
    }
    position = Position.AFTER_TARGET;
    return this;
  }

  /**
   * Sets the state of the recorder to represent a position after a call to the
   * last method invoked on the target object. Used for void methods where
   * chaining is impossible.
   *
   * @see #afterCalling
   */
  public MethodRecorder<T> afterCallingLastMethod() {
    return afterCalling(null);
  }

  /**
   * Creates a new CodePosition corresponding to the last methods called on the
   * control object, and optionally on a target object. After returning a code
   * position, the state of the recorder is reset, and other target methods must
   * be invoked before calling this method again.
   *
   * @see #getControl
   * @see #createTarget
   *
   * @throws IllegalStateException if control and target methods have not been
   * called.
   */

  public CodePosition position() {
    if (position == Position.UNDEFINED) {
      throw new IllegalStateException("No method has been called");
    }
    return getPosition();
  }

  /**
   * Creates a CodePosition if one has been defined. Returns null if no
   * target/control methods have been invoked. Will still throw
   * IllegalStateException if methods have been invoked incorrectly.
   *
   * @see #position
   */
  CodePosition getPositionIfAny() {
    if (position == Position.UNDEFINED) {
      return null;
    }
    return getPosition();
  }

  private CodePosition getPosition() {
    switch (position) {
      case START:
      case END:
        // We fall through to here - the check is the same for START and END
        if (lastTargetMethod != null) {
          throw new IllegalStateException("Cannot combine start/end with target object method");
        }
        break;

      case WITHIN:
        // WITHIN is a transient state, and you need to call beforeCalling() or
        // afterCalling() first.
        throw new IllegalStateException("Must specify a target object method");
    }
    try {
      switch (position) {
        case START:
          return instrumentedClass.atMethodStart(lastControlMethod);
        case END:
          return instrumentedClass.atMethodEnd(lastControlMethod);
        case BEFORE_TARGET:
          return instrumentedClass.beforeCall(lastControlMethod, lastTargetMethod);
        case AFTER_TARGET:
          return instrumentedClass.afterCall(lastControlMethod, lastTargetMethod);
        default:
          throw new IllegalStateException("Unknown state " + position);
      }
    } finally {
      position = Position.UNDEFINED;
      lastControlMethod = null;
      lastTargetMethod = null;
    }
  }

  /**
   * Creates a Breakpoint for the current CodePosition in the given thread. Note
   * that this method can only be called if this recorder was created with an
   * object, not a class. See {@link MethodRecorder#MethodRecorder(Class)}
   *
   * @see #position
   */
  public Breakpoint breakpoint(Thread thread) {
    if (instrumentedObject == null) {
      throw new IllegalStateException(
          "Cannot get breakpoint unless recorder was created with an object");
    }
    return instrumentedObject.createBreakpoint(position(), thread);
  }
}
