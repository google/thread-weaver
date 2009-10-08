/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.makeThreadSafe;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import com.google.testing.threadtester.Script;
import com.google.testing.threadtester.ScriptedTask;
import com.google.testing.threadtester.Scripter;
import com.google.testing.threadtester.ThreadedTest;
import com.google.testing.threadtester.ThreadedTestRunner;

import junit.framework.TestCase;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tests {@link UserCache} using a {@link Script}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UserCacheTest extends TestCase {
  private static final String USER = "user";
  private static final String AVATAR1 = "A1";
  private static final String AVATAR2 = "A2";
  private UserDb db;
  private RenderingContext context;
  private UserCache cache;

  // Create a cache, and verify that it initialises itself from the database.
  private void createCache() {
    db = createMock(UserDb.class);
    makeThreadSafe(db, true);
    context = createMock(RenderingContext.class);
    makeThreadSafe(context, true);
    expect(db.getAvatar(USER)).andReturn(AVATAR1);
    replay(db, context);
    cache = new UserCache(db, USER, context);
    verify(db, context);
  }

  private void resetMocks() {
    // Note that resetting a mock will clear the thread-safe setting, so we need to
    // mark the mock as threadsafe every time we reset it. Annoying but true...
    reset(db);
    makeThreadSafe(db, true);
    reset(context);
    makeThreadSafe(context, true);
  }

  /** Simple test for non-threaded operation */
  public void testUserCacheBasicOperation() {
    createCache();
    // Verify a normal draw operation
    resetMocks();
    context.draw(AVATAR1);
    replay(db, context);

    cache.drawUserAvatar();
    verify(db, context);

    // Update the user, and verify that the cache is updated.
    resetMocks();
    db.update(USER);
    expect(db.getAvatar(USER)).andReturn(AVATAR2);
    context.draw(AVATAR2);
    replay(db, context);

    cache.updateUser();
    cache.drawUserAvatar();
    verify(db, context);
  }


  /** Runner for the threaded tests */
  public void testThreadedTests() {
    ThreadedTestRunner runner = new ThreadedTestRunner();
    runner.setDebug(false);  // Set this to true for debugging the scripting
    runner.runTests(getClass(), UserCache.class);
  }

  /**
   * Multithreaded test demonstrating the use of scripts.
   */
  @ThreadedTest
  public void runUserCacheMultiThreaded() throws Exception {
    createCache();

    // Update the user, which will mark the cache as invalid. This
    // means that when we call cache.drawUserAvatar() below, it will
    // refresh the cache.
    resetMocks();
    db.update(USER);
    expect(db.getAvatar(USER)).andReturn(AVATAR2);
    context.draw(AVATAR2);
    replay(db, context);
    cache.updateUser();

    // Create two scripts. The main script will call
    // cache.drawUserAvatar(). It will yeild control to the second
    // script at two different pooints, and the second script will
    // then verify the state of the locks.
    final Script<UserCache> main = new Script<UserCache>(cache);
    final Script<UserCache> second = new Script<UserCache>(main);

    // Create control and target objects to allow us to specify a release point.
    final UserCache control = main.object();
    final UserDb dbTarget = main.createTarget(UserDb.class);
    final RenderingContext contextTarget = main.createTarget(RenderingContext.class);
    final ReadWriteLock lockTarget = main.createTarget(ReadWriteLock.class);

    // Some of the methods that we are invoking are void, which means that we cannot use the
    // main.in(xx).beforeCallng(yy) syntax. Instead we need to invoke a method on the control
    // object, and then use inLastMethod() and beforeCalling(). This is analagous to
    // EasyMock.expectLastcall()
    //
    // In this case we want to release control twice. The first time will be just before we
    // call getAvatar(), and the second will be just before we callcontext.draw().
    control.updateCache();
    main.inLastMethod().beforeCalling(dbTarget.getAvatar("")).releaseTo(second);

    control.drawUserAvatar();
    main.inLastMethod();
    contextTarget.draw("");
    main.beforeCallingLastMethod().releaseTo(second);

    main.addTask(new ScriptedTask<UserCache>() {
      @Override
      public void execute() {
        cache.drawUserAvatar();
      }
    });

    second.addTask(new ScriptedTask<UserCache>() {
      @Override
      public void execute() {
        // The second task verifies the locks at the point where it is released, and then
        // returns control to the first script.
        ReentrantReadWriteLock lock = cache.rwl;

        // This is the first point where the main script releases control to the second
        // script. We print some diagnostics, and release control back to the main.
        System.out.printf("First release - write = %s, num readers = %d\n",
            lock.isWriteLocked(), lock.getReadLockCount());
        releaseTo(main);

        // This is the second point where the main script releases control to the second
        // script. Again, print diagnostics, and release control back to the main.
        System.out.printf("Second release - write = %s, num readers = %d\n",
            lock.isWriteLocked(), lock.getReadLockCount());
        releaseTo(main);
      }
    });

    // Run the two scripts defined above.
    new Scripter<UserCache>(main, second).execute();
  }
}
