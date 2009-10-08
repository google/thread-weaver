/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import java.util.HashMap;
import java.util.Map;

/**
 * Provides a layer between a {@link Controller} that plays audio, and the
 * underlying {@link AudioService}. Maintains a mapping between the
 * audio tokens used by the AudioService and the corresponding controller
 * object.
 * <p>
 * This is a simplified version of a real class, and is used to
 * illustrate potential race conditions.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class Player implements AudioListener {

  /** Maps an audio token onto the corresponding Controller */
  private Map<Integer, Controller> controllerMap = new HashMap<Integer,Controller>();

  private AudioService service;

  public Player(AudioService service) {
    this.service = service;
  }

  /**
   * Plays the given media asset. When the asset has finished playing,
   * invokes {@link Controller#onFinished}.
   */
  public int playAsset(String assetName, Controller controller) {
    validateAsset(assetName);

    // Start playing the asset, and get the token that identifies the audio stream.
    int token = service.cue(assetName, this);

    // Add a mapping from token back to the controller that owns it.
    controllerMap.put(token, controller);
    return token;
  }

  /**
   * Invoked by the audio service when the asset has finished
   * playing. Calls the controller's {@link Controller#onFinished} method.
   */
  @Override
  public void onAudioPlayed(int audioToken) {
    Controller controller = controllerMap.get(audioToken);
    if (controller == null) {
      throw new IllegalStateException("No controller for token " + audioToken);
    }
    controller.onFinished(audioToken);
  }

  private void validateAsset(String assetName) {
    if (assetName == null) {
      throw new NullPointerException();
    }
  }
}
