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

import com.google.testing.instrumentation.Instrumenter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * An Instrumenter that transforms a class' bytecode in order to add tracing
 * calls to a {@link CallLogger}. The tracing can be used to create {@link
 * Breakpoint Breakpoints} that allow the execution of this class to be
 * suspended.
 * <p>
 * A TestInstrumenter is normally invoked via an {@link
 * com.google.testing.instrumentation.InstrumentedClassLoader}. If tests are run
 * using a {@link BaseThreadedTestRunner}, this is handled automatically. In
 * addition, a TestInstrumenter implements the {@link
 * java.lang.instrument.ClassFileTransformer} interface, so that it can be used
 * to instrument classes as they are loaded by the system classloader.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class TestInstrumenter implements Instrumenter, ClassFileTransformer {

  /** Set this to true to enable debugging */
  private static final boolean DEBUG = false;

  /*
   *  A note on implementation. Given a class like this:
   *
   *  public class SimpleInteger {
   *    int value;
   *
   *    public SimpleInteger(int value) {
   *      this.value = value;
   *    }
   *
   *    public int getValue() {
   *      return value;
   *    }
   *
   *    public synchronized int getAbsValue() {
   *      return Math.abs(value);
   *    }
   *  }
   *
   *
   * The transformed class will look something like that shown below. When
   * trying to understand the code in TestInstrumenter, it may be useful to
   * refer to this example. (Note that the actual calls to the
   * __testLogger.end() methods are wrapped in the equivalent of a try...finally
   * clause.)
   *
   * public class SimpleInteger {
   *
   *  // All method calls will be logged to this object.
   *  private CallLogger __testLogger;
   *
   *  // Table of methods, indexed by string. Allows us to pass
   *  // a Method object into the CallLogger
   *  private static final HashMap __methodTable = new HashMap();
   *
   *  static {
   *    __methodTable.put("getValue0",
   *        SimpleInteger.class.getDeclaredMethod("getValue", new Class[0]));
   *    __methodTable.put("getAbsValue0",
   *        SimpleInteger.class.getDeclaredMethod("getAbsValue", new Class[0]));
   *    __methodTable.put("SimpleInteger0",
   *        SimpleInteger.class.getDeclaredConstructor(new Class[] {int.class}));
   *    __methodTable.put("abs0",
   *        Math.class.getDeclaredMethod("abs", (new Class[] {int.class}));
   *  }
   *
   *  // Accessor for the Method table.
   *  private static Method __getMethod(String name) {
   *    return (Method)__methodTable.get(name);
   *  }
   *
   *  // Public accessor that defines the instrumented method in this class.
   *  public static List __getInstrumentation() {
   *    List methods = new ArrayList();
   *    {
   *      List lines = new ArrayList(1);
   *      lines.add(new LineInstrumentation(17));
   *      MethodInstrumentationImpl method =
   *          new MethodInstrumentationImpl(__getMethod("getValue0"), lines);
   *      methods.add(method);
   *    }
   *    return methods;
   *  }
   *
   *  // Instrumented methods
   *
   *  public SimpleInteger(int value) {
   *    this.value = value;
   *    __testLogger = CallLoggerFactory.createLoggerForNewObject(this);
   *  }
   *
   *  public int getValue() {
   *    __testLogger.start(__getMethod("getValue0"));
   *    __testLogger.atLine(17);
   *    __testLogger.end(__getMethod("getValue0"));
   *    return value;
   *  }
   *
   *  public int getAbsValue() {
   *    __testLogger.start(__getMethod("getAbsValue0"));
   *    int result = __synchronized_getAbsValue();
   *    __testLogger.end(__getMethod("getAbsValue0"));
   *    return result;
   *  }
   *
   *  private synchronized int __synchronized_getAbsValue() {
   *    __testLogger.beginCall(__getMethod("getAbsValue0"),
   *        __getMethod("abs0"));
   *    int result = Math.abs(value);
   *    __testLogger.endCall(__getMethod("getAbsValue0"),
   *        __getMethod("abs0"));
   *    return result;
   *  }
   */

  /**
   * The name of the static method that is added to each instrumented class in
   * order to get the instrumented data. The test framework invokes this method
   * in order to find the InstrumentedMethods in the class. The expected
   * signature is:
   * <pre>
   *  public static List<InstrumentedMethod> __getInstrumentation()
   * </pre>
   *
   */
  static final String GET_INSTRUMENTATION = "__getInstrumentation";


  // Names of new internal methods added to the instrumented class

  // Visible for testing
  protected static final String GET_METHOD = "__getMethod";

  // Visible for testing
  protected static final String METHOD_TABLE = "__methodTable";

  // Visible for testing
  protected static final String LOGGER = "__testLogger";

  // Per-class versions of the above names. We generate a name that is
  // guaranteed not to clash with other names in the instrumented class.
  private String getMethodName;
  private String methodTableName;
  private String loggerName;

  // Prefix for private version of synchronized methods
  private static final String SYNC_PREFIX = "__synchronized_";

  // Names of external classes and methods. References to these are added to the
  // instrumented code.
  private static final String INSTRUMENTED_METHOD = MethodInstrumentationImpl.class.getName();
  private static final String INSTRUMENTED_LINE = LineInstrumentation.class.getName();

  // Visible for testing
  static String FACTORY_CLASS = CallLoggerFactory.class.getName();

  private static final String GET_LOGGER = "createLoggerForNewObject";
  private static final String LOGGER_CLASS = CallLogger.class.getName();

  private static final String AT_LINE = "atLine";
  private static final String START_METHOD = "start";
  private static final String END_METHOD = "end";
  private static final String BEGIN_CALL = "beginCall";
  private static final String END_CALL = "endCall";

  private Set<String> instrumentedClasses;

  /**
   * Maps primitive type names into the primitive class. We need this becasue
   * we cannot use Class.forName("int") to yield int.class
   */
  private static final Map<String, Class<?>> primitives = new HashMap<String, Class<?>>();

  static {
    primitives.put("byte", byte.class);
    primitives.put("short", short.class);
    primitives.put("int", int.class);
    primitives.put("long", long.class);
    primitives.put("float", float.class);
    primitives.put("double", double.class);
    primitives.put("boolean", boolean.class);
    primitives.put("char", char.class);
  }

  /**
   * Maps primitive type names into the array qualifier for an array of that
   * type. We need this to convert from the CtClass' representation of an array
   * to the java representation. E.g. the classname of an array of "int" is
   * "[I".
   */
  private static final Map<String, String> primitiveArrayTypes = new HashMap<String,String>();

  static {
    primitiveArrayTypes.put("byte", "B");
    primitiveArrayTypes.put("short", "S");
    primitiveArrayTypes.put("int", "I");
    primitiveArrayTypes.put("long", "J");
    primitiveArrayTypes.put("float", "F");
    primitiveArrayTypes.put("double", "D");
    primitiveArrayTypes.put("boolean", "Z");
    primitiveArrayTypes.put("char", "C");
  }

  /**
   * Tracks the methods that have been instrumented. Provides a unique
   * identifying string for each method. Also tracks any methods that have been
   * renamed. An instance of this map is created for every class that is
   * processed by the TestInstrumenter.
   */
  private static class MethodMap {
    /**
     * Acts as a key for a CtMethod. The implementation of CtMethod.equals()
     * is flawed, as it only looks at the signature, and doesn't consider the
     * defining class. In order to distinguish an overridden method from a
     * superclass' method, we need a key that takes this into account.
     */
    private static class MethodReference {
      final CtMethod method;
      MethodReference(CtMethod method) {
        this.method = method;
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof MethodReference)) {
          return false;
        }
        MethodReference other = (MethodReference) obj;
        return method.equals(other.method) &&
            method.getDeclaringClass().equals(other.method.getDeclaringClass());
      }

      @Override
      public int hashCode() {
        int result = 17;
        result += 37 * method.hashCode();
        result += 37 * method.getDeclaringClass().hashCode();
        return result;
      }
    }

    /** Maps a CtMethod onto its unique name */
    private Map<MethodReference, String> map = new HashMap<MethodReference,String>();

    /** Maps a renamed CtMethod onto its original name */
    private Map<MethodReference, String> renameMap = new HashMap<MethodReference,String>();

    /** Maps a CtMethod onto its line number data */
    private Map<MethodReference, List<Integer>> lineMap =
        new HashMap<MethodReference,List<Integer>>();

    /**
     * Maps a method name onto a variable suffix. Used to generate unique
     * names.
     */
    private Map<String, Integer> suffixMap = new HashMap<String,Integer>();

    /**
     * Registers a CtMethod, plus an associated list of line numbers, in the
     * map, and returns the unique string that identifies it.
     */
    String registerMethodId(CtMethod newMethod, List<Integer> lines) {
      MethodReference reference = new MethodReference(newMethod);
      String uniqueName = map.get(reference);
      if (uniqueName == null) {
        String methodName = newMethod.getName();
        Integer suffix = suffixMap.get(methodName);
        if (suffix == null) {
          suffix = Integer.valueOf(0);
        } else {
          suffix = Integer.valueOf(suffix.intValue() + 1);
        }
        uniqueName = methodName + suffix;
        suffixMap.put(methodName, suffix);
        map.put(reference, uniqueName);
      }
      // We may previously have been called with lines == null,
      // so always update the lines here.
      if (lines != null) {
        lineMap.put(reference, lines);
      }
      return uniqueName;
    }

    /**
     * Registers a CtMethod in the map, and returns the unique string that
     * identifies it.
     */
    String registerMethodId(CtMethod newMethod) {
      return registerMethodId(newMethod, null);
    }

    /**
     * Gets all the instrumented methods.
     */
    List<CtMethod> getMethods() {
      Set<MethodReference> refs = map.keySet();
      List<CtMethod> methods = new ArrayList<CtMethod>(refs.size());
      for (MethodReference ref : refs) {
        methods.add(ref.method);
      }
      return methods;
    }

    /**
     * Returns the unique string that identifies a CtMethod, or null if the
     * behaviour is unknown.
     */
    String getMethodId(CtMethod method) {
      return map.get(new MethodReference(method));
    }

    /**
     * Registers a method that has been renamed.
     * @see #getOriginalName
     */
    void logRenamedMethod(CtMethod method, String originalName) {
      renameMap.put(new MethodReference(method), originalName);
    }

    /**
     * Returns the original name of a renamed method.
     */
    String getOriginalName(CtMethod method) {
      String result = renameMap.get(new MethodReference(method));
      if (result == null) {
        result = method.getName();
      }
      return result;
    }

    /**
     * Returns a list of line numbers in a given method.
     */
    List<Integer> getLines(CtMethod method) {
      return lineMap.get(new MethodReference(method));
    }
  }

  /**
   * Creates a new TestInstrumenter that will instrumented the given list of
   * named classes. These names should be in the format returned by {@link
   * Class#getName}.
   */
  public TestInstrumenter(List<String> classes) {
    instrumentedClasses = new HashSet<String>(classes);
  }

  /**
   * The premain method that will be invoked if the TestInstrumenter is used in
   * conjunction with the -javaagent command-line option. See the package
   * documentation for java.lang.instrument.
   *
   * @param agentArgument a comma-separated list of classnames to instrument
   * @param instrumentation the JVM's instrumentation service.
   */
  public static void premain(String agentArgument, Instrumentation instrumentation) {
    if (agentArgument != null) {
      String[] args = agentArgument.split(",");
      instrumentation.addTransformer(new TestInstrumenter(args));
    } else {
      System.err.printf("No classes defined oncommand line\n");
    }
  }

  /**
   * Constructor used by {@link #premain}.
   */
  private TestInstrumenter(String[] classes) {
    instrumentedClasses = new HashSet<String>();
    for (String clss : classes) {
      // Transform com.google.someclass into com/google/someclass
      StringBuffer className = new StringBuffer(clss);
      for (int i = 0; i < className.length(); i++) {
        if (className.charAt(i) == '.') {
          className.setCharAt(i, '/');
        }
      }
      debugPrint("TestInstrumenter - instrumenting %s\n", className.toString());
      instrumentedClasses.add(className.toString());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public byte[] transform(ClassLoader loader, String className, Class clss,
      ProtectionDomain domain, byte[] bytes) {
    return instrument(className, bytes);
  }

  @SuppressWarnings("unchecked")
  @Override
  public byte[] instrument(String className, byte[] bytes) {
    if (instrumentedClasses.contains(className)) {
      debugPrint("Transforming %s\n", className);
      try {
        return processClass(className, bytes);
      } catch (CannotCompileException e) {
        throw new RuntimeException("Cannot instrument class", e);
      }
    } else {
      return bytes;
    }
  }

  /**
   * Instruments a given class.
   */
  private byte[] processClass(String name, byte[] bytes) throws CannotCompileException {
    ClassPool pool = ClassPool.getDefault();
    CtClass cl = null;
    try {
      cl = pool.makeClass(new ByteArrayInputStream(bytes));
      if (cl.isInterface()) {
        throw new IllegalArgumentException("Cannot instrument interfaces");
      }

      // Generate unique names for the methods and fields that we will be adding.
      getMethodName = cl.makeUniqueName(GET_METHOD);
      methodTableName = cl.makeUniqueName(METHOD_TABLE);
      loggerName = cl.makeUniqueName(LOGGER);

      addDeclaration(cl);
      addGetMethod(cl);
      for (CtConstructor constructor : cl.getDeclaredConstructors()) {
        processConstructor(constructor);
      }
      MethodMap methodMap = new MethodMap();
      for (CtMethod method : cl.getDeclaredMethods()) {
        processMethod(cl, method, methodMap);
      }
      addGetInstrumentedMethods(cl, methodMap);
      addMethodTableInitializer(cl, methodMap);
      bytes = cl.toBytecode();
    } catch (IOException e) {
      // Shouldn't happen with a ByteArrayInputStream
      throw new RuntimeException("Cannot process class data", e);
    } finally {
      if (cl != null) {
        if (DEBUG) {
          cl.debugWriteFile("/tmp");
        }
        cl.detach();
      }
    }
    return bytes;
  }

  /**
   * Creates a new private field for the logger
   */
  private void addDeclaration(CtClass cl) throws CannotCompileException {
    String loggerDeclaration = "private " + LOGGER_CLASS + " " + loggerName + ";";
    CtField field = CtField.make(loggerDeclaration, cl);
    cl.addField(field);
  }

  /**
   * Creates a static private method that returns a java.lang.reflect.Method object
   * for a given method string
   */
  private void addGetMethod(CtClass cl) throws CannotCompileException {

    CtField methodTable = CtField.make("private static final java.util.HashMap " +
        methodTableName + " = new java.util.HashMap();\n", cl);
    cl.addField(methodTable);
    StringBuilder code = new StringBuilder();
    code.append("  private static java.lang.reflect.Method ").append(getMethodName)
        .append("(String name) {\n");
    code.append("    return (java.lang.reflect.Method)").append(methodTableName)
        .append(".get(name);\n  }\n");
    debugPrint("Adding method %s\n", code);
    CtMethod newMethod = CtMethod.make(code.toString(), cl);
    cl.addMethod(newMethod);
  }

  /**
   * Adds a prefix to a method name. Used when renaming a synchronized method.
   */
  private String addSyncPrefix(CtClass clss, String name) {
    return clss.makeUniqueName(SYNC_PREFIX + name);
  }

  /**
   * Creates a static class initializer that creates the table used in
   * GET_METHOD
   */
  private void addMethodTableInitializer(CtClass cl, MethodMap methodMap)
      throws CannotCompileException {
    CtConstructor initializer = cl.makeClassInitializer();

    StringBuilder code = new StringBuilder();
    code.append("  {\n");

    for (CtMethod methodDescriptor : methodMap.getMethods()) {
      String methodName = methodMap.getOriginalName(methodDescriptor);
      CtClass definingClass = methodDescriptor.getDeclaringClass();
      String methodId = methodMap.getMethodId(methodDescriptor);
      List<Class<?>> paramClasses = getParameterClasses(methodDescriptor);
      String paramArgs = getParameterArg(paramClasses);
      code.append("    ").append(methodTableName).append(".put(\"");
      code.append(methodId).append("\", ");
      code.append(definingClass.getName()).append(".class.");
      code.append("getDeclaredMethod(\"").append(methodName).append("\", ");
      code.append(paramArgs).append("));\n");
    }
    code.append("  }\n");
    debugPrint("Adding method table %s\n", code);
    initializer.insertBefore(code.toString());
  }


  /**
   * Creates a static method that returns a List of InstrumentedMethod objects
   * describing the Methods in this class
   */
  private void addGetInstrumentedMethods(CtClass cl, MethodMap methodMap)
      throws CannotCompileException {
    StringBuilder code = new StringBuilder();
    code.append("  public static java.util.List ").append(GET_INSTRUMENTATION).append("() {\n");
    code.append("    java.util.List methods = new java.util.ArrayList();\n");

    for (CtMethod methodDescriptor : methodMap.getMethods()) {
      // Generate the code to add the list of lines to the method
      List<Integer> lines = methodMap.getLines(methodDescriptor);
      if (lines == null) {
        debugPrint("No lines for %s\n", methodDescriptor);
        continue;
      }
      code.append("    {\n      java.util.List lines = new java.util.ArrayList(");
      code.append(lines.size()).append(");\n");

      for (Integer line : lines) {
        code.append("      lines.add(new ").append(INSTRUMENTED_LINE);
        code.append("(").append(line).append("));\n");
      }

      String methodId = methodMap.getMethodId(methodDescriptor);
      code.append("      ").append(INSTRUMENTED_METHOD).append(" method = new ");
      code.append(INSTRUMENTED_METHOD);
      code.append("(").append(getMethodName).append("(\"");
      code.append(methodId).append("\"), lines);\n").append("      methods.add(method);\n    }\n");
    }

    code.append("    return methods;\n  }\n");
    debugPrint("Adding method \n%s\n", code);

    CtMethod newMethod = CtMethod.make(code.toString(), cl);
    cl.addMethod(newMethod);
  }

  /**
   * Returns the Class object corresponding to a CtClass.
   */
  static Class<?> getClass(CtClass clss) {
    String name = clss.getName();
    Class<?> result = primitives.get(name);
    if (result == null) {
      // Work out the array dimensionality (if any).  CtClass uses "int[][]" to
      // refer to "[[I"
      int end = name.length()-1;
      int dimension = 0;
      while (end > 0 && name.charAt(end) == ']') {
        dimension++;
        end -= 2;
      }
      if (dimension > 0) {
        String baseType = name.substring(0, end + 1);
        String arrayType = primitiveArrayTypes.get(baseType);
        if (arrayType == null) {
          arrayType = "L" + baseType + ";";
        }
        String prefix = "[";
        for (int i = 1; i < dimension; i++) {
          prefix += "[";
        }
        name = prefix + arrayType;
        debugPrint("Looking up array type %s\n", name);
      }
      try {
        result = Class.forName(name);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    return result;
  }

  /**
   * Returns a list of the classes of the parameters to a given method.
   */
  private List<Class<?>> getParameterClasses(CtBehavior method) {
    try {
      CtClass[] params =  method.getParameterTypes();
      List<Class<?>> result = new ArrayList<Class<?>>(params.length);
      for (CtClass paramClass : params) {
        result.add(getClass(paramClass));
      }
      return result;
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Converts a list of parameter classes to a String representation
   * of an array of class objects. E.g. given the parameter classes from
   * <code>aMethod(int x, String y, List<Integer> z)</code>,
   * returns "new Class[] {int.class, java.lang.String.class, java.util.List.class}";
   */
  private String getParameterArg(List<Class<?>> paramClasses) {
    if (paramClasses.size() == 0) {
      // Javassist seems unhappy with "new Class[] {}", so just return a special
      // empty array here.
      return "new Class[0]";
    } else {
      StringBuilder result = new StringBuilder();
      result.append("new Class[] {");
      int len = paramClasses.size();
      for (int i = 0; i < len; i++) {
        Class<?> paramClass = paramClasses.get(i);
        // If we're dealing with an array, we need to use Class.forName(). There
        // are no built-in type variables for arrays. If it's not an array, just
        // use the class name and the ".class" extension.
        if (paramClass.isArray()) {
          result.append("Class.forName(\"").append(paramClass.getName()).append("\")");
        } else {
          result.append(paramClass.getName()).append(".class");
        }
        if (i < len - 1) {
          result.append(", ");
        }
      }
      result.append("}");
      return result.toString();
    }
  }

  /**
   * Handles a constructor. Adds a call to create a new CallLogger for the new
   * object. Note that this code is added to each constructor. If one
   * constructor invokes another, then we will make multiple calls to
   * CallLoggerFactory.createLoggerForNewObject() This is OK, as the method can
   * be called multiple times.
   */
  private void processConstructor(CtConstructor constructor) throws CannotCompileException {
    StringBuilder before = new StringBuilder();

    // Build the call that logs the object's creation. Note that $0 is a
    // Javassist identifier representing 'this'
    before.append(loggerName).append(" = ").append(FACTORY_CLASS).append(".").
        append(GET_LOGGER).append("($0);");
    debugPrint("    For construtor, Before = %s\n", before);

    // Note that we are inserting this method at the start of the
    // constructor. The compiler wouldn't let us do this, but the bytecode
    // verifier doesn't mind. By creating the logger straight away, we guarantee
    // that it won't be null if the constructor invokes any other methods on
    // 'this'.
    constructor.insertBeforeBody(before.toString());
  }

  private String getBeforeLogging(String methodId) {
    StringBuilder before = new StringBuilder();
    before.append(loggerName).append(".").append(START_METHOD).append("(");
    before.append(getMethodName).append("(\"").append(methodId).append("\"));");
    debugPrint("    Before = %s\n", before);
    return before.toString();
  }

  private String getAfterLogging(String methodId) {
    StringBuilder after = new StringBuilder();
    after.append(loggerName).append(".").append(END_METHOD).append("(");
    after.append(getMethodName).append("(\"").append(methodId).append("\"));\n");
    debugPrint("    After = %s\n", after);
    return after.toString();
  }

  private void processMethod(CtClass clss, CtMethod method, final MethodMap methodMap)
      throws CannotCompileException {
    debugPrint("   Processing %s\n", method.getName());
    MethodInfo methodInfo = method.getMethodInfo();
    if (methodInfo.isStaticInitializer()) {
      return;
    }
    int modifiers = method.getModifiers();
    if (Modifier.isStatic(modifiers)) {
      return;
    }
    boolean isSynchronized = Modifier.isSynchronized(modifiers);
    String name = method.getName();

    // Don't process the GET_METHOD method that we have already added to this
    // class. It's not part of the original class. (Note that we have to add
    // GET_METHOD before we process the other methods in the class, otherwise we
    // won't be able to compile calls to it. See the varous calls to 'replace"
    // below.)
    if (name.equals(getMethodName)) {
      return;
    }
    CodeAttribute codeAttr = methodInfo.getCodeAttribute();
    int byteCodeLength = codeAttr == null ? 0 : codeAttr.getCode().length;
    if (byteCodeLength == 0) {
      debugPrint("    No byteCode in method???\n");
      return;
    }

    List<Integer> lineNumbers = new ArrayList<Integer>();
    Integer prevLine = methodInfo.getLineNumber(0);
    lineNumbers.add(prevLine);
    for (int i = 0; i < byteCodeLength; i++) {
      int line = methodInfo.getLineNumber(i);
      if (line > prevLine) {
        prevLine = Integer.valueOf(line);
        lineNumbers.add(line);
      }
    }

    final String thisMethodId = methodMap.registerMethodId(method, lineNumbers);

    // Instrument each external method call made within this method body. We
    // wrap each invocation with a cal to BEGIN_CALL and END_CALL.
    method.instrument(
        new ExprEditor() {
          @Override
          public void edit(MethodCall called) throws CannotCompileException {
            try {
              CtMethod calledMethod = called.getMethod();
              String calledMethodId = methodMap.registerMethodId(calledMethod);
              String methodReplacement;
              CtClass returnType = calledMethod.getReturnType();
              boolean isVoid = returnType == CtClass.voidType;
              if (isVoid) {
                methodReplacement = "{$proceed($$);}";
              } else {
                methodReplacement = "{$_ = $proceed($$);}";
              }
              // Build arguments for the BEGIN_CALL and END_CALL methods. These
              // are the method doing the invocation (thisMethodId), the line
              // number, and the method being invoked

              StringBuilder loggerArgs = new StringBuilder();
              loggerArgs.append(getMethodName).append("(\"").append(thisMethodId).append("\"), ");
              loggerArgs.append(called.getLineNumber()).append(", ");
              loggerArgs.append(getMethodName).append("(\"").append(calledMethodId).append("\")");

              StringBuilder replacement = new StringBuilder();
              replacement.append("{").append(loggerName).append(".").append(BEGIN_CALL).append("(");
              replacement.append(loggerArgs).append(");} ");

              replacement.append(methodReplacement);

              replacement.append("{").append(loggerName).append(".").append(END_CALL).append("(");
              replacement.append(loggerArgs).append(");} ");
              debugPrint("    Replacing with \"%s\"\n", replacement);
              called.replace(replacement.toString());

            } catch (NotFoundException e) {
              throw new CannotCompileException(e);
            }
          }
        });



    // For normal methods, add the logger calls at the start and end of the
    // method.  For synchronized methods, we need to rename the method to a
    // private one, and then create a public non-synchronized wrapper.
    //
    // Note that we want to add the before/after calls AFTER we have
    // instrumented the other external calls, otherwise these LOGGER
    // methods well be intrumented too...
    //

    if (!isSynchronized) {
      method.insertBefore(getBeforeLogging(thisMethodId));

      // Pass 'true' in to insertAfter in order to ensure that we always execute
      // the after code, even if an Exception is thrown.
      method.insertAfter(getAfterLogging(thisMethodId), true);
    } else {
      addSynchronizedWrapper(clss, method, thisMethodId, methodMap);
    }

    for (Integer line : lineNumbers) {
      StringBuilder atLine = new StringBuilder();
      atLine.append("{").append(loggerName).append(".").append(AT_LINE);
      atLine.append("(").append(line).append(");}");
      debugPrint("   Inserting %s at %d\n", atLine, line);

      int inserted = method.insertAt(line, atLine.toString());
      if (inserted != line) {
        throw new IllegalStateException("Tried to insert at " + line + ", got " + atLine);
      }
    }
  }

  /**
   * Wraps a synchronized method in a non-synchronized wrapper. The original
   * method is renamed and made private, and a non-synchronized method is
   * created as a wrapper. The before/after blocks are added to the wrapper.
   */

  private void addSynchronizedWrapper(CtClass clss, CtMethod originalMethod, String thisMethodId,
      MethodMap methodMap) throws CannotCompileException {
    StringBuilder wrapper = new StringBuilder();
    try {
      CtClass returnType = originalMethod.getReturnType();
      boolean isVoid = returnType == CtClass.voidType;


      int originalModifiers = originalMethod.getModifiers();
      int wrapperModifiers = originalModifiers;
      originalModifiers = Modifier.setPrivate(originalModifiers);
      originalMethod.setModifiers(originalModifiers);
      wrapperModifiers = wrapperModifiers & (~Modifier.SYNCHRONIZED);

      String name = originalMethod.getName();
      String privateName = addSyncPrefix(clss, name);
      originalMethod.setName(privateName);
      methodMap.logRenamedMethod(originalMethod, name);
      originalMethod.setModifiers(originalModifiers);
      CtMethod wrapperMethod = CtNewMethod.copy(originalMethod, name, clss, null);


      wrapper.append(" {\n");
      wrapper.append(getBeforeLogging(thisMethodId));
      wrapper.append("\n  try {\n    ");
      if (!isVoid) {
        wrapper.append("return ");
      }
      wrapper.append(privateName).append("($$);\n");
      wrapper.append("  } finally {\n    ");
      wrapper.append(getAfterLogging(thisMethodId));
      wrapper.append("  }\n}\n");
      debugPrint("Wrapper = \n%s\n", wrapper);
      wrapperMethod.setBody(wrapper.toString());
      wrapperMethod.setModifiers(wrapperModifiers);
      clss.addMethod(wrapperMethod);

    } catch (NotFoundException e) {
      throw new CannotCompileException(e);
    }
  }

  private static void debugPrint(String format, Object... args) {
    if (DEBUG) {
      System.out.printf(format, args);
    }
  }
}
