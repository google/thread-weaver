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


import junit.framework.TestCase;

import java.util.List;

/**
 * Tests {@link Scripter} and {@link Script}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class ScripterTest extends TestCase {

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.setDebug(true);
    int count = 0;
    try {
      while (count <= 0) {
        count++;
        runner.runTests(getClass(), SimpleClass3.class, SimpleClass4.class);
      }
    } finally {
      System.out.printf("Count = %d\n", count);
    }
  }

  @ThreadedTest
  public void runScript() throws Exception {
    final SimpleClass3 testObject = new SimpleClass3();

    final Script<SimpleClass3> main = new Script<SimpleClass3>(testObject);
    final Script<SimpleClass3> second = new Script<SimpleClass3>(main);

    // Create control and target objects to allow us to specify a release point.
    final SimpleClass3 control = main.object();
    final SimpleClass4 target = main.createTarget(SimpleClass4.class);

    // Tell the main script to release to the second script after we've called
    // method1() from within mainMethod().
    main.in(control.mainMethod("")).afterCalling(target.method1()).releaseTo(second);

    main.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ calling mainMethod()\n");
        testObject.mainMethod("Script 1 ");
        System.out.printf("@@@ called mainMethod() - setting release point\n");
        main.atStartOf(control.secondMethod("")).releaseTo(second);
        System.out.printf("@@@ calling secondMethod()\n");
        testObject.secondMethod("Script 1 ");
      }
    });

    second.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ in script 2 - calling mainMethod()\n");
        testObject.mainMethod("Script 2 ");
        System.out.printf("@@@ in script 2 - releasing to main\n");
        releaseTo(main);
        testObject.secondMethod("Script 2 ");
      }
    });

    new Scripter<SimpleClass3>(main, second).execute();

    List<String> sequence = testObject.getSequence();
    assertEquals("Script 1 1", sequence.get(0));
    assertEquals("Script 2 1", sequence.get(1));
    assertEquals("Script 2 2", sequence.get(2));
    assertEquals("Script 2 3", sequence.get(3));
    assertEquals("Script 2 4", sequence.get(4));
    assertEquals("Script 1 2", sequence.get(5));
    assertEquals("Script 1 3", sequence.get(6));
    assertEquals("Script 1 4", sequence.get(7));
    assertEquals("Script 2 5", sequence.get(8));
    assertEquals("Script 2 6", sequence.get(9));
    assertEquals("Script 2 7", sequence.get(10));
    assertEquals("Script 2 8", sequence.get(11));
    assertEquals("Script 1 5", sequence.get(12));
    assertEquals("Script 1 6", sequence.get(13));
    assertEquals("Script 1 7", sequence.get(14));
    assertEquals("Script 1 8", sequence.get(15));
  }


  @ThreadedTest
  public void runScriptWithMethodWithSynchronizedBlock() throws Exception {
    final SimpleClass3 testObject = new SimpleClass3();

    final Script<SimpleClass3> main = new Script<SimpleClass3>(testObject);
    final Script<SimpleClass3> second = new Script<SimpleClass3>(main);

    // Create control and target objects to allow us to specify a release point.
    final SimpleClass3 control = main.object();
    final SimpleClass4 target = main.createTarget(SimpleClass4.class);

    // Tell the main script to release to the second script after we've called
    // method1() from within mainMethodWithSyncBlock().
    main.in(control.mainMethodWithSyncBlock("")).afterCalling(target.method1()).releaseTo(second);

    main.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ calling mainMethodWithSyncBlock()\n");
        testObject.mainMethodWithSyncBlock("Script 1 ");
        System.out.printf("@@@ called mainMethodWithSyncBlock() - setting release point\n");
        main.atStartOf(control.secondMethod("")).releaseTo(second);
        System.out.printf("@@@ calling secondMethod()\n");
        testObject.secondMethod("Script 1 ");
      }
    });

    second.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ in script 2 - calling mainMethodWithSyncBlock()\n");
        testObject.mainMethodWithSyncBlock("Script 2 ");
        System.out.printf("@@@ in script 2 - releasing to main\n");
        releaseTo(main);
        testObject.secondMethod("Script 2 ");
      }
    });

    new Scripter<SimpleClass3>(main, second).execute();

    List<String> sequence = testObject.getSequence();
    for (int i = 0; i < sequence.size(); i++) {
      System.out.printf("    assertEquals(\"%s\", sequence.get(%d));\n", sequence.get(i), i);
    }
    assertEquals("Script 1 1", sequence.get(0));
    assertEquals("Script 2 1", sequence.get(1));
    assertEquals("Script 2 2", sequence.get(2));
    assertEquals("Script 2 3", sequence.get(3));
    assertEquals("Script 2 4", sequence.get(4));
    assertEquals("Script 1 2", sequence.get(5));
    assertEquals("Script 1 3", sequence.get(6));
    assertEquals("Script 1 4", sequence.get(7));
    assertEquals("Script 2 5", sequence.get(8));
    assertEquals("Script 2 6", sequence.get(9));
    assertEquals("Script 2 7", sequence.get(10));
    assertEquals("Script 2 8", sequence.get(11));
    assertEquals("Script 1 5", sequence.get(12));
    assertEquals("Script 1 6", sequence.get(13));
    assertEquals("Script 1 7", sequence.get(14));
    assertEquals("Script 1 8", sequence.get(15));
  }

  @ThreadedTest
  public void runScriptWithSynchronizedMethod() throws Exception {
    final SimpleClass3 testObject = new SimpleClass3();

    final Script<SimpleClass3> main = new Script<SimpleClass3>(testObject);
    final Script<SimpleClass3> second = new Script<SimpleClass3>(main);

    // Create control and target objects to allow us to specify a release point.
    final SimpleClass3 control = main.object();
    final SimpleClass4 target = main.createTarget(SimpleClass4.class);

    // Tell the main script to release to the second script after we've called
    // method1() from within mainMethodSynchronized().
    main.in(control.mainMethodSynchronized("")).afterCalling(target.method1()).releaseTo(second);

    main.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ calling mainMethodSynchronized()\n");
        testObject.mainMethodSynchronized("Script 1 ");
        System.out.printf("@@@ called mainMethodSynchronized() - setting release point\n");
        main.atStartOf(control.secondMethod("")).releaseTo(second);
        System.out.printf("@@@ calling secondMethod()\n");
        testObject.secondMethod("Script 1 ");
      }
    });

    second.addTask(new ScriptedTask<SimpleClass3>() {
      @Override
      public void execute() {
        System.out.printf("@@@ in script 2 - calling mainMethodSynchronized()\n");
        testObject.mainMethodSynchronized("Script 2 ");
        System.out.printf("@@@ in script 2 - releasing to main\n");
        releaseTo(main);
        testObject.secondMethod("Script 2 ");
      }
    });

    new Scripter<SimpleClass3>(main, second).execute();

    List<String> sequence = testObject.getSequence();
    for (int i = 0; i < sequence.size(); i++) {
      System.out.printf("    assertEquals(\"%s\", sequence.get(%d));\n", sequence.get(i), i);
    }
    assertEquals("Script 1 1", sequence.get(0));
    assertEquals("Script 1 2", sequence.get(1));
    assertEquals("Script 1 3", sequence.get(2));
    assertEquals("Script 1 4", sequence.get(3));
    assertEquals("Script 2 1", sequence.get(4));
    assertEquals("Script 2 2", sequence.get(5));
    assertEquals("Script 2 3", sequence.get(6));
    assertEquals("Script 2 4", sequence.get(7));
    assertEquals("Script 2 5", sequence.get(8));
    assertEquals("Script 2 6", sequence.get(9));
    assertEquals("Script 2 7", sequence.get(10));
    assertEquals("Script 2 8", sequence.get(11));
    assertEquals("Script 1 5", sequence.get(12));
    assertEquals("Script 1 6", sequence.get(13));
    assertEquals("Script 1 7", sequence.get(14));
    assertEquals("Script 1 8", sequence.get(15));
  }
}
