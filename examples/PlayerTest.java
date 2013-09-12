/*
 * Copyright 2009 Weaver authors
 *
 * This code is part of the Weaver tutorial and may be freely used.
 */



import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.testing.threadtester.ClassInstrumentation;
import com.google.testing.threadtester.CodePosition;
import com.google.testing.threadtester.Instrumentation;
import com.google.testing.threadtester.InterleavedRunner;
import com.google.testing.threadtester.MainRunnableImpl;
import com.google.testing.threadtester.MethodRecorder;
import com.google.testing.threadtester.RunResult;
import com.google.testing.threadtester.SecondaryRunnableImpl;
import com.google.testing.threadtester.ThreadedTest;
import com.google.testing.threadtester.ThreadedTestRunner;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Tests {@link Player} using a {@link CodePosition}.
 *
 * NOTE: This test will fail. It was written to demonstrate a fault in the class
 * under test.
 *
 * @author alasdair.mackintosh@gmail.com (Alasdair Mackintosh)
 */
public class PlayerTest extends TestCase {

  private static final String ASSET = "song";
  private static final int TOKEN = 1;

  ThreadedTestRunner runner = new ThreadedTestRunner();

  public void testThreadedTests() {
    runner.runTests(getClass(), Player.class);
  }

  @ThreadedTest
  public void runPlayAsset() throws Exception {
    // Create a CodePosition at the point where we want to break the main
    // thread.
    CodePosition cp = getCodePositionForTest();

    // Use an InterleavedRunner to interleave the main and second threads,
    // breaking at the specified position.
    RunResult result = InterleavedRunner.interleave(new PlayerMain(), new PlayerSecond(),
                                                    Arrays.asList(cp));
    result.throwExceptionsIfAny();
  }

  /**
   * Creates a CodePosition at the point where we want to break the main thread.
   */
  CodePosition getCodePositionForTest() {
    // Create a MethodRecorder for the Player
    MethodRecorder<Player> recorder = new MethodRecorder<Player>(Player.class);

    // Get hold of a dummy Player instance so that we can record method calls.
    Player player = recorder.getControl();

    // Get hold of a dummy AudioService instance so that we can record method calls.
    AudioService service = recorder.createTarget(AudioService.class);

    // Specify a position in "playAsset" after calling "service.play"
    CodePosition cp = recorder.
        in(player.playAsset(ASSET, null)).
        afterCalling(service.cue(ASSET, null)).
        position();
    return cp;
  }

  /**
   * Runs the main thread of execution.
   */
  private class PlayerMain extends MainRunnableImpl<Player> {
    Player player;
    Controller controller = createMock(Controller.class);
    AudioService service = createMock(AudioService.class);

    @Override
    public Class<Player> getClassUnderTest() {
      return Player.class;
    }

    @Override
    public Player getMainObject() {
      return player;
    }

    /**
     * This is invoked at the start, to set up the test.
     */
    @Override
    public void initialize() {
      // Create mock and test objects, and set expectations
      controller = createMock(Controller.class);
      service = createMock(AudioService.class);

      player = new Player(service);
      expect(service.cue(ASSET, player)).andReturn(TOKEN);
      controller.onFinished(TOKEN);
      expectLastCall();
      replay(controller, service);
    }

    /**
     * This is invoked after initialization, to run the main body of the
     * test. Becasue of the CodePosition that we have specified, we will break
     * in the middle of playAsset(), and the second thread will be run.
     */
    @Override
    public void run() {
      System.out.println("Running main thread");
      player.playAsset(ASSET, controller);
      System.out.println("Main thread finished");
    }

    /**
     * This is invoked after both threads have run. We can use it to verify
     * expectations.
     */
    @Override
    public void terminate() {
      verify(controller, service);
    }
  }

  /**
   * Runs the second thread of execution.
   */
  private class PlayerSecond extends
      SecondaryRunnableImpl<Player, PlayerMain> {
    Player player;

    /**
     * This is invoked at the start, after PlayerMain.initialize has been
     * invoked. The Player instance created by PlayerMain is passed in.
     */
    @Override
    public void initialize(PlayerMain main) {
      player = main.getMainObject();
    }

    /**
     * This is invoked after the main thread has paused.
     */
    @Override
    public void run() {
      System.out.printf("Running second thread\n");
      player.onAudioPlayed(TOKEN);
      System.out.printf("Second thread finished\n");
    }
  }

  CodePosition getCodePositionForTestVersion2() throws Exception {
    // An alternative way of declaring CodePositions using Method objects. Unlike
    // MethodRecorder, this does not offer compile-time checking of the method
    // names.
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(Player.class);
    Method playAsset = Player.class.getDeclaredMethod("playAsset", Controller.class, String.class);
    Method play = AudioService.class.getDeclaredMethod("cue", String.class, AudioListener.class);
    CodePosition cp = instr.afterCall(playAsset, play);
    return cp;
  }

  CodePosition getCodePositionForTestVersion3() {
    // Yet another way of declaring CodePositions using method names. This is
    // simple, but may cause problems if the names are ambiguous. The position
    // is defined at the start of the call to the first method named "cue", no
    // matter what class it is invoked upon. Also, if the class under test has
    // multiple overloaded methods named "playAsset", the test framework will
    // throw an exception.
    ClassInstrumentation instr = Instrumentation.getClassInstrumentation(Player.class);
    CodePosition cp = instr.afterCall("playAsset", "cue");
    return cp;
  }

}
