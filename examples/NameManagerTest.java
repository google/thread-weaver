/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */

import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Unit test for NameManager. Demonstrates use of
 * {@link com.google.testing.threadtester.AnnotatedTestRunner}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class NameManagerTest extends TestCase {

  private static final String HELLO = "Hello";

  private volatile NameManager nameManager;

  public void testPutIfAbsent() {
    // Create an AnnotatedTestRunner that will run the threaded tests defined in
    // this class. These tests are expected to makes calls to NameManager.
    AnnotatedTestRunner runner = new AnnotatedTestRunner();
    runner.runTests(this.getClass(), NameManager.class);
  }

  @ThreadedBefore
  public void before() {
    // Set up a new NameManager instance for the test
    nameManager = new NameManager();
  }

  @ThreadedMain
  public void main() {
    // Add a new element to the list in the main test thread
    nameManager.setNames(Arrays.asList("a", "b", "c"));
  }

  @ThreadedSecondary
  public void secondary() {
    // Add a new element to the list in the secondary test thread
    nameManager.setNames(Arrays.asList("a", "b", "c"));
  }

  @ThreadedAfter
  public void after() {
    List<String> names = nameManager.getNames();
    assertEquals(3, names.size());
    assertEquals("a", names.get(0));
    assertEquals("b", names.get(1));
    assertEquals("c", names.get(2));
  }
}
