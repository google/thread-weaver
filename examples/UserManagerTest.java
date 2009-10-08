/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import com.google.testing.threadtester.InterleavedRunner;
import com.google.testing.threadtester.MainRunnableImpl;
import com.google.testing.threadtester.RunResult;
import com.google.testing.threadtester.SecondaryRunnableImpl;
import com.google.testing.threadtester.utils.BlockingProxy;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit test for UserManager. Demonstrates use of
 * {@link com.google.testing.threadtester.utils.BlockingProxy}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UserManagerTest extends TestCase {

  private static final String USER = "User";

  private class FakeDatabase implements Database {
    private Set<String> users = new HashSet<String>();

    @Override
    public boolean userExists(String username) {
      return users.contains(username);
    }

    @Override
    public void addUser(String username) {
      if (userExists(username)) {
        throw new IllegalArgumentException("User exists");
      }
      users.add(username);
    }
  }

  public void testAddUser() {
    // Create a fake DB, and then create a blocking proxy that will block after
    // we have invoked db.userExists()
    final Database fakeDb = new FakeDatabase();
    final BlockingProxy<Database> dbProxy =
      BlockingProxy.create(Database.class, fakeDb, "userExists", false);

    // Use an InterleavedRunner to interleave the main and second threads.
    // The main thread will break at the proxy's blocking position, and
    // the second thread will run.
    RunResult result = InterleavedRunner.interleaveAtBreakpoint(
        new UserMain(dbProxy.getProxy()), new UserSecond(), dbProxy);
    result.throwExceptionsIfAny();
  }

  /**
   * Runs the main thread of execution.
   */
  private class UserMain extends MainRunnableImpl<UserManager> {
    final Database db;
    UserManager manager;

    UserMain(Database db) {
      this.db = db;
    }

    @Override
    public Class<UserManager> getClassUnderTest() {
      return UserManager.class;
    }

    @Override
    public UserManager getMainObject() {
      return manager;
    }

    @Override
    public void initialize() {
      manager = new UserManager(db);
    }

    /**
     * This is invoked after initialization, to run the main body of the
     * test. Because of the proxy that we have specified, we will break
     * in the middle of playAsset(), and the second thread will be run..
     */
    @Override
    public void run() {
      System.out.printf("Running main thread %s\n", Thread.currentThread());
      manager.addUser(USER);
      System.out.printf("Main thread finished\n");
    }
  }

  /**
   * Runs the second thread of execution.
   */
  private class UserSecond extends
      SecondaryRunnableImpl<UserManager, UserMain> {
    UserManager manager;

    @Override
    public void initialize(UserMain main) {
      manager = main.getMainObject();
    }

    @Override
    public void run() {
      System.out.printf("Running second thread %s\n", Thread.currentThread());
      assertFalse(manager.addUser(USER));
      System.out.printf("Second thread finished\n");
    }
  }
}
