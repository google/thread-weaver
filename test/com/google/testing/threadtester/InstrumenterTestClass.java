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

import java.io.IOException;

/**
 * Class used for testing the TestInstrumenter. Contains various methods that
 * will be instrumented. See {@link TestInstrumenterTest}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class InstrumenterTestClass {

  public void stringArg(String[][] s) {
  }

  public void intArg(int[] arg) {
  }

  public InstrumenterTestClass() {
    this("");
  }

  public InstrumenterTestClass(String name) {
  }

  public void throwException() throws IOException {
    throw new IOException();
  }

  public void throwRuntimeException() {
    throw new IllegalArgumentException();
  }

  public void callVarArgs() {
    varArgs("Hello", ", ", "World");
  }

  public void varArgs(String... strings) {
  }

  public void overloaded(int a) {
  }

  public void overloaded(String s) {
  }
}
