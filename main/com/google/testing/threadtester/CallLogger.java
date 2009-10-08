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
 *
 * An instance of a CallLogger is provided for every instance of an instrumented
 * class under test. The bytecode of the instrumented class is modified to
 * invoke the CallLogger during execution of its own methods. E.g. whenever a
 * call is made to a method in the instrumented class, the
 * {@link #start(Method)} method is invoked on the CallLogger. This allows
 * execution of the instrumented class to be controlled. The methods in this
 * class should only be invoked as described above, and should not be invoked by
 * normal test code.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 *
 */
public interface CallLogger {

  public void start(Method method);

  public void end(Method method);

  public void beginCall(Method source, int line, Method target);

  public void endCall(Method source, int line, Method target);

  public void atLine(int line);
}
