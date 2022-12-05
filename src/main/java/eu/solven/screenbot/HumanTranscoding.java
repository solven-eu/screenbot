package eu.solven.screenbot;

import java.awt.Color;

/**
 * Relates with Human lexic. Just for logs, and easy of interpretation by a human
 * 
 * @author Benoit Lacelle
 *
 */
public class HumanTranscoding {

	/**
	 * 
	 * @param color
	 *            like #1D27C1
	 * @return
	 */
	public static int hexToString(String color) {
		if (!color.startsWith("#")) {
			throw new IllegalArgumentException("color: " + color);
		}
		// Skip the leading '#'
		int rgb = Integer.parseInt(color.substring(1), 16);
		return rgb;
	}

	public static int hexToInt(String c) {
		return new Color(hexToString(c)).getRGB() & 0xFFFFFF;
	}

	public static String toHex(int key) {
		// .substring(2) to skip alpha (as the first 2 byte)
		return "#" + Integer.toHexString(key | 0xFF000000).substring(2);
	}

	public static int convertFromHumanToRobot(int human) {
		boolean left = 0 != (human & 1);
		boolean up = 0 != (human & 2);
		boolean right = 0 != (human & 4);
		boolean down = 0 != (human & 8);

		if (left) {
			if (right) {
				return 5;
			} else if (up) {
				return 7;
			} else if (down) {
				return 1;
			} else {
				return 4;
			}
		} else if (right) {
			if (left) {
				return 5;
			} else if (up) {
				return 9;
			} else if (down) {
				return 3;
			} else {
				return 6;
			}
		} else if (up) {
			if (down) {
				return 5;
			} else {
				return 8;
			}
		} else if (down) {
			return 2;
		} else {
			return 5;
		}
	}
}
