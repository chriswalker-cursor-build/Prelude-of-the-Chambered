package com.mojang.escape.ui;

import com.mojang.escape.IntegerScaler;

/**
 * Pure geometry for the V4 screen-space HUD, derived from the presenter
 * placement so the bar sits exactly over the legacy 29px panel strip at any
 * integer scale. Kept free of AWT so it characterises headless.
 */
public final class HudLayout {
	public static final int VIEWPORT_HEIGHT = 91;
	public static final int PANEL_HEIGHT = 29;
	public static final int SLOTS = 8;

	public record Rect(int x, int y, int w, int h) {
	}

	private final int scale;
	private final Rect strip;

	public HudLayout(IntegerScaler.Placement p) {
		this.scale = p.scale();
		this.strip = new Rect(p.x(), p.y() + VIEWPORT_HEIGHT * p.scale(), 160 * p.scale(), PANEL_HEIGHT * p.scale());
	}

	public int scale() {
		return scale;
	}

	public Rect strip() {
		return strip;
	}

	public Rect healthBar() {
		return new Rect(strip.x() + 4 * scale, strip.y() + 2 * scale, 62 * scale, 5 * scale);
	}

	public Rect slot(int index) {
		int size = 18 * scale;
		int total = SLOTS * size + (SLOTS - 1) * scale;
		int x0 = strip.x() + (strip.w() - total) / 2;
		int y = strip.y() + 9 * scale;
		return new Rect(x0 + index * (size + scale), y, size, size);
	}

	public Rect icon(int slotIndex) {
		Rect s = slot(slotIndex);
		return new Rect(s.x() + scale, s.y() + scale, 16 * scale, 16 * scale);
	}
}
