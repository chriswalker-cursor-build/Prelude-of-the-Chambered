package com.mojang.escape.support;

import com.mojang.escape.session.Game;
import com.mojang.escape.sim.Player;
import com.mojang.escape.world.Level;

/**
 * Synthetic-level harness. Builds a bordered map from raw colour values so
 * characterisation can place the player on a known block type without
 * depending on the shipped PNGs.
 */
public final class Worlds {
	public static final int SOLID = 0xFFFFFF;
	public static final int PLAIN = 0x808080;
	public static final int ICE = 0x3F3F60;
	public static final int WATER = 0x0000FF;

	private Worlds() {
	}

	public static class SyntheticLevel extends Level {
		public SyntheticLevel() {
			name = "Synthetic";
		}
	}

	/** Solid border, uniform interior; opaque alpha so every block id decodes to 0. */
	public static Level bordered(int w, int h, int interiorCol) {
		int[] pixels = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				pixels[x + y * w] = 0xFF000000 | (border ? SOLID : interiorCol);
			}
		}
		Game game = new Game();
		game.player = new Player();
		SyntheticLevel level = new SyntheticLevel();
		level.init(game, "synthetic", w, h, pixels);
		return level;
	}

	public static Player placePlayer(Level level, double x, double z, double rot) {
		Player player = level.player;
		player.level = level;
		player.x = x;
		player.z = z;
		player.rot = rot;
		level.addEntity(player);
		return player;
	}
}
