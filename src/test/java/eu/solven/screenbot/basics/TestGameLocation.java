package eu.solven.screenbot.basics;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import eu.solven.screenbot.RunScreenBot_VampireSurvivors;

public class TestGameLocation {
	@Disabled
	@Test
	public void testIsMenu_isMenu() throws IOException, AWTException {
		RunScreenBot_VampireSurvivors robot = new RunScreenBot_VampireSurvivors();

		BufferedImage img = ImageIO.read(new ClassPathResource("/chooseoption/not_fullscreen-levelup.png").getURL());
		RunScreenBot_VampireSurvivors.updateGameLocation(robot, new AtomicInteger(), img);

		Assertions.assertThat(robot.recordedTop.get()).isEqualTo(123);
		Assertions.assertThat(robot.recordedLeft.get()).isEqualTo(123);
		Assertions.assertThat(robot.recordedBottom.get()).isEqualTo(123);
		Assertions.assertThat(robot.recordedRight.get()).isEqualTo(123);
	}
}
