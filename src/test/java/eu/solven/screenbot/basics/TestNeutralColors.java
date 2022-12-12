package eu.solven.screenbot.basics;

import java.awt.AWTException;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.solven.screenbot.RunScreenBot_VampireSurvivors;

public class TestNeutralColors {
	// @BeforeEach
	// public void reset() {
	// RunScreenBot_VampireSurvivors.reset();
	// }

	@Test
	public void testIsMenu_isMenu() throws IOException, AWTException {
		// Assertions.assertThat(RunScreenBot_VampireSurvivors.neutralColors).isEmpty();

		RunScreenBot_VampireSurvivors robot = new RunScreenBot_VampireSurvivors();
		robot.initNeutralColors();

		// Assertions.assertThat(RunScreenBot_VampireSurvivors.neutralColors)
		// .map(HumanTranscoding::toHex)
		// .contains("#000000", "#262844", "#21233d", "#171828")
		// .hasSize(4);

		Assertions.assertThat(robot.neutralHashes.asMap()).hasSize(9);
	}
}
