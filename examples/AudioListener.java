/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



/**
 * Responds to changes in an audio stream. An instance of this is passed in to
 * {@link AudioService#cue}.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public interface AudioListener {

  /**
   * Invoked when the audio stream represented by the given token has finished
   * playing.
   */
  void onAudioPlayed(int audioToken);
}
