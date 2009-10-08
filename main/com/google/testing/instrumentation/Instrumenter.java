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

package com.google.testing.instrumentation;


/**
 * An object that transforms the byte code that defines a class.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */

public interface Instrumenter {

  /**
   * Transforms an array of bytes that defines a class.
   *
   * @param className the name of the class being defined.
   * @param classData the bytes that make up the class data.
   *
   * @return the transformed bytecode. A given Instrumenter may chose not to
   * transform all classes, in which case it may just return the given
   * classData.
   *
   */
  byte[] instrument(String className, byte[] classData);

}
