package eu.solven.screenbot.useless;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ButWeNeverKnow {
	private static final Logger LOGGER = LoggerFactory.getLogger(ButWeNeverKnow.class);

	private static void trackHumanActions(JFrame f, AtomicInteger directionFromHuman) {
		f.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// This assume QUERTY layout
				if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
					LOGGER.info("Human press {}", "left");
					directionFromHuman.set(directionFromHuman.get() | 1);
				} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
					LOGGER.info("Human press {}", "up");
					directionFromHuman.set(directionFromHuman.get() | 2);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
					LOGGER.info("Human press {}", "right");
					directionFromHuman.set(directionFromHuman.get() | 4);
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
					LOGGER.info("Human press {}", "down");
					directionFromHuman.set(directionFromHuman.get() | 8);
				} else {
					LOGGER.info("Human press code={}, char={}", e.getKeyCode(), e.getKeyChar());
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// This assume QUERTY layout
				if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
					LOGGER.info("Human release {}", "left");
					// https://stackoverflow.com/questions/3920307/how-can-i-remove-a-flag-in-c
					directionFromHuman.set(directionFromHuman.get() & ~1);
				} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
					LOGGER.info("Human release {}", "up");
					directionFromHuman.set(directionFromHuman.get() & ~2);
				} else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
					LOGGER.info("Human release {}", "right");
					directionFromHuman.set(directionFromHuman.get() & ~4);
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
					LOGGER.info("Human release {}", "down");
					directionFromHuman.set(directionFromHuman.get() & ~8);
				} else {
					LOGGER.info("Human release code={}, char={}", e.getKeyCode(), e.getKeyChar());
				}
			}
		});
	}
}
