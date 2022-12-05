package eu.solven.screenbot.basics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import eu.solven.screenbot.VampireSurvivorExpertise;

public class TestOtherApp {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestOtherApp.class);

	@Test
	public void testIsMenu_isMenu() throws IOException {
		Stream.of(new PathMatchingResourcePatternResolver().getResources("/otherapp/*")).forEach(resource -> {
			LOGGER.info("Processing: {}", resource);
			try {
				BufferedImage img = ImageIO.read(resource.getURL());

				Assertions.assertThat(VampireSurvivorExpertise.isOtherApp(img)).isTrue();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test
	public void testIsMenu_notMenu() throws IOException {
		Stream.of(new PathMatchingResourcePatternResolver().getResources("/chooseoption/*")).forEach(resource -> {
			LOGGER.info("Processing: {}", resource);
			try {
				BufferedImage img = ImageIO.read(resource.getURL());

				Assertions.assertThat(VampireSurvivorExpertise.isOtherApp(img)).isFalse();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		Stream.of(new PathMatchingResourcePatternResolver().getResources("/menu/*")).forEach(resource -> {
			LOGGER.info("Processing: {}", resource);
			try {
				BufferedImage img = ImageIO.read(resource.getURL());

				Assertions.assertThat(VampireSurvivorExpertise.isOtherApp(img)).isFalse();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		Stream.of(new PathMatchingResourcePatternResolver().getResources("/suffering/*")).forEach(resource -> {
			LOGGER.info("Processing: {}", resource);
			try {
				BufferedImage img = ImageIO.read(resource.getURL());

				Assertions.assertThat(VampireSurvivorExpertise.isOtherApp(img)).isFalse();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}
