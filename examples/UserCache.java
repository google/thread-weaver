/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Caches an avatar corresponding to a given user. This is a test class,
 * used to illustrate issues with read-write locks. See class example for
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 *
 * NOTE: This test will fail. It was written to demonstrate a fault in the class
 * under test.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class UserCache {
  private final UserDb db;
  private final String username;

  private String avatar;
  private RenderingContext context;
  volatile boolean cacheValid;

  ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

  UserCache(UserDb db, String username, RenderingContext context) {
    this.db = db;
    this.username = username;
    this.context = context;
    updateCache();
    cacheValid = true;
  }

  void updateCache() {
    avatar = db.getAvatar(username);
  }

  public void drawUserAvatar() {
    rwl.readLock().lock();
    if (!cacheValid) {
       // upgrade lock manually
       rwl.readLock().unlock();   // must unlock first to obtain writelock
       rwl.writeLock().lock();
       if (!cacheValid) { // recheck
         updateCache();
         cacheValid = true;
       }
       // downgrade lock
       rwl.readLock().lock();  // reacquire read without giving up write lock
       rwl.writeLock().unlock(); // unlock write, still hold read
    }
    context.draw(avatar);
    rwl.readLock().unlock();
  }

  /** Invoked when the user changes. Marks the cache as dirty. */
  public void updateUser() {
    db.update(username);
    cacheValid = false;
  }
}
