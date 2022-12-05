package eu.solven.screenbot;

import java.util.Random;

public class Statistics {

	// https://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
	public static void shuffleArray(Random r, int[] array) {
		int index;
		for (int i = array.length - 1; i > 0; i--) {
			index = r.nextInt(i + 1);
			if (index != i) {
				array[index] ^= array[i];
				array[i] ^= array[index];
				array[index] ^= array[i];
			}
		}
	}
}
