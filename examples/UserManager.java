/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * Adds users to a database.
 * <p>
 * This class is designed to demonstrate the testing of race conditions,
 * and is not intended to be complete or correct.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UserManager {
  private Database db;

  public UserManager(Database db) {
    this.db = db;
  }

  /**
   * Adds a new user. Returns true if the user was added, and false
   * if the user already exists.
   */
  public boolean addUser(String username) {
    boolean exists = db.userExists(username);
    if (!exists) {
      db.addUser(username);
    }
    return !exists;
  }
}
