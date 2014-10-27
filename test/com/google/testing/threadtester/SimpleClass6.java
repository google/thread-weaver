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
 * Simple test class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class SimpleClass6 {

  SimpleClass calledClass;

  public void method1() {
    protected1();
  }

  protected void protected1() {
    private1();
  }

  protected void protected1(int arg) {
    private2(arg);
  }

  private void private1() {
  }

  private void private2(int arg) {
  }

  public void callSimpleClass() {
    calledClass.unique();
  }
}
