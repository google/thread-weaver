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
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates a {@link CallLogger} for every instrumented class. A reference to
 * this class is added to the bytecode of any class instrumented by the
 * {@link TestInstrumenter}.
 * <p>
 * This class is made public in order to allow instrumented classes to call it,
 * but it should not be called otherwise.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class CallLoggerFactory {

  /**
   * The current instance of CallLoggerFactory. This is a static, initialized
   * the first time we call {@link #getFactory}. It is made static because we
   * need a way to obtain a call logger inside the constructor of each
   * instrumented class.
   */
  private static CallLoggerFactory factory;

  /**
   * Maps an Object onto the corresponding instrumentation data. Note that we
   * use an IdentityHashMap. We always want to track each object independently,
   * regardless of the object's implementation of hashCode and equals. Plus, we
   * don't want to have to invoke the object's hashCode() and equals() methods
   * during its own constructor, as it may not be designed to allow that. (See
   * {@link #getObjectInstrumentation}, which is called from within an
   * instrumented object's constructor.
   */
  private IdentityHashMap<Object, ObjectInstrumentationImpl<?>> instrumentedMap =
    new IdentityHashMap<Object, ObjectInstrumentationImpl<?>>();

  /** Maps a class onto the corresponding ClassInstrumentationImpl */
  private Map<Class<?>, ClassInstrumentationImpl> classMap = new HashMap<Class<?>,ClassInstrumentationImpl>();

  /**
   * Registered listeners. We expect relatively few modifications compared to
   * traversals, so a CopyOnWriteArrayList is fine.
   */
  private Collection<ObjectCreationListener> listeners =
    new CopyOnWriteArrayList<ObjectCreationListener>();

  private CallLoggerFactory() {
    // Can only obtain a CallLoggerFactory through getFactory()
  }

  /**
   * Gets a CallLoggerFactory instance. This method is invoked by the
   * instrumented classes.
   */
  public static synchronized CallLoggerFactory getFactory() {
    if (factory == null) {
      factory = new CallLoggerFactory();
    }
    return factory;
  }

  /**
   * Adds a new {@link ObjectCreationListener} to this factory. When a new
   * instrumented object is created, the listener will be informed.
   */
  synchronized void addObjectCreationListener(ObjectCreationListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes a registered {@link ObjectCreationListener} from this factory.
   */
  synchronized void removeObjectCreationListener(ObjectCreationListener listener) {
    listeners.remove(listener);
  }

  /**
   * Gets an {@link ClassInstrumentation} for a given Class.
   *
   * @throw IllegalArgumentException if the class has not been instrumented.
   */
  ClassInstrumentation getClassInstrumentation(Class<?> clss) {
    ClassInstrumentation result = getClassInstrumentationTolerant(clss);
    if (result == null) {
      throw new IllegalArgumentException("Class " + clss.getSimpleName() + " is not instrumented");
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private synchronized ClassInstrumentationImpl getClassInstrumentationTolerant(Class<?> clss) {
    ClassInstrumentationImpl result = classMap.get(clss);
    if (result == null) {
      try {
        ClassInstrumentationImpl instrumentedSuperclass = null;
        Class superclass = clss.getSuperclass();
        if (superclass != null) {
          instrumentedSuperclass = getClassInstrumentationTolerant(superclass);
        }
        // Get the static method that returns instrumented data. This method is
        // added by the instrumenter to every instrumented class. (And make it
        // accessible, because the test class may not be public, even if the
        // method is.)
        // Note that instrumentation is added  when the class is loaded by the
        // test class loader.
        Method method = clss.getMethod(TestInstrumenter.GET_INSTRUMENTATION);
        method.setAccessible(true);

        // Need to suppress the unchecked warning here, because invoke returns an Object
        List<MethodInstrumentationImpl> methods =
          (List<MethodInstrumentationImpl>) method.invoke(null);

        // Note that instrumentedSuperclass may be null here. This is OK, as it
        // simply means that the superclass has not been instrumented.
        // ClassInstrumentationImpl will accept a null parameter here.
        result = new ClassInstrumentationImpl(clss, instrumentedSuperclass, methods);
        classMap.put(clss, result);
      } catch (NoSuchMethodException nsme) {
        // Class is not instrumented. Just return null.
        return null;
      } catch (IllegalAccessException iae) {
        throw new IllegalArgumentException("Cannot call instrumented method in " + clss, iae);
      } catch (InvocationTargetException ite) {
        throw new IllegalArgumentException("Cannot call instrumented method in " + clss, ite);
      }
    }
    return result;
  }

  /**
   * Returns a set of line numbers for the methods in a given instrumented
   * object.
   *
   * @param instrumented an instance of an instrumented class
   * @return a Map of line number data. The keys are method names. The values
   * are lists of valid line numbers within each method, sorted in increasing
   * order.
   * @throws IllegalArgumentException if the object is not instrumented (and hence
   * has no line number data.)
   */
  @SuppressWarnings("unchecked")
  Map<String, List<Integer>> getLineNumbers(Object instrumented) {
    try {
      Method method = instrumented.getClass().getMethod(TestInstrumenter.GET_INSTRUMENTATION);
      // Need to suppress the unchecked warning here, because invoke returns an Object
      Map<String, List<Integer>> result = (Map<String, List<Integer>>) method.invoke(null);
      if (result == null) {
        throw new IllegalArgumentException("No line number data for " + instrumented);
      }
      return result;
    } catch (NoSuchMethodException nsme) {
      throw new IllegalArgumentException("No instrumented method in " + instrumented);
    } catch (IllegalAccessException iae) {
      throw new IllegalArgumentException("Cannot call instrumented method in " + instrumented);
    } catch (InvocationTargetException ite) {
      throw new IllegalArgumentException("Cannot call instrumented method in " + instrumented);
    }
  }

  /**
   * Gets an {@link ObjectInstrumentationImpl} for a given object.
   *
   * @throws IllegalArgumentException if the object has not been instrumented.
   */
  @SuppressWarnings("unchecked")
  public ObjectInstrumentationImpl getObjectInstrumentation(Object obj) {
    ObjectInstrumentationImpl result;
    synchronized (instrumentedMap) {
      result = instrumentedMap.get(obj);
    }
    if (result == null) {
      throw new IllegalArgumentException("No instrumented object for " + obj);
    }
    return result;
  }

  /**
   * Creates a new CallLogger for the given object, or returns an existing
   * CallLogger if one has already been created
   */
  @SuppressWarnings("unchecked")
  private CallLogger getLogger(Object obj) {
    boolean created = false;
    ObjectInstrumentationImpl instrumented;
    synchronized (instrumentedMap) {
      instrumented = instrumentedMap.get(obj);
      if (instrumented == null) {
        instrumented = new ObjectInstrumentationImpl(obj);
        instrumentedMap.put(obj, instrumented);
        created = true;
      }
    }
    if (created) {
      for (ObjectCreationListener listener : listeners) {
        listener.newObject(instrumented, Thread.currentThread());
      }
    }
    return instrumented;
  }

  /**
   * Method called by instrumented code from within the instrumented
   * constructor. Creates a new CallLogger that the instrumented object can then
   * use to record its internal method invocations.
   * <p>
   * Note that it is safe to call this multiple times for a given object. Only a
   * single CallLogger will be returned.
   */
  public static CallLogger createLoggerForNewObject(Object obj) {
    return getFactory().getLogger(obj);
  }
}
