package com.mojang.escape;

/**
 * Lerp mouse-look (ADR 0002, V2). Mouse deltas accumulate into a pending yaw;
 * each tick releases a fixed fraction, so a flick eases over several frames
 * instead of snapping. Keyboard turning is untouched — this only ever adds to
 * the player's rot.
 */
public final class MouseLook {
	public static final double SENSITIVITY = 0.0025;
	public static final double SMOOTHING = 0.35;

	private double pendingYaw;

	/** Positive dx (mouse moved right) turns right, which is negative rot in this engine. */
	public void mouseDelta(int dx) {
		pendingYaw += -dx * SENSITIVITY;
	}

	/** Returns the yaw step to apply this tick. */
	public double tick() {
		double step = pendingYaw * SMOOTHING;
		pendingYaw -= step;
		return step;
	}

	public double pendingYaw() {
		return pendingYaw;
	}
}
