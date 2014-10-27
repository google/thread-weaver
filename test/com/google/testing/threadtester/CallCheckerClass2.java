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

/**
 * Simple class that calls other classes. Used by CallCheckerTest
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class CallCheckerClass2 {

  SimpleClass6 simple6;

  public void caller() {
    simple6.method1();
  }

  public void caller2() {
    simple6.callSimpleClass();
  }
}