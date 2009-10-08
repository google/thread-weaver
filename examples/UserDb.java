/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * Stores information about a user. See {@link UserCache}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface UserDb {

  String getAvatar(String username);

  void update(String username);
}
