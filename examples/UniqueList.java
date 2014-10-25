/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import java.util.ArrayList;

/**
 * Implementation of {@link java.util.ArrayList} that allows
 * an element to be added only if it is not already in the list.
 * <p>
 * This class is designed to demonstrate the testing of race conditions,
 * and is not intended to be complete or correct.
 *
 * @param <E> the list element
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UniqueList<E> extends ArrayList<E> {

  /**
   * Adds an element iff it is not already present in this list. Returns true
   * if the element was added, and false if it was already found.
   */
  public boolean putIfAbsent(E elem) {
    return putIfAbsentInternal(elem);
  }

  // The actual method that we want to test is a private one. To make this work, we
  // specify the name of this method in the test setup. See
  // UniqueListTest.testPutIfAbsent()
  private boolean putIfAbsentInternal(E elem) {
    boolean absent = !super.contains(elem);
    if (absent) {
      super.add(elem);
    }
    return absent;
  }
}
