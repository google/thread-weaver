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

/**
 * Unit test for UniqueList. Demonstrates use of
 * {@link com.google.testing.threadtester.AnnotatedTestRunner}.
 *
 * NOTE: This test will fail. It was written to demonstrate a fault in the class
 * under test.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UniqueListTest extends TestCase {

  private static final String HELLO = "Hello";

  private volatile UniqueList<String> uniqueList;

  public void testPutIfAbsent() {
    System.out.printf("In testPutIfAbsent\n");
    // Create an AnnotatedTestRunner that will run the threaded tests defined in
    // this class. These tests are expected to makes calls to UniqueList.
    AnnotatedTestRunner runner = new AnnotatedTestRunner();
    runner.runTests(this.getClass(), UniqueList.class);
  }

  @ThreadedBefore
  public void before() {
    // Set up a new UniqueList instance for the test
    uniqueList = new UniqueList<String>();
    System.out.printf("Created new list\n");
  }

  @ThreadedMain
  public void main() {
    // Add a new element to the list in the main test thread
    uniqueList.putIfAbsent(HELLO);
  }

  @ThreadedSecondary
  public void secondary() {
    // Add a new element to the list in the secondary test thread
    uniqueList.putIfAbsent(HELLO);
  }

  @ThreadedAfter
  public void after() {
    // If UniqueList is behaving correctly, it should only contain
    // a single copy of HELLO
    assertEquals(1, uniqueList.size());
    assertTrue(uniqueList.contains(HELLO));
  }
}
