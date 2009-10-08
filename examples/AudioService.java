/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * A service for playing media assets on an audio channel. This is a
 * simplified instance of a real class, and is used for illustrating
 * race conditions. See {@link Player}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface AudioService {

  /**
   * Plays the given media asset. When the asset finishes playing,
   * the listener will be notified. Returns a token identifying the
   * audio stream that is playing the given asset.
   */
  int cue(String assetName, AudioListener listener);
}
