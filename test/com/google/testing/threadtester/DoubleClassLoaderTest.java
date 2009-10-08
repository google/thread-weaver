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


import junit.framework.TestCase;

/**
 * Test that we can create a MethodRecorder control object in two
 * separate tests. We had a classloader issue which meant that the
 * second control object would be created in the classloader used by
 * the first test. This test verifies that the issue is fixed.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class DoubleClassLoaderTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.runTests(getClass(), SimpleClass.class);
  }

  public void testThreadedTestsAgain() {
    runner.runTests(getClass(), SimpleClass.class);
  }


  @ThreadedTest
  public void getControl() {
    SimpleClass instance = new SimpleClass();
    MethodRecorder<SimpleClass> recorder = new MethodRecorder<SimpleClass>(SimpleClass.class);
    // The following assigment will fail if the classloader for the
    // control object is wrong.
    SimpleClass control = recorder.getControl();
  }
}
