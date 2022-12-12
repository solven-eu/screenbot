package eu.solven.screenbot;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RobotWithMoves {
	private static final Logger LOGGER = LoggerFactory.getLogger(RobotWithMoves.class);

	final static Map<Integer, Set<Integer>> keyToRobotDirections = new HashMap<>();

	static {
		keyToRobotDirections.put(KeyEvent.VK_UP, Set.of(7, 8, 9));
		keyToRobotDirections.put(KeyEvent.VK_DOWN, Set.of(1, 2, 3));
		keyToRobotDirections.put(KeyEvent.VK_LEFT, Set.of(1, 4, 7));
		keyToRobotDirections.put(KeyEvent.VK_RIGHT, Set.of(3, 6, 9));
	}

	public static void executeChangeOfDirection(Robot robot, AtomicInteger direction, int newDirection) {
		int oldDirection = direction.getAndSet(newDirection);
		if (oldDirection == newDirection) {
			// Same direction
			return;
		}
		LOGGER.info("Change direction from " + oldDirection + " to " + newDirection);

		keyToRobotDirections
				.forEach((key, directions) -> pressOrReleaseIf(robot, newDirection, oldDirection, directions, key));
	}

	public static void pressOrReleaseIf(Robot robot, int newDirection, int oldDirection, Set<Integer> ups, int key) {
		if (ups.contains(oldDirection)) {
			if (ups.contains(newDirection)) {
				// still up
			} else {
				// Not up anymore
				robot.keyRelease(key);
				System.out.println("Release " + key);
			}
		} else if (ups.contains(newDirection)) {
			// now up
			robot.keyPress(key);
			System.out.println("Press " + key);
		}
	}
}
