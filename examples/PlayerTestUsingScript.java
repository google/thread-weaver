/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import com.google.testing.threadtester.Script;
import com.google.testing.threadtester.ScriptedTask;
import com.google.testing.threadtester.Scripter;
import com.google.testing.threadtester.ThreadedTest;
import com.google.testing.threadtester.ThreadedTestRunner;

import junit.framework.TestCase;

/**
 * Tests {@link Player} using a {@link Script}. This performs the same
 * tests as {@link PlayerTest}, but was written to illustrate the use
 * of Scripts.
 *
 * NOTE: This test will fail. It was written to demonstrate a fault in the class
 * under test.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class PlayerTestUsingScript extends TestCase {

  private static final String ASSET = "song";
  private static final int TOKEN = 1;

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.runTests(getClass(), Player.class);
  }

  @ThreadedTest
  public void runPlayAsset() throws Exception {
    // Create mock and test objects, and set expectations
    final Controller controller = createMock(Controller.class);
    final AudioService service = createMock(AudioService.class);

    final Player player = new Player(service);

    expect(service.cue(ASSET, player)).andReturn(TOKEN);
    replay(controller, service);

    // Create two scripts. The main script will call Player.playAsset(), and the
    // second will call Player.onAudioPlayed()
    final Script<Player> main = new Script<Player>(player);
    final Script<Player> second = new Script<Player>(main);

    // Create control and target objects to allow us to specify a release point.
    final Player control = main.object();
    final AudioService target = main.createTarget(AudioService.class);

    main.addTask(new ScriptedTask<Player>() {
      @Override
      public void execute() {
        // Tell the main script to release to the second script after we've called
        // AudioService.play() from within Plyer.playAsset()
        main.in(control.playAsset(ASSET, controller))
            .afterCalling(target.cue(ASSET, player))
            .releaseTo(second);
        player.playAsset(ASSET, controller);
      }
    });

    second.addTask(new ScriptedTask<Player>() {
      @Override
      public void execute() {
        player.onAudioPlayed(TOKEN);
      }
    });

    new Scripter<Player>(main, second).execute();
  }
}
