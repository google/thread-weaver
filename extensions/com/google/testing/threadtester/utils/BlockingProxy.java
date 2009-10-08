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

package com.google.testing.threadtester.utils;

import com.google.testing.threadtester.AbstractBreakpoint;
import com.google.testing.threadtester.ReusableBreakpoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides a proxy that can be used to control thread execution by blocking
 * at a given point. It can be used to proxy method calls to a fake test
 * object, and provide a BreakPoint at the point where a particular method
 * is called.
 *
 * @param <T> the fake object being proxied
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class BlockingProxy<T> extends AbstractBreakpoint
    implements ReusableBreakpoint, InvocationHandler {

  /** The underlying object (typically a fake for testing).*/
  private T original;

  /** The dynamic proxy object that wraps the {@link #original}. */
  private T proxy;

  /** The name of the method where this BlockingProxy should block */
  private String methodName;

  /** The Method where this BlockingProxy should block */
  private Method targetMethod;

  /** If true, block before the method, otherwise block afterwards. */
  private boolean before;

  /**
   * Gets the dynamic proxy object. All methods invked on this will be proxied to the
   * underlying original object
   */
  public T getProxy() {
    return proxy;
  }

  private BlockingProxy(T original, String methodName, boolean before) {
    super();
    this.original = original;
    this.methodName = methodName;
    this.targetMethod = null;
    this.before = before;
  }

  private BlockingProxy(T original, Method method, boolean before) {
    super();
    this.original = original;
    this.methodName = null;
    this.targetMethod = method;
    this.before = before;
  }

  @Override
  public Object invoke(Object proxy, Method invoked, Object[] args) throws Throwable {
    boolean block;
    if (targetMethod != null) {
      block = targetMethod.equals(invoked);
    } else {
      block = methodName.equals(invoked.getName());
    }
    if (block && before) {
      doBlock();
    }
    Object result = invoked.invoke(original, args);
    if (block && !before) {
      doBlock();
    }
    return result;
  }

  private void doBlock() {
    if (thread == null || thread.equals(Thread.currentThread())) {
      hitBreakpoint();
    }
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  @Override
  public void setThread(Thread thread) {
    setThreadImpl(thread);
  }

  /**
   * Creates a new BlockingProxy for the given object. The proxy will block
   * at the given named method.
   *
   * @param clss the class being proxied. Must be an interface.
   * @param original the object being proxied. Must implement the given interface.
   * @param methodName the name of the method where this proxy will block.
   * @param before if true, block before calling the given method. If false,
   * block afterwards.
   */
  @SuppressWarnings("unchecked")
  public static <T> BlockingProxy<T> create(Class<T> clss,
                                            T original,
                                            String methodName,
                                            boolean before) {
    BlockingProxy<T> blockingProxy = new BlockingProxy(original, methodName, before);
    blockingProxy.proxy =
        (T) Proxy.newProxyInstance(clss.getClassLoader(), new Class[] {clss }, blockingProxy);
    return blockingProxy;
  }

  /**
   * Creates a new BlockingProxy for the given object. The proxy will block
   * at the given method.
   *
   * @param clss the class being proxied. Must be an interface.
   * @param original the object being proxied. Must implement the given interface.
   * @param method the method where this proxy will block.
   * @param before if true, block before calling the given method. If false,
   * block afterwards.
   */
  @SuppressWarnings("unchecked")
  public static <T> BlockingProxy<T> create(Class<T> clss,
                                            T original,
                                            Method method,
                                            boolean before) {
    BlockingProxy<T> blockingProxy = new BlockingProxy(original, method, before);
    blockingProxy.proxy =
        (T) Proxy.newProxyInstance(clss.getClassLoader(), new Class[] {clss }, blockingProxy);
    return blockingProxy;
  }
}
