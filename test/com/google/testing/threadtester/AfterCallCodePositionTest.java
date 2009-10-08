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

/**
 * Unit tests for AfterCallCodePosition.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class AfterCallCodePositionTest extends CallCodePositionTest {

  public void testConstructor_throwsExceptionWithNullMethod() {
    try {
      AfterCallCodePosition position = new AfterCallCodePosition(null, "method1");
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testConstructor_throwsExceptionWithNullCalledMethod() throws Exception {
    Method caller = DummyClassWithMethods.class.getDeclaredMethod("method1");
    Method called = null;
    try {
      AfterCallCodePosition position = new AfterCallCodePosition(caller, called);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testConstructor_throwsExceptionWithNullCalledMethodName() throws Exception {
    Method caller = DummyClassWithMethods.class.getDeclaredMethod("method1");
    String called = null;
    try {
      AfterCallCodePosition position = new AfterCallCodePosition(caller, called);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testMatches_doesNotmatchBeforeCodePosition() throws Exception {
    AfterCallCodePosition after = (AfterCallCodePosition) getFirst();
    BeforeCallCodePosition before = new BeforeCallCodePosition(
        DummyClassWithMethods.class.getDeclaredMethod("method1"),
        DummyClassWithMethods.class.getDeclaredMethod("method2"));
    assertFalse(after.matches(before));
  }

  @Override
  protected CallCodePosition getFirst() throws Exception {
    return new AfterCallCodePosition(
        DummyClassWithMethods.class.getDeclaredMethod("method1"),
        DummyClassWithMethods.class.getDeclaredMethod("method2"));
  }

  @Override
  protected CallCodePosition getSecond() throws Exception {
    return new AfterCallCodePosition(
        DummyClassWithMethods.class.getDeclaredMethod("method2"),
        DummyClassWithMethods.class.getDeclaredMethod("method1"));
  }

  @Override
  protected CallCodePosition getNamedFirst() throws Exception {
    return new AfterCallCodePosition(
        DummyClassWithMethods.class.getDeclaredMethod("method1"), "method2");
  }

  @Override
  protected CallCodePosition getNamedSecond() throws Exception {
    return new AfterCallCodePosition(
        DummyClassWithMethods.class.getDeclaredMethod("method2"), "method1");
  }

  @Override
  protected CodePosition getAny() {
    return null;
  }
}
