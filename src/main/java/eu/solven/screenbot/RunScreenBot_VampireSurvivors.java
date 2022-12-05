package eu.solven.screenbot;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.AtomicLongMap;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

/**
 * This is the entrypoint for VampireSurvivor bot. You should start the game, put it full screen, start a game, pause,
 * start the bot (this class), get back in the game, and resume.
 * 
 * @author Benoit Lacelle
 *
 */
// Add 'Accessibility', 'Developper Tools' and 'Screen Recording' to Eclipse, and start Eclipse from Finder (not the
// Toolbar)
public class RunScreenBot_VampireSurvivors {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunScreenBot_VampireSurvivors.class);

	// final static Map<Integer, Set<Integer>> keyToRobotDirections = new HashMap<>();

	// This colors are considered as irrelevant to play the game. They are typically very present when no monster is
	// present in the screen.
	// final public static Set<Integer> neutralColors = new HashSet<>();
	final public static AtomicDouble neutralDistance = new AtomicDouble();
	final public static AtomicLongMap<Hash> neutralHashes = AtomicLongMap.create();

	// static {
	// keyToRobotDirections.put(KeyEvent.VK_UP, Set.of(7, 8, 9));
	// keyToRobotDirections.put(KeyEvent.VK_DOWN, Set.of(1, 2, 3));
	// keyToRobotDirections.put(KeyEvent.VK_LEFT, Set.of(1, 4, 7));
	// keyToRobotDirections.put(KeyEvent.VK_RIGHT, Set.of(3, 6, 9));
	// }

	public static void reset() {
		VampireSurvivorExpertise.maxXpRatio.set(0);
		// neutralColors.clear();
		neutralHashes.clear();
	}

	public static void main(String[] args) throws Exception {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		f.setSize(0, 0);

		AtomicInteger directionFromHuman = new AtomicInteger(5);

		trackHumanActions(f, directionFromHuman);

		Robot robot = new Robot();

		AtomicInteger directionFromRobot = new AtomicInteger(5);

		initNeutralColors();

		AtomicInteger previousXp = new AtomicInteger();

		boolean doPlay = true;
		boolean doSaveCapture = true;

		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
			try {
				BufferedImage capture =
						robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

				capture = RobotWithEye.resize(capture, 640, 480);

				int percentXp = VampireSurvivorExpertise.computeXp(capture);
				if (previousXp.get() != percentXp) {
					previousXp.set(percentXp);
					LOGGER.info("New XP: {}", percentXp);
				}

				if (doSaveCapture) {
					doSaveCapture(directionFromHuman, directionFromRobot, capture);
				}
			} catch (Throwable t) {
				LOGGER.error("ARG", t);
			}
		}, 1, 5000, TimeUnit.MILLISECONDS);

		AtomicLong previousMenuPercent = new AtomicLong();

		// Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
		// // Cycle from 1 to 9
		// executeChangeOfDirection(robot, directionFromRobot, 1 + directionFromRobot.get() % 9);
		// }, 2, 2, TimeUnit.SECONDS);

		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
			try {

				BufferedImage capture =
						robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

				capture = RobotWithEye.resize(capture, 640, 480);

				if (VampireSurvivorExpertise.isOtherApp(capture)) {
					return;
				}

				List<String> colors = VampireSurvivorExpertise.menuColors;
				long percentWithMenu = RobotWithEye.percentForColors(colors, capture);
				if (previousMenuPercent.get() != percentWithMenu) {
					previousMenuPercent.set(percentWithMenu);
					LOGGER.info("menu= {}%", percentWithMenu);
				}

				if (VampireSurvivorExpertise.isMenu(capture)) {
					return;
				}

				int directionFromScreenshot = computeDirectionFromRobot(capture);
				if (directionFromRobot.get() != directionFromScreenshot) {
					LOGGER.info("Capture suggests we should go to {}", directionFromRobot);

					if (doPlay) {
						directionFromRobot.set(directionFromScreenshot);
					}
				}

				if (false) {
					doSaveCapture(directionFromHuman, directionFromRobot, capture);
				}
			} catch (Throwable t) {
				LOGGER.error("ARG", t);
			}
		}, 1, 1, TimeUnit.MILLISECONDS);
	}

	private static void doSaveCapture(AtomicInteger directionFromHuman,
			AtomicInteger directionFromRobot,
			BufferedImage capture) {
		boolean otherApp = VampireSurvivorExpertise.isOtherApp(capture);
		boolean menu = VampireSurvivorExpertise.isMenu(capture);
		boolean suffering = VampireSurvivorExpertise.isSuffering(capture);

		int directionFromHumanAsRobotLayout = HumanTranscoding.convertFromHumanToRobot(directionFromHuman.get());

		String filename = "screenshot" + "-"
				+ OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY-HHmmss"))
				+ "-human="
				+ directionFromHumanAsRobotLayout
				+ "-robot="
				+ directionFromRobot.get()
				+ "-otherApp="
				+ otherApp
				+ "-menu="
				+ menu
				+ "-suffering="
				+ suffering
				+ ".png";

		LOGGER.info("Saving {}", filename);
		try {
			ImageIO.write(capture, "PNG", new File(filename));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void initNeutralColors() {
		try {
			String r = "/go2/easy-monsters=2.png";
			BufferedImage capture = ImageIO.read(new ClassPathResource(r).getURL());

			// AtomicLongMap<Integer> sampleToCount = RobotWithEye.groupByColor(capture);
			// long nbPixels = capture.getWidth() * capture.getHeight();
			// while (true) {
			// Optional<Entry<Integer, Long>> mostPresent =
			// sampleToCount.asMap().entrySet().stream().max(Comparator.comparing(e -> e.getValue()));
			//
			// if (mostPresent.isPresent() && mostPresent.get().getValue() > nbPixels * 0.1D) {
			// int rgb = mostPresent.get().getKey();
			// if (neutralColors.add(rgb)) {
			// LOGGER.info("We registered {} as irrelevant ({})", HumanTranscoding.toHex(rgb), r);
			// }
			//
			// sampleToCount.remove(rgb);
			// } else {
			// break;
			// }
			// }

			{
				HashingAlgorithm hasher = new PerceptiveHash(32);

				// We consider our eye has a 10_000 pixel resolution
				// The strategy is then to detect what's considered background/neutral pixels/blocks
				int blockWidth = RobotWithEye.getBlockWidth(capture);
				int blockHeight = RobotWithEye.getBLockHeight(capture);

				double limitDistance = 0.5D;
				// We start considering equivalent hashes are exact-match
				double minLimitDistance = 0D;
				double maxLimitDistance = 1D;

				int[] iArray =
						IntStream.iterate(blockWidth, i -> i < capture.getWidth() - blockWidth, i -> i + blockWidth / 2)
								.toArray();
				int[] jArray = IntStream
						.iterate(blockHeight, j -> j < capture.getHeight() - blockHeight, j -> j + blockHeight / 2)
						.toArray();

				// We iterate randomly in order to pick a random first-hash (else, our first hash may always be the
				// top-left corner which is biaised)
				Random random = new Random(123);
				Statistics.shuffleArray(random, iArray);
				Statistics.shuffleArray(random, jArray);

				AtomicLongMap<Hash> localNeutralHashes = AtomicLongMap.create();
				while (true) {
					// We divide increment by 2 in order to have overlapping blocks
					for (int j : jArray) {
						for (int i : iArray) {
							BufferedImage block = capture.getSubimage(i, j, blockWidth, blockHeight);
							Hash currentBlockHash = hasher.hash(block);

							Optional<Entry<Hash, Long>> minimizingHash = localNeutralHashes.asMap()
									.entrySet()
									.stream()
									.min(Comparator
											.comparing(h -> h.getKey().normalizedHammingDistance(currentBlockHash)));

							if (minimizingHash.isPresent() && minimizingHash.get()
									.getKey()
									.normalizedHammingDistance(currentBlockHash) < limitDistance) {
								// We give strength to this already similar block
								localNeutralHashes.incrementAndGet(minimizingHash.get().getKey());
							} else {
								LOGGER.debug("We added a neutral hash for {} {}", i, j);
								localNeutralHashes.incrementAndGet(currentBlockHash);
							}
						}
					}

					long sum = iArray.length * jArray.length;
					AtomicLongMap<Hash> localNeutralTopHashes = AtomicLongMap.create();
					localNeutralHashes.asMap()
							.entrySet()
							.stream()
							.sorted(Comparator.comparing(e -> e.getValue()))
							.forEach(e -> {
								// We accept neutral hashes until 2/3rd our sight is considered neutral
								if (localNeutralTopHashes.sum() < 0.66D * sum) {
									localNeutralTopHashes.addAndGet(e.getKey(), e.getValue());
								} else {
									LOGGER.info("We spotted when covering {}%",
											((int) localNeutralTopHashes.sum() * 100 / sum));
								}
							});

					if (localNeutralTopHashes.size() >= sum * 0.01D) {
						// We have too many different hashes: let's consider has can be slightly more similar the one to
						// the others
						limitDistance += (maxLimitDistance - limitDistance) / 3D;
						LOGGER.info("We retry with limitDistance={}", limitDistance);
					} else if (localNeutralTopHashes.size() < sum * 0.01D * 0.001D) {
						// We have too few different hashes: let's consider has can be slightly less similar the one to
						// the others
						limitDistance -= (limitDistance - minLimitDistance) / 3D;
						LOGGER.info("We retry with limitDistance={}", limitDistance);
					} else {
						// We have a good balance between block-similarity and number of pixels considered neutral
						neutralHashes.putAll(localNeutralTopHashes.asMap());
						neutralDistance.set(limitDistance);
						break;
					}

					// We consider this image is absolute neutral
					// break;
				}
				LOGGER.info("We consider {} neutral hashes", neutralHashes.size());
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static int computeDirectionFromRobot(BufferedImage capture) {
		int widthCenter = capture.getWidth() / 2;
		int heightCenter = capture.getHeight() / 2;

		HashingAlgorithm hasher = new PerceptiveHash(32);

		// This is biaised for VampireSurvivor, as we input the knowledge the center is important
		// Skip d=0 as it would not indicate a direction (but it may indicate we have to move in any direction)
		for (int d = 1; d < Math.max(widthCenter, heightCenter); d += 1 + d * 0.1D) {
			double iTotalFlee = 0D;
			double jTotalFlee = 0D;

			// Angle increment should increase with d
			int fleePixel = 32;
			double angleIncrement = 2D * Math.PI / fleePixel;
			for (int angle = 0; angle < fleePixel; angle += 1) {
				double iFlee = Math.cos(angle * angleIncrement);
				int i = (int) (widthCenter + d * iFlee);
				double jFlee = Math.sin(angle * angleIncrement);
				int j = (int) (heightCenter + d * jFlee);

				int blockWidth = RobotWithEye.getBlockWidth(capture);
				int blockHeight = RobotWithEye.getBLockHeight(capture);
				// int blockWidth = 1 + d / 2;
				if (i < 0 || j < 0 || i + blockWidth >= capture.getWidth() || j + blockHeight >= capture.getHeight()) {
					// This happens typically when the capture is not square
					continue;
				}

				BufferedImage block = capture.getSubimage(i, j, blockWidth, blockHeight);
				Hash currentBLockHash = hasher.hash(block);
				if (neutralHashes.asMap()
						.entrySet()
						.stream()
						.filter(e -> e.getKey().normalizedHammingDistance(currentBLockHash) < neutralDistance.get())
						.findAny()
						.isEmpty()) {
					// This is not a neutral block: flee
					iTotalFlee += iFlee;
					jTotalFlee += jFlee;
				}
			}

			if (Math.pow(iTotalFlee, 2D) + Math.pow(jTotalFlee, 2D) >= 1D) {
				// The flee signal is clear enough
				// printFlee

				// https://stackoverflow.com/questions/14079127/getting-angle-back-from-a-sin-cos-conversion
				// We multiple by -1 as we want to flee this direction
				double angle = Math.atan2(-jTotalFlee, -iTotalFlee);

				if (Math.abs(angle) >= Math.PI * 7D / 8D) {
					// left
					return 4;
				} else if (Math.abs(angle) <= Math.PI * 1D / 8D) {
					// right
					return 6;
				} else if (angle > 0D) {
					if (angle <= Math.PI * 3D / 8D) {
						// up-right
						return 9;
					} else if (angle <= Math.PI * 5D / 8D) {
						// up
						return 8;
					} else {
						// up-left
						return 7;
					}
				} else if (angle < 0D) {
					double nAngle = -angle;
					if (nAngle <= Math.PI * 3D / 8D) {
						// down-right
						return 3;
					} else if (nAngle <= Math.PI * 5D / 8D) {
						// down
						return 2;
					} else {
						// down-left
						return 1;
					}
				}
			}
		}

		return 5;
	}

	private static void updateRobotDirection(BufferedImage screenShot, AtomicInteger directionFromRobot) {
		int directionFromScreenshot = computeDirectionFromRobot(screenShot);
		if (directionFromRobot.get() != directionFromScreenshot) {
			LOGGER.info("Capture suggests we should go to {}", directionFromRobot);
			directionFromRobot.set(directionFromScreenshot);
		}
	}

	private static void trackHumanActions(JFrame f, AtomicInteger directionFromHuman) {
		f.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// This assume QUERTY layout
				if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
					LOGGER.info("Human press {}", "left");
					directionFromHuman.set(directionFromHuman.get() | 1);
				} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
					LOGGER.info("Human press {}", "up");
					directionFromHuman.set(directionFromHuman.get() | 2);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
					LOGGER.info("Human press {}", "right");
					directionFromHuman.set(directionFromHuman.get() | 4);
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
					LOGGER.info("Human press {}", "down");
					directionFromHuman.set(directionFromHuman.get() | 8);
				} else {
					LOGGER.info("Human press code={}, char={}", e.getKeyCode(), e.getKeyChar());
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// This assume QUERTY layout
				if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
					LOGGER.info("Human release {}", "left");
					// https://stackoverflow.com/questions/3920307/how-can-i-remove-a-flag-in-c
					directionFromHuman.set(directionFromHuman.get() & ~1);
				} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
					LOGGER.info("Human release {}", "up");
					directionFromHuman.set(directionFromHuman.get() & ~2);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
					LOGGER.info("Human release {}", "right");
					directionFromHuman.set(directionFromHuman.get() & ~4);
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
					LOGGER.info("Human release {}", "down");
					directionFromHuman.set(directionFromHuman.get() & ~8);
				} else {
					LOGGER.info("Human release code={}, char={}", e.getKeyCode(), e.getKeyChar());
				}
			}
		});
	}
}