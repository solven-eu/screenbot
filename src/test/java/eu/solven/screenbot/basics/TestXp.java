package eu.solven.screenbot.basics;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import eu.solven.screenbot.RunScreenBot_VampireSurvivors;
import eu.solven.screenbot.VampireSurvivorExpertise;

public class TestXp {

	@BeforeEach
	public void reset() throws AWTException {
		new RunScreenBot_VampireSurvivors().reset();
	}

	@Test
	public void testXp_empty() throws IOException {
		BufferedImage img = ImageIO.read(new ClassPathResource("/go2/easy-monsters=2.png").getURL());
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img)).isEqualTo(0);
	}

	@Test
	public void testXp_middle() throws IOException {
		BufferedImage img15 = ImageIO.read(new ClassPathResource("/go8/easy-monsters=8.png").getURL());
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img15)).isEqualTo(100);

		BufferedImage img60 = ImageIO.read(new ClassPathResource("/go6/easy-monsters=8-diamonds=6.png").getURL());
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img60)).isEqualTo(100);
		// After detecting a new max, the previous is not 100 anymore
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img15)).isEqualTo(9);

		BufferedImage img80 = ImageIO.read(new ClassPathResource("/go6/easy-monsters=6.png").getURL());
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img80)).isEqualTo(100);
		// After detecting a new max, the previous is not 100 anymore
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img60)).isEqualTo(59);
		Assertions.assertThat(VampireSurvivorExpertise.computeXp(img15)).isEqualTo(5);
	}

}
