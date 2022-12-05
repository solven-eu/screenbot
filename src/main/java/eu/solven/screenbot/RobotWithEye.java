package eu.solven.screenbot;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicLongMap;

/**
 * Anything related with Image processing, as generic as having an eye.
 * 
 * @author Benoit Lacelle
 *
 */
public class RobotWithEye {
	private static final Logger LOGGER = LoggerFactory.getLogger(RobotWithEye.class);

	// https://stackoverflow.com/questions/9417356/bufferedimage-resize
	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}

	// https://stackoverflow.com/questions/10088465/need-faster-way-to-get-rgb-value-for-each-pixel-of-a-buffered-image
	// https://stackoverflow.com/questions/39649292/imageio-reading-slightly-different-rgb-values-than-other-methods
	public static AtomicLongMap<Integer> groupByColor(BufferedImage capture) {
		return groupByColor(1, capture);
	}

	public static AtomicLongMap<Integer> groupByColor(int blockSize, BufferedImage capture) {
		AtomicLongMap<Integer> sampleToCount = AtomicLongMap.create();

		int[] pixel = new int[capture.getRaster().getNumBands()];
		if (pixel.length != 3) {
			throw new IllegalStateException("We expect only RGB, not alpha");
		}

		for (int j = 0; j < capture.getHeight(); j++) {
			nextPixel: for (int i = 0; i < capture.getWidth(); i++) {
				int rgb = getRbg(capture, pixel, i, j);

				// if (blockSize >= 2) {
				// for (int ii = i - blockSize; ii <= i + blockSize; ii++) {
				// if (ii < 0 || ii >= capture.getWidth()) {
				// continue;
				// }
				// for (int jj = j - blockSize; jj <= j + blockSize; jj++) {
				// if (jj < 0 || jj >= capture.getHeight()) {
				// continue;
				// }
				//
				// if (rgb != getRbg(capture, pixel, ii, jj)) {
				// continue nextPixel;
				// }
				// }
				// }
				// }

				long previous = sampleToCount.getAndIncrement(rgb);
				if (previous == 0) {
					LOGGER.debug("New color: {}", HumanTranscoding.toHex(rgb));
				}

			}
		}
		return sampleToCount;
	}

	private static int getRbg(BufferedImage capture, int[] pixel, int i, int j) {
		capture.getRaster().getPixel(i, j, pixel);
		int rgb = pixel[0] << 16 | pixel[1] << 8 | pixel[2];
		return rgb;
	}

	public static long percentForColors(Collection<String> colors, BufferedImage capture) {
		return percentForColors(1, colors, capture);
	}

	public static long percentForColors(int blockSize, Collection<String> colors, BufferedImage capture) {
		AtomicLongMap<Integer> sampleToCount = RobotWithEye.groupByColor(blockSize, capture);

		long nbPixelsWithColors =
				colors.stream().mapToInt(c -> HumanTranscoding.hexToInt(c)).mapToLong(sampleToCount::get).sum();

		// This may be smaller than 'capture.getWidth() * capture.getHeight()' if blockSize >=2
		long nbPixels = sampleToCount.sum();
		long percentWithColor = nbPixelsWithColors * 100 / nbPixels;
		return percentWithColor;
	}

	public static int getBLockHeight(BufferedImage capture) {
		return capture.getHeight() / 100;
	}

	public static int getBlockWidth(BufferedImage capture) {
		return capture.getWidth() / 100;
	}

	private static void logPixelsWithColor(BufferedImage capture, Integer searched) {
		int[] pixel = new int[3];
		for (int j = 0; j < capture.getHeight(); j++) {
			for (int i = 0; i < capture.getWidth(); i++) {
				pixel = capture.getRaster().getPixel(i, j, pixel);
				int rgb = pixel[0] << 16 | pixel[1] << 8 | pixel[2];

				if (rgb == searched) {
					LOGGER.info("Matching {}: i={} j={}", HumanTranscoding.toHex(searched), i, j);
				}
			}
		}
	}

	private static long distance(int c, int i) {
		double rSquared = Math.pow((c & 0x0000FF) - (i & 0x0000FF), 2);
		double gSquared = Math.pow((c & 0x00FF00) - (i & 0x00FF00), 2);
		double bSquared = Math.pow((c & 0xFF0000) - (i & 0xFF0000), 2);
		return (long) Math.sqrt(rSquared + gSquared + bSquared);
	}
}
