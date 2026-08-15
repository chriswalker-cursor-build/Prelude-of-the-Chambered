package com.mojang.escape.present;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.IntegerScaler;
import com.mojang.escape.support.Check;

/** V1: integer-fill placement math and flag defaults. */
public final class PresenterTest {

	public static void testDefaultModeIsLegacyFixed4() {
		System.clearProperty("escape.present.mode");
		Check.equal(EscapeSettings.PresentMode.FIXED4, EscapeSettings.presentMode(), "default present mode");
	}

	public static void testFlagEnablesIntegerFill() {
		System.setProperty("escape.present.mode", "integerFill");
		try {
			Check.equal(EscapeSettings.PresentMode.INTEGER_FILL, EscapeSettings.presentMode(), "flagged present mode");
		} finally {
			System.clearProperty("escape.present.mode");
		}
	}

	public static void testLegacyWindowIsExactlyScaleFour() {
		IntegerScaler.Placement p = IntegerScaler.fit(160, 120, 640, 480);
		Check.equal(4, p.scale(), "scale");
		Check.equal(0, p.x(), "x");
		Check.equal(0, p.y(), "y");
		Check.equal(640, p.w(), "w");
		Check.equal(480, p.h(), "h");
	}

	public static void testFullHdLetterboxesAtScaleNine() {
		IntegerScaler.Placement p = IntegerScaler.fit(160, 120, 1920, 1080);
		Check.equal(9, p.scale(), "scale floors to min axis");
		Check.equal(1440, p.w(), "w");
		Check.equal(1080, p.h(), "h fills short axis");
		Check.equal(240, p.x(), "centred letterbox");
		Check.equal(0, p.y(), "no vertical bars");
	}

	public static void testNeverScalesBelowOne() {
		IntegerScaler.Placement p = IntegerScaler.fit(160, 120, 100, 90);
		Check.equal(1, p.scale(), "clamped to 1 on tiny canvas");
		Check.equal(160, p.w(), "w stays native");
	}

	public static void testOddCanvasCentersWithIntegerPixels() {
		IntegerScaler.Placement p = IntegerScaler.fit(160, 120, 1001, 749);
		Check.equal(6, p.scale(), "scale 6 fits 1001x749");
		Check.equal(960, p.w(), "w");
		Check.equal(720, p.h(), "h");
		Check.equal(20, p.x(), "x centred");
		Check.equal(14, p.y(), "y centred");
	}
}
