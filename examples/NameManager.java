/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import java.util.ArrayList;
import java.util.List;

/**
 * Simple class that manages a list of names. This is intended to be
 * threadsafe, but has a bug. Please write a unit test that
 * demonstrates the bug.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */

public class NameManager {

  private List<String> names = new ArrayList<String>();

  public void setNames(List<String> newNames) {
    synchronized (names) {
      names = new ArrayList<String>();
      for (String name : newNames) {
        names.add(name);
      }
    }
  }

  public List<String> getNames() {
    return new ArrayList<String>(names);
  }
}
