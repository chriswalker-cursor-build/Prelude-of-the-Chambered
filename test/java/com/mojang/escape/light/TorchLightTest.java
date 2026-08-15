package com.mojang.escape.light;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.session.Game;
import com.mojang.escape.sim.Player;
import com.mojang.escape.render.Bitmap3D;
import com.mojang.escape.render.TorchLight;
import com.mojang.escape.support.Check;
import com.mojang.escape.support.Worlds;

/** V3: torch-radius lighting — falloff maths and a headless render comparison. */
public final class TorchLightTest {

	public static void testDefaultModeIsDepthFog() {
		System.clearProperty("escape.light.mode");
		Check.equal(EscapeSettings.LightMode.DEPTH_FOG, EscapeSettings.lightMode(), "default light mode");
	}

	public static void testFlagEnablesTorchRadius() {
		System.setProperty("escape.light.mode", "torchRadius");
		try {
			Check.equal(EscapeSettings.LightMode.TORCH_RADIUS, EscapeSettings.lightMode(), "flagged light mode");
		} finally {
			System.clearProperty("escape.light.mode");
		}
	}

	public static void testFalloffIsQuadraticAndBounded() {
		Game game = torchWorld();
		TorchLight light = new TorchLight();
		light.collect(game.level);
		Check.equal(1, light.torchCount(), "one torch collected");

		double atTorch = light.lightAt(5.0, 8.0);
		Check.close(TorchLight.STRENGTH, atTorch, 1e-9, "full strength at the torch");
		double atOne = light.lightAt(5.0, 7.0);
		double atTwo = light.lightAt(5.0, 6.0);
		Check.isTrue(atOne > atTwo && atTwo > 0, "monotonic falloff");
		Check.close(0.0, light.lightAt(5.0, 8.0 + TorchLight.RADIUS), 1e-9, "zero at radius");
		Check.close(0.0, light.lightAt(5.0, 8.0 + TorchLight.RADIUS + 2), 1e-9, "zero beyond radius");
	}

	public static void testTorchModeOnlyBrightensRenderedFrame() {
		Game game = torchWorld();

		System.clearProperty("escape.light.mode");
		int[] fogFrame = renderFrame(game);

		System.setProperty("escape.light.mode", "torchRadius");
		int[] torchFrame;
		try {
			torchFrame = renderFrame(game);
		} finally {
			System.clearProperty("escape.light.mode");
		}

		int brightened = 0;
		for (int i = 0; i < fogFrame.length; i++) {
			int fr = (fogFrame[i] >> 16) & 0xff, fg = (fogFrame[i] >> 8) & 0xff, fb = fogFrame[i] & 0xff;
			int tr = (torchFrame[i] >> 16) & 0xff, tg = (torchFrame[i] >> 8) & 0xff, tb = torchFrame[i] & 0xff;
			Check.isTrue(tr >= fr && tg >= fg && tb >= fb, "torch light never darkens pixel " + i);
			if (tr > fr || tg > fg || tb > fb) brightened++;
		}
		Check.isTrue(brightened > 100, "a visible pool of pixels brightened (got " + brightened + ")");
	}

	private static Game torchWorld() {
		int w = 11, h = 11;
		int[] pixels = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
				pixels[x + y * w] = 0xFF000000 | (border ? Worlds.SOLID : Worlds.PLAIN);
			}
		}
		pixels[5 + 8 * w] = 0xFF000000 | 0xFF3A02; // torch ahead of the player
		Game game = new Game();
		game.player = new Player();
		Worlds.SyntheticLevel level = new Worlds.SyntheticLevel();
		level.init(game, "torchWorld", w, h, pixels);
		game.level = level;
		Worlds.placePlayer(level, 5.0, 5.0, 0.0);
		return game;
	}

	private static int[] renderFrame(Game game) {
		Bitmap3D viewport = new Bitmap3D(160, 91);
		viewport.render(game);
		viewport.postProcess(game.level);
		return viewport.pixels.clone();
	}
}
