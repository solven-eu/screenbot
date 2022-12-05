package eu.solven.screenbot;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.google.common.util.concurrent.AtomicLongMap;

/**
 * Anything specific/hardcoded for the game
 * 
 * @author Benoit Lacelle
 *
 */
public class VampireSurvivorExpertise {
	// Blue
	final static String menuButton = "#2741CD";
	// Green
	final static String menuConfirm = "#3FB55C";
	// Red
	final static String menuBack = "#D32B0C";
	// Red
	final static String redLife = "#E00808";

	final static Set<String> redSuffering = Set.of("#D60E05");
	final static Set<String> blueDiamond = Set.of("#0F7599");

	final static String blueExperience = "#2266DD";

	final static String eclipseWhite = "#FFFFFF";
	final static List<String> menuColors = Arrays.asList(menuButton, menuConfirm, menuBack);

	public static final AtomicLong maxXpRatio = new AtomicLong();

	public static int computeXp(BufferedImage capture) {
		AtomicLongMap<Integer> sampleToCount = RobotWithEye.groupByColor(capture);

		long nbPixelsWithMenuColor = Stream.of(blueExperience)
				.mapToInt(c -> HumanTranscoding.hexToInt(c))
				.mapToLong(sampleToCount::get)
				.sum();

		if (nbPixelsWithMenuColor == 0) {
			return 0;
		}

		long nbPixels = capture.getWidth() * capture.getHeight();
		long percentThousandOfScreen = nbPixelsWithMenuColor * 100_000 / nbPixels;
		if (percentThousandOfScreen > maxXpRatio.get()) {
			// We encounter a new maximum
			maxXpRatio.set(percentThousandOfScreen);
		}

		return (int) (percentThousandOfScreen * 100 / maxXpRatio.get());
	}

	public static boolean isMenu(BufferedImage capture) {
		List<String> colors = Arrays.asList(menuButton, menuConfirm, menuBack);
		long percentWithColor = RobotWithEye.percentForColors(3, colors, capture);
		return percentWithColor >= 1;
	}

	public static boolean isOtherApp(BufferedImage capture) {
		long percent = RobotWithEye.percentForColors(Arrays.asList(eclipseWhite), capture);

		return percent >= 10;
	}

	public static boolean isSuffering(BufferedImage capture) {
		long percent = RobotWithEye.percentForColors(redSuffering, capture);

		return percent >= 5;
	}
}
