package eu.solven.screenbot;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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

	// This colors are considered as irrelevant to play the game. They are typically very present when no monster is
	// present in the screen.
	// final public static Set<Integer> neutralColors = new HashSet<>();
	final public AtomicDouble neutralDistance = new AtomicDouble();
	final public AtomicLongMap<Hash> neutralHashes = AtomicLongMap.create();

	public void reset() {
		VampireSurvivorExpertise.maxXpRatio.set(0);
		// neutralColors.clear();
		neutralHashes.clear();
	}

	final JPanel panel = new JPanel();

	final JTextField robotStatus = new JTextField();

	final Robot robot;

	public final AtomicInteger widthBeforeCrop = new AtomicInteger(0);
	public final AtomicInteger heightBeforeCrop = new AtomicInteger(0);
	final ImageIcon recordedAndEnriched = new ImageIcon();

	// BufferedImage consider (0,0) as upper-left, X as width and Y as height
	public final AtomicInteger recordedBottom = new AtomicInteger();
	public final AtomicInteger recordedLeft = new AtomicInteger();
	public final AtomicInteger recordedTop = new AtomicInteger();
	public final AtomicInteger recordedRight = new AtomicInteger();

	// bit-1 is left
	// bit-2 bit is up
	// bit-4 bit is right
	// bit-8 bit is bottom
	final AtomicInteger directionFromHuman = new AtomicInteger(5);
	// 5 is no-op
	// 4 is left
	// 8 is up
	// 6 is right
	// 2 is bottom
	final AtomicInteger directionFromRobot = new AtomicInteger(5);

	public RunScreenBot_VampireSurvivors() throws AWTException {
		this.robot = new Robot();
	}

	public static void main(String[] args) throws Exception {

		RunScreenBot_VampireSurvivors robot = new RunScreenBot_VampireSurvivors();

		JFrame f = new JFrame();

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
		f.setSize(500, 800);

		// https://stackoverflow.com/questions/1626735/how-can-i-display-a-bufferedimage-in-a-jframe
		// f.getContentPane().setLayout(new FlowLayout());

		// https://stackoverflow.com/questions/11357720/java-vertical-alignment-within-jpanel
		// robot.panel.setLayout(new GridBagLayout());
		// GridBagConstraints gbc = new GridBagConstraints();
		robot.panel.setLayout(new BoxLayout(robot.panel, BoxLayout.Y_AXIS));

		// https://stackoverflow.com/questions/31245320/how-to-add-a-button-to-a-jframe-gui
		f.add(robot.panel);

		{
			robot.robotStatus.setEditable(false);
			robot.panel.add(robot.robotStatus);
		}

		// We assume not full-screen, as the robot console will be visible
		CountDownLatch cdlRecordGamePosition = new CountDownLatch(1);

		{
			JLabel imageHolder = new JLabel(robot.recordedAndEnriched);
			imageHolder.setSize(400, 300);
			robot.panel.add(imageHolder//
			);

			AtomicReference<MouseEvent> startDrag = new AtomicReference<>();

			imageHolder.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					startDrag.set(e);
				}

				@Override
				public void mouseReleased(MouseEvent end) {
					MouseEvent start = startDrag.getAndSet(null);
					if (start == null) {
						return;
					}

					int height = robot.heightBeforeCrop.get();

					int imgTop = Math.min(start.getY(), end.getY());
					int newTop = height * imgTop / robot.recordedAndEnriched.getIconHeight();
					robot.recordedTop.set(newTop);

					int imgBottom = Math.max(start.getY(), end.getY());
					int newBottom = height * imgBottom / robot.recordedAndEnriched.getIconHeight();
					robot.recordedBottom.set(newBottom);

					if (newTop == newBottom) {
						LOGGER.warn("new height is 0");
						return;
					}

					int width = robot.widthBeforeCrop.get();
					int imgLeft = Math.min(start.getX(), end.getX());
					int newLeft = width * imgLeft / robot.recordedAndEnriched.getIconWidth();
					robot.recordedLeft.set(newLeft);

					int imgRight = Math.max(start.getX(), end.getX());
					int newRight = width * imgRight / robot.recordedAndEnriched.getIconWidth();
					robot.recordedRight.set(newRight);

					if (newLeft == newRight) {
						LOGGER.warn("new width is 0");
						return;
					}

					robot.captureNowThenCropThenShow();
					cdlRecordGamePosition.countDown();
				}
			});

		}
		// f.pack();

		{
			// By default, we are fullscreen
			resetCrop(robot);
		}

		{
			AtomicInteger nbTry = new AtomicInteger();
			JButton resetGameLocation = new JButton();
			robot.panel.add(resetGameLocation
			// , gbc
			);
			resetGameLocation.setText("Reset Game Location");
			resetGameLocation.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					BufferedImage capture = robot.captureNowThenCropThenShow();

					boolean updated = updateGameLocation(robot, nbTry, capture);
					if (updated) {
						cdlRecordGamePosition.countDown();
					}
					robot.cropThenShow(capture);
				}

			});
		}

		{
			JButton saveCrop = new JButton();
			robot.panel.add(saveCrop
			// , gbc
			);
			saveCrop.setText("Save cropped now!");
			saveCrop.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					BufferedImage capture = robot.captureNowThenCropThenShow();

					robot.doSaveCapture(capture);
				}
			});
		}

		{
			JButton zoomOut = new JButton();
			robot.panel.add(zoomOut
			// , gbc
			);
			zoomOut.setText("Reset Crop");
			zoomOut.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					resetCrop(robot);
				}
			});
		}

		AtomicBoolean doPlay = new AtomicBoolean();
		{
			JButton doPlayButton = new JButton();
			robot.panel.add(doPlayButton);
			doPlayButton.setText("Start playing!");
			doPlayButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (doPlay.compareAndSet(false, true)) {
						doPlayButton.setText("Start playing!");
					} else {
						doPlayButton.setText("Stop playing!!!");
					}
				}
			});
		}

		AtomicInteger frameIndex = new AtomicInteger();
		final JTextField frameStatus = new JTextField();
		{
			frameStatus.setEditable(false);
			robot.panel.add(frameStatus);
		}

		// Save screenshot before waiting
		{
			boolean doSaveCapture = true;

			Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
				try {
					BufferedImage capture = robot.captureNowThenCropThenShow();
					if (doSaveCapture) {
						robot.doSaveCapture(capture);
					}
				} catch (Throwable t) {
					LOGGER.error("ARG", t);
				}
			}, 1, 5000, TimeUnit.MILLISECONDS);
		}

		robot.robotStatus.setText("Please reset the game location!");
		cdlRecordGamePosition.await();

		// trackHumanActions(f, robot.directionFromHuman);

		robot.initNeutralColors();

		AtomicInteger previousXp = new AtomicInteger();

		AtomicLong previousMenuPercent = new AtomicLong();

		// Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
		// // Cycle from 1 to 9
		// executeChangeOfDirection(robot, directionFromRobot, 1 + directionFromRobot.get() % 9);
		// }, 2, 2, TimeUnit.SECONDS);

		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
			int frameNowIndex = frameIndex.incrementAndGet();
			frameStatus.setText("#" + frameNowIndex);

			try {
				BufferedImage capture = robot.captureNowThenCropThenShow();

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

				int percentXp = VampireSurvivorExpertise.computeXp(capture);
				if (previousXp.get() != percentXp) {
					previousXp.set(percentXp);
					LOGGER.info("New XP: {}", percentXp);
				}

				int directionFromScreenshot = robot.computeDirectionFromRobot(capture);
				if (robot.directionFromRobot.get() != directionFromScreenshot) {
					LOGGER.info("Capture suggests we should go to {}", robot.directionFromRobot);
					robot.robotStatus.setText("I would go: " + robot.directionFromRobot);

					if (doPlay.get()) {
						RobotWithMoves.executeChangeOfDirection(robot.robot,
								robot.directionFromRobot,
								directionFromScreenshot);
					} else {
						robot.directionFromRobot.set(directionFromScreenshot);
					}
				}

				if (false) {
					robot.doSaveCapture(capture);
				}
			} catch (Throwable t) {
				LOGGER.error("ARG", t);
			}
		}, 1, 1, TimeUnit.MILLISECONDS);
	}

	public static void resetCrop(RunScreenBot_VampireSurvivors robot) {
		BufferedImage capture = robot.captureNow();

		robot.recordedTop.set(0);
		robot.recordedLeft.set(0);
		robot.recordedBottom.set(capture.getHeight());
		robot.recordedRight.set(capture.getWidth());

		robot.cropThenShow(capture);
	}

	@Deprecated(since = "It is doomed to try detecting the window automatically")
	public static boolean updateGameLocation(RunScreenBot_VampireSurvivors robot,
			AtomicInteger nbTry,
			BufferedImage capture) {
		boolean updated = false;

		int top = VampireSurvivorExpertise.gameTop(capture);
		int left = VampireSurvivorExpertise.gameLeft(capture);
		int bottom = VampireSurvivorExpertise.gameBottom(capture);
		int right = VampireSurvivorExpertise.gameRight(capture);

		if (top >= bottom) {
			LOGGER.warn("We reject top={} >= bottom={}", top, bottom);
			robot.robotStatus.setText("Retry reset the game location! (" + nbTry.getAndIncrement() + ")");
		} else if (left >= right) {
			LOGGER.warn("We reject left={} >= right={}", left, right);
			robot.robotStatus.setText("Retry reset the game location! (" + nbTry.getAndIncrement() + ")");
		} else {
			saveLogIfDifferent(robot.recordedTop, top, "gameTop");
			saveLogIfDifferent(robot.recordedLeft, left, "gameLeft");
			saveLogIfDifferent(robot.recordedBottom, bottom, "gameBottom");
			saveLogIfDifferent(robot.recordedRight, right, "gameRight");

			updated = true;
		}
		return updated;
	}

	public static void saveLogIfDifferent(AtomicInteger currentValue, int newValue, String message) {
		if (currentValue.get() != newValue) {
			LOGGER.info("{} changed from {} to {}", "bottom", currentValue, newValue);
			currentValue.set(newValue);
		}
	}

	public static void saveLogIfDifferent(AtomicLong currentValue, long newValue, String message) {
		if (currentValue.get() != newValue) {
			LOGGER.info("{} changed from {} to {}", "bottom", currentValue, newValue);
			currentValue.set(newValue);
		}
	}

	public BufferedImage captureNow() {
		BufferedImage capture = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

		return capture;
	}

	public BufferedImage cropThenShow(BufferedImage capture) {
		BufferedImage crop = capture.getSubimage(recordedLeft.get(),
				recordedTop.get(),
				recordedRight.get() - recordedLeft.get(),
				recordedBottom.get() - recordedTop.get());

		widthBeforeCrop.set(capture.getWidth());
		heightBeforeCrop.set(capture.getHeight());
		crop = RobotWithEye.resize(crop, 640, 480);

		recordedAndEnriched.setImage(crop);

		panel.invalidate();
		// Use this ONLY if invalidate doesn't work...
		panel.revalidate();
		panel.repaint();

		return crop;
	}

	public BufferedImage captureNowThenCropThenShow() {
		BufferedImage capture = captureNow();

		return cropThenShow(capture);
	}

	public void doSaveCapture(BufferedImage capture) {
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

	public void initNeutralColors() {
		try {
			String r = "/go2/easy-monsters=2.png";
			BufferedImage capture = ImageIO.read(new ClassPathResource(r).getURL());

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
			int[] jArray =
					IntStream.iterate(blockHeight, j -> j < capture.getHeight() - blockHeight, j -> j + blockHeight / 2)
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
								.min(Comparator.comparing(h -> h.getKey().normalizedHammingDistance(currentBlockHash)));

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
								LOGGER.info("We stopped neutral hashed detection when covering {}%",
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
			}
			LOGGER.info("We consider {} neutral hashes", neutralHashes.size());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public int computeDirectionFromRobot(BufferedImage capture) {
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

	private void updateRobotDirection(BufferedImage screenShot, AtomicInteger directionFromRobot) {
		int directionFromScreenshot = computeDirectionFromRobot(screenShot);
		if (directionFromRobot.get() != directionFromScreenshot) {
			LOGGER.info("Capture suggests we should go to {}", directionFromRobot);
			directionFromRobot.set(directionFromScreenshot);
		}
	}
}