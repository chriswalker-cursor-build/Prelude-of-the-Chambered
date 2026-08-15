package com.mojang.escape.render;

import com.mojang.escape.world.Level;
import com.mojang.escape.world.block.Block;
import com.mojang.escape.world.block.TorchBlock;
import com.mojang.escape.render.Sprite;

/**
 * Torch light query for ADR 0002 V3. Torches contribute a quadratic-falloff
 * pool of light within RADIUS tiles; between torches the 2011 depth fog still
 * dominates. This never darkens a pixel — it only adds to fog brightness.
 */
public final class TorchLight {
	public static final double RADIUS = 2.75;
	/** Brightness units (0-255 scale) added at the torch itself. */
	public static final double STRENGTH = 320;

	private double[] xs = new double[16];
	private double[] zs = new double[16];
	private int count;

	public void collect(Level level) {
		count = 0;
		for (int y = 0; y < level.height; y++) {
			for (int x = 0; x < level.width; x++) {
				Block block = level.getBlock(x, y);
				if (!(block instanceof TorchBlock)) continue;
				double sx = x, sz = y;
				if (!block.sprites.isEmpty()) {
					Sprite flame = block.sprites.get(0);
					sx = x + flame.x;
					sz = y + flame.z;
				}
				if (count == xs.length) {
					xs = java.util.Arrays.copyOf(xs, count * 2);
					zs = java.util.Arrays.copyOf(zs, count * 2);
				}
				xs[count] = sx;
				zs[count] = sz;
				count++;
			}
		}
	}

	public int torchCount() {
		return count;
	}

	/**
	 * Additional brightness (0-255 scale, unclamped) at a world position.
	 * (1 - d/R)^2 concentrates light at the torch so dense torch clusters
	 * (e.g. the prison spawn) still read as pools, not a global lift.
	 */
	public double lightAt(double worldX, double worldZ) {
		double r2 = RADIUS * RADIUS;
		double sum = 0;
		for (int i = 0; i < count; i++) {
			double dx = worldX - xs[i];
			double dz = worldZ - zs[i];
			double d2 = dx * dx + dz * dz;
			if (d2 < r2) {
				double t = 1 - Math.sqrt(d2) / RADIUS;
				sum += t * t * STRENGTH;
			}
		}
		return sum;
	}
}
