package eu.solven.screenbot.basics;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import eu.solven.screenbot.HumanTranscoding;

public class TestDirections {
	@Test
	public void testIsMenu_isMenu() throws IOException {
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(0)).isEqualTo(5);
		// left and right at the same time
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(1 + 4)).isEqualTo(5);

		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(1)).isEqualTo(4);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(1 + 2)).isEqualTo(7);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(2)).isEqualTo(8);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(2 + 4)).isEqualTo(9);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(4)).isEqualTo(6);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(4 + 8)).isEqualTo(3);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(8)).isEqualTo(2);
		Assertions.assertThat(HumanTranscoding.convertFromHumanToRobot(8 + 1)).isEqualTo(1);
	}
}
