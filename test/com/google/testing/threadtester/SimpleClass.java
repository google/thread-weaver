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

import junit.framework.AssertionFailedError;

import java.io.IOException;

/**
 * Simple class with a variety of methods. Used to test Breakpoints and
 * CodePositions.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SimpleClass {

  /**
   * Tracks the execution position within this class' methods. Accessed as a
   * field not a method, so that it can be accessed without going through
   * instrumented code.
   */
  volatile int position = 0;

  private volatile SimpleClass2 second;

  public void setSecond(SimpleClass2 second) {
    this.second = second;
  }

  public int add(int a, int b) {
    position = 1;
    position = 2;
    int result = a + b;
    return result;
  }

  public int add(Integer a, Integer b) {
    position = 3;
    position = 4;
    return a + b;
  }

  public int add(SimpleInteger a, SimpleInteger b) {
    position = 5;
    position = 6;
    return a.getValue() + b.getValue();
  }

  public int unique() {
    position = 7;
    innerMethod();
    position = 8;
    return 0;
  }

  public int unique2() {
    position = 9;
    innerMethod();
    position = 10;
    return 0;
  }

  public void innerMethod() {
    position = 11;
  }

  public int callSecond() {
    position = 12;
    if (second != null) {
      second.setPosition(this, 13);
    }
    position = 14;
    return 0;
  }

  public int nonBlocking() {
    return 0;
  }

  public int blocking() throws InterruptedException {
    Thread.sleep(Options.timeout() * 10);
    return 0;
  }

  public synchronized int blockingInternally() throws InterruptedException {
    blocking();
    return 0;
  }

  public synchronized int synchronizedNonBlocking() {
    return 0;
  }

  public int throwException() throws IOException {
    throw new IOException();
  }

  public int throwRuntimeException() {
    throw new IllegalArgumentException();
  }

  public int throwError() {
    throw new AssertionFailedError();
  }
}
