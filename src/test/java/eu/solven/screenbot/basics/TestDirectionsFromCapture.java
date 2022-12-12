package eu.solven.screenbot.basics;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import eu.solven.screenbot.RunScreenBot_VampireSurvivors;

public class TestDirectionsFromCapture {
	// @BeforeEach
	// public void reset() {
	// RunScreenBot_VampireSurvivors.reset();
	// }

	@Test
	public void testIsMenu_isMenu() throws IOException, AWTException {
		RunScreenBot_VampireSurvivors robot = new RunScreenBot_VampireSurvivors();
		robot.initNeutralColors();

		BufferedImage img = ImageIO.read(new ClassPathResource("/go2/easy-monsters=2.png").getURL());
		Assertions.assertThat(robot.computeDirectionFromRobot(img)).isEqualTo(1);
	}
}
