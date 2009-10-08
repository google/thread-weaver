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

import java.util.List;

/**
 * Interface for running a set of multi-threaded tests. A BaseThreadedTestRunner
 * loads a new instance of this class to perform the actual test run.
 * <p>
 * Note that because the test classes are loaded in a separate class loader, all
 * access must be done via reflection. The various implementations of
 * BaseTestWrapper simplify this process by having a single entry point that the
 * BaseThreadedTestRunner can invoke. The implementation can then make compiled
 * calls into the test classes.
 *
 * @see BaseThreadedTestRunner
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface BaseTestWrapper {

  /**
   * Runs the multithreaded tests defined in the given test class.
   */
  /*
   * NOTE: this method is invoked by reflection from BaseThreadedTestRunner. Do
   * not change this method name without updating BaseThreadedTestRunner.
   */
  void runTests(Class<?> testClass, List<String> instrumentedClasses) throws Exception;

}
