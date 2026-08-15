package com.mojang.escape.look;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.input.MouseLook;
import com.mojang.escape.support.Check;

/** V2: lerp mouse-look maths and flag defaults. Keyboard look stays locked by S1. */
public final class MouseLookTest {

	public static void testDefaultModeIsKeyboard() {
		System.clearProperty("escape.look.mode");
		Check.equal(EscapeSettings.LookMode.KEYBOARD, EscapeSettings.lookMode(), "default look mode");
	}

	public static void testFlagEnablesMouseLerp() {
		System.setProperty("escape.look.mode", "mouseLerp");
		try {
			Check.equal(EscapeSettings.LookMode.MOUSE_LERP, EscapeSettings.lookMode(), "flagged look mode");
		} finally {
			System.clearProperty("escape.look.mode");
		}
	}

	public static void testNoInputProducesNoRotation() {
		MouseLook look = new MouseLook();
		Check.close(0.0, look.tick(), 0.0, "idle tick");
	}

	public static void testRightwardMouseTurnsRight() {
		MouseLook look = new MouseLook();
		look.mouseDelta(100);
		double step = look.tick();
		Check.isTrue(step < 0, "positive dx yields negative rot (turn right)");
		Check.close(-100 * MouseLook.SENSITIVITY * MouseLook.SMOOTHING, step, 1e-12, "first step fraction");
	}

	public static void testFlickEasesOverSeveralTicksNotOne() {
		MouseLook look = new MouseLook();
		look.mouseDelta(-200); // flick left
		double target = 200 * MouseLook.SENSITIVITY;
		double first = look.tick();
		Check.isTrue(first < target * 0.5, "no single-frame snap");
		double applied = first;
		double previous = first;
		for (int i = 0; i < 40; i++) {
			double step = look.tick();
			Check.isTrue(step >= 0 && step <= previous + 1e-15, "steps decay monotonically");
			previous = step;
			applied += step;
		}
		Check.close(target, applied, target * 0.001, "converges to the full mouse delta");
	}

	public static void testDeltasAccumulateWhilePending() {
		MouseLook look = new MouseLook();
		look.mouseDelta(40);
		look.mouseDelta(60);
		Check.close(-100 * MouseLook.SENSITIVITY, look.pendingYaw(), 1e-12, "pending sums deltas");
	}
}
