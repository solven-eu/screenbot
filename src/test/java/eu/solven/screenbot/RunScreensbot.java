package eu.solven.screenbot;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicLongMap;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.LoadLibs;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.WPGMCLinkage;
import smile.math.distance.Distance;
import smile.math.distance.EuclideanDistance;

public class RunScreensbot {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunScreensbot.class);

	// brew install tesseract
	// brew install tesseract-lang
	static {
		// https://stackoverflow.com/questions/21394537/tess4j-unsatisfied-link-error-on-mac-os-x
		// System.setProperty("jna.library.path", "/opt/local/lib/");
		System.setProperty("jna.library.path", "/usr/local/Cellar/tesseract/5.1.0/lib/");

		// https://github.com/nguyenq/tess4j/issues/194
		System.setProperty("jna.debug", "true");
		System.setProperty("jna.debug_load", "true");
	}

	public static void main(String[] args) throws IOException {
		BufferedImage capture = captureImage();

		Path tmpPath = Files.createTempFile("screenbot", ".bmp");
		LOGGER.info("Saving screenshot to {}", tmpPath);

		ImageIO.write(capture, "bmp", tmpPath.toFile());

		// pocTesseract(capture);

		int squareSize = 3;
		AtomicLongMap<Integer> sampleToCount = AtomicLongMap.create();
		for (int i = 0; i < capture.getWidth(); i++) {
			if (i < squareSize || i >= capture.getWidth() - squareSize) {
				continue;
			}
			int constI = i;
			for (int j = 0; j < capture.getHeight(); j++) {
				if (j < squareSize || j >= capture.getHeight() - squareSize) {
					continue;
				}
				int constJ = j;

				int rgb = capture.getRGB(i, j);

				OptionalInt diffII = IntStream.rangeClosed(-1 * squareSize, squareSize).filter(ii -> {
					return IntStream.rangeClosed(-1 * squareSize, squareSize).filter(jj -> {

						int rgbIIJJ = capture.getRGB(constI + ii, constJ + jj);

						return distance(new Color(rgbIIJJ), new Color(rgb)) > 5;
					}).findAny().isPresent();
				}).findAny();

				if (diffII.isPresent()) {
					LOGGER.trace("This pixel is not equals to its square: skip as we are looking for 1-color blocks");
				} else {
					sampleToCount.incrementAndGet(rgb);
				}

			}
		}

		Color redByVampireSurvivor = new Color(215, 36, 24);
		Color greenByVampireSurvivor = new Color(13, 172, 88);
		LOGGER.info("Red by VampireSurvivor: {}", sampleToCount.get(redByVampireSurvivor.getRGB()));
		LOGGER.info("Green by VampireSurvivor: {}", sampleToCount.get(greenByVampireSurvivor.getRGB()));

		Map<Integer, Integer> colorToPartitionColor = new HashMap<>();

		int nbColorGroups = 32;

		if (sampleToCount.size() > nbColorGroups) {
			LOGGER.info("Clusterize pixels into {} partitions (size={})", nbColorGroups, sampleToCount.size());

			Map.Entry[] asArray = sampleToCount.asMap().entrySet().stream().toArray(Map.Entry[]::new);

			@SuppressWarnings("unchecked")
			Linkage linkage = new WPGMCLinkage(asArray.length,
					WPGMCLinkage.proximity(asArray, new Distance<Map.Entry<Integer, Long>>() {
						private static final long serialVersionUID = 5036431033221872375L;

						@Override
						public double d(Map.Entry<Integer, Long> x, Map.Entry<Integer, Long> y) {
							Color left = new Color(x.getKey());
							Color right = new Color(y.getKey());
							return new EuclideanDistance().d(
									new int[] { left.getRed(), left.getGreen(), left.getBlue() },
									new int[] { right.getRed(), right.getGreen(), right.getBlue() });
						}
					}));
			HierarchicalClustering clustering = HierarchicalClustering.fit(linkage);
			int[] indexToPartition = clustering.partition(128);
			// ScatterPlot.plot(x, y, '.', Palette.COLORS).window();

			AtomicLongMap<Integer> reducedSampleToCount = AtomicLongMap.create();

			Map<Integer, Integer> partitionToFirstColor = new HashMap<>();
			IntStream.range(0, indexToPartition.length).forEach(index -> {
				int partition = indexToPartition[index];
				Map.Entry<Integer, Long> entry = asArray[index];

				if (!partitionToFirstColor.containsKey(partition)) {
					// This is the first entry for given partition: register its color as color of the partition
					partitionToFirstColor.put(partition, entry.getKey());
				}

				Integer partitionColor = partitionToFirstColor.get(partition);
				if (!colorToPartitionColor.containsKey(entry.getKey())) {
					colorToPartitionColor.put(entry.getKey(), partitionColor);
				}

				reducedSampleToCount.addAndGet(partitionColor, entry.getValue());
			});

			sampleToCount = reducedSampleToCount;
		} else {
			sampleToCount.asMap().keySet().forEach(color -> colorToPartitionColor.put(color, color));
		}

		AtomicInteger index = new AtomicInteger(0);

		// Use 'Digital Color Meter' to know the color of anything in screen
		sampleToCount.asMap()
				.entrySet()
				.stream()
				.sorted(Comparator.comparing(e -> -e.getValue()))
				// Discard trivial colors
				// .filter(e -> e.getKey().intValue() != Color.WHITE.getRGB()
				// && e.getKey().intValue() != Color.BLACK.getRGB())
				.limit(nbColorGroups)
				.forEach(e -> {
					int rgbAsInt = e.getKey();
					Color c = new Color(rgbAsInt);

					LOGGER.info("Sample {} has count={}", c, e.getValue());

					int discardedMarkerColor = Color.ORANGE.getRGB();

					for (int i = 0; i < capture.getWidth(); i++) {
						for (int j = 0; j < capture.getHeight(); j++) {
							int rgb = capture.getRGB(i, j);

							if (discardedMarkerColor == rgb) {
								// Do not process pixels previously marked
								continue;
							} else if (!colorToPartitionColor.containsKey(rgb)) {
								// This pixel has supposedly been discarded previously
								continue;
							}

							int partitionRgb = colorToPartitionColor.get(rgb);

							if (partitionRgb == rgbAsInt) {
								capture.setRGB(i, j, discardedMarkerColor);
							}
						}
					}

					try {
						File tmpFile = new File(tmpPath.getParent().toFile(),
								tmpPath.getFileName().toString() + "." + index.getAndIncrement() + ".bmp");
						ImageIO.write(capture, "bmp", tmpFile);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
	}

	private static int distance(Color color, Color color2) {
		int redDiff = color.getRed() - color2.getRed();
		int greenDiff = color.getGreen() - color2.getGreen();
		int blueDiff = color.getBlue() - color2.getBlue();
		return (int) Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
	}

	private static void pocTesseract(BufferedImage capture) {
		ITesseract tesseract = new Tesseract();

		// In case you don't have your own tessdata, let it also be extracted for you
		File tessDataFolder = LoadLibs.extractTessResources("tessdata");

		// Set the tessdata path
		tesseract.setDatapath(tessDataFolder.getAbsolutePath());

		// tesseract.setDatapath("src/main/resources/tessdata");
		tesseract.setLanguage("eng");
		tesseract.setPageSegMode(1);
		tesseract.setOcrEngineMode(1);
		try {
			List<Word> words = tesseract.getWords(capture, 1);
			LOGGER.info("Words: {}", words);

			// String result = tesseract.doOCR(capture);
			// LOGGER.info("Text: {}", result);
		} catch (UnsatisfiedLinkError e) {
			LOGGER.warn("Issue with Tesseract binaries", e);
			LOGGER.info("Go with: 'brew install tesseract'");
			// } catch (TesseractException e) {
			// throw new IllegalArgumentException("Issue with Tesseract", e);
		}
	}

	private static BufferedImage captureImage() {
		Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

		// screenRect = new Rectangle(0, 250, 800, 500);
		Robot robot;
		try {
			robot = new Robot();
		} catch (AWTException e) {
			throw new IllegalStateException("Issue initializing the screenshot feature", e);
		}
		BufferedImage capture = robot.createScreenCapture(screenRect);
		return capture;
	}
}
