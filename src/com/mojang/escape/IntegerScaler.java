package com.mojang.escape;

/**
 * Integer-only fit of the 160x120 framebuffer into an arbitrary canvas.
 * Non-integer stretch would smear the software raycaster's pixels, so the
 * scale is floored and the remainder letterboxed (ADR 0002, V1).
 */
public final class IntegerScaler {
	public record Placement(int scale, int x, int y, int w, int h) {
	}

	private IntegerScaler() {
	}

	public static Placement fit(int frameW, int frameH, int canvasW, int canvasH) {
		int scale = Math.max(1, Math.min(canvasW / frameW, canvasH / frameH));
		int w = frameW * scale;
		int h = frameH * scale;
		return new Placement(scale, (canvasW - w) / 2, (canvasH - h) / 2, w, h);
	}
}
