/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * Simplistic Database interface used for demonstrating
 * multithreaded tests. See {@link UserManager}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface Database {

  boolean userExists(String username);

  void addUser(String username);
}
