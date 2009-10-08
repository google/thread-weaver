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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that designates a test that uses the {@link ThreadedTestRunner}
 * framework to perform multithreaded tests.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ThreadedTest {

  /** Default value for the {@link #expected} parameter. */
  static class NoException extends Throwable {
    // Empty class, specified because we need a non-null default.
  }

  /**
   * An optional <code>expected</code> Throwable. If this is specified, the
   * given test method is expected to throw the given exception class during its
   * execution. Failure to throw the specified exception is an error.
   */
  Class<? extends Throwable> expected() default NoException.class;
}
