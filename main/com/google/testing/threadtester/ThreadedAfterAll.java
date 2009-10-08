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
 * An optional annotation that designates part of a test suite that uses the
 * {@link AnnotatedTestRunner} framework to perform multithreaded tests. The
 * method tagged with the ThreadedAfterAll attribute will be run once after all
 * test cases have been run. Note that this annotation should only be applied to
 * a static method.
 *
 * @see AnnotatedTestRunner
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ThreadedAfterAll {
}
