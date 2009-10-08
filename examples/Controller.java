/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * A top-level object that plays entries an a playlist. Uses a {@link Player} to
 * play media assets.
 * <p>
 * This is a simplified version of a real class, and is used to illustrate potential
 * race conditions. See {@link Player}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface Controller {

  /** Invoked when a media asset has finished playing. */
  void onFinished(int token);
}
