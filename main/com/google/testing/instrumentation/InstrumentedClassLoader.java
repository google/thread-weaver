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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * A custom ClassLoader that performs byte-code instrumentation on all loaded
 * classes.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public final class InstrumentedClassLoader extends ClassLoader {

  /** Initial size of buffer used to read class data. */
  /*  Note: should be enough for most classes, and is not a hard limit. */
  private static final int BUF_SIZE = 4096;

  private final Instrumenter instrumenter;

  /**
   * List of class prefixes that this loader doesn't load, but delegates to the
   * parent.
   */
  /* Note - we run into various obscure errors with on-the-fly classes if we try
   * to load sun packages, so delegate these to the parent class loader. Also
   * delegate java and javax packages, on the assumption that we don't want to
   * instrument these.
   *
   * We also exclude classes from the JUnit and EasyMock test frameworks, from
   * Objenesis, and from org.w3c.dom, on the grounds that we are not likely to
   * test these directly. We have found classloader/native lib issues with
   * the jdom library, so exclude that as well.
   *
   * TODO(alasdair): determine if we want to provide an interface that lets
   * callers specify this.
   */
  private static final List<String> excludedClassPrefixes =
      Arrays.asList("java.", "javax.", "sun.", "net.sf.cglib", "junit.",
          "org.junit.", "org.objenesis.", "org.easymock.", "org.w3c.dom", "org.jdom");

  /**
   * Creates a new instrumented class loader using the given {@link
   * Instrumenter}. All classes loaded by this loader will have their byte-code
   * passed through the {@link Instrumenter#instrument} method.
   *
   * @param instrumenter the instrumenter
   */
  public InstrumentedClassLoader(Instrumenter instrumenter){
    super(InstrumentedClassLoader.class.getClassLoader());
    if (instrumenter == null) {
      throw new IllegalArgumentException("instrumenter cannot be null");
    }
    this.instrumenter = instrumenter;
  }

  /**
   * Returns true if this class should be loaded by this classloader. If not,
   * then loading delegates to the parent.
   */
  private boolean shouldLoad(String className) {
    for (String excluded : excludedClassPrefixes) {
      if (className.startsWith(excluded)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Loads a class, and throws an IllegalArgumentException if the class cannot
   * be loaded. Useful for classes which we expect to be able to find, e.g. for
   * currently loaded classes that are being reloaded by this
   * InstrumentedClassLoader.
   *
   * @param name the full name of the class
   *
   * @return the loaded class
   */
  public Class<?> getExpectedClass(String name) {
    try {
      return findClass(name);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Cannot find " + e);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded. If not, load it
    // ourselves or delegate to the parent.
    Class<?> result = findLoadedClass(name);
    if (result == null) {
      if (shouldLoad(name)) {
        result = findClass(name);
      } else {
        return super.loadClass(name, resolve);
      }
    }
    if (resolve) {
      resolveClass(result);
    }
    return result;
  }

  @Override
  public Class<?> findClass(String className) throws ClassNotFoundException {
    try {
      // Create a package for this class, unless it's in the default package.
      int dotpos = className.lastIndexOf('.');
      if (dotpos != -1) {
        String pkgname = className.substring(0, dotpos);
        if (getPackage(pkgname) == null) {
          definePackage(pkgname, null, null, null, null, null, null, null);
        }
      }
      String resourceName = className.replace('.', '/') + ".class";
      InputStream input = getSystemResourceAsStream(resourceName);
      byte[] classData =  instrumenter.instrument(className, loadClassData(input));
      Class<?> result = defineClass(className, classData, 0, classData.length, null);
      return result;
    } catch (IOException e) {
      throw new ClassNotFoundException("Cannot load " + className, e);
    }
  }

  /**
   * Load class data from a given input stream.
   */
  private byte[] loadClassData(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream(BUF_SIZE);
    byte[] buffer = new byte[BUF_SIZE];
    int readCount;
    while ((readCount = input.read(buffer, 0, BUF_SIZE)) >= 0) {
      output.write(buffer, 0, readCount);
    }
    return output.toByteArray();
  }
}
