package com.mojang.escape.hud;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.session.Game;
import com.mojang.escape.IntegerScaler;
import com.mojang.escape.sim.Item;
import com.mojang.escape.render.Screen;
import com.mojang.escape.support.Check;
import com.mojang.escape.ui.HudLayout;
import com.mojang.escape.ui.HudRenderer;

/** V4: hotbar/health HUD — layout maths, Screen blanking, and a headless composite render. */
public final class HudTest {

	public static void testDefaultModeIsLegacyPanel() {
		System.clearProperty("escape.hud.mode");
		Check.equal(EscapeSettings.HudMode.LEGACY_PANEL, EscapeSettings.hudMode(), "default hud mode");
	}

	public static void testFlagEnablesHotbar() {
		System.setProperty("escape.hud.mode", "hotbar");
		try {
			Check.equal(EscapeSettings.HudMode.HOTBAR, EscapeSettings.hudMode(), "flagged hud mode");
		} finally {
			System.clearProperty("escape.hud.mode");
		}
	}

	public static void testLayoutSitsOverThePanelStripAtAnyScale() {
		for (int scale : new int[] { 4, 9 }) {
			IntegerScaler.Placement p = new IntegerScaler.Placement(scale, 17, 3, 160 * scale, 120 * scale);
			HudLayout layout = new HudLayout(p);
			HudLayout.Rect strip = layout.strip();
			Check.equal(3 + 91 * scale, strip.y(), "strip starts under viewport at scale " + scale);
			Check.equal(29 * scale, strip.h(), "strip height at scale " + scale);
			Check.equal(160 * scale, strip.w(), "strip width at scale " + scale);

			HudLayout.Rect first = layout.slot(0);
			HudLayout.Rect last = layout.slot(7);
			Check.isTrue(first.x() >= strip.x(), "hotbar inside strip left");
			Check.isTrue(last.x() + last.w() <= strip.x() + strip.w(), "hotbar inside strip right");
			int gap = layout.slot(1).x() - (first.x() + first.w());
			Check.equal(scale, gap, "one-scaled-pixel gap between slots");
		}
	}

	public static void testLegacyPanelIsBlankedInHotbarMode() {
		Game game = new Game();
		game.newGame();
		game.setMenu(null);

		System.setProperty("escape.hud.mode", "hotbar");
		int[] hotbarFrame;
		try {
			hotbarFrame = renderScreen(game);
		} finally {
			System.clearProperty("escape.hud.mode");
		}
		int[] legacyFrame = renderScreen(game);

		int panelStart = 160 * (120 - HudLayout.PANEL_HEIGHT);
		boolean legacyHasPanel = false;
		for (int i = panelStart; i < 160 * 120; i++) {
			Check.equal(0, hotbarFrame[i], "hotbar mode blanks low-res panel pixel " + i);
			if (legacyFrame[i] != 0) legacyHasPanel = true;
		}
		Check.isTrue(legacyHasPanel, "legacy mode still draws the 2011 panel");
	}

	public static void testHudRendersHealthAndSelectionHeadless() {
		Game game = new Game();
		game.newGame();
		game.player.items[0] = Item.powerGlove;
		game.player.health = 13;

		IntegerScaler.Placement p = new IntegerScaler.Placement(4, 0, 0, 640, 480);
		BufferedImage canvas = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = canvas.createGraphics();
		new HudRenderer().render(g, game, p);
		g.dispose();

		HudLayout layout = new HudLayout(p);
		HudLayout.Rect hp = layout.healthBar();
		int fill = canvas.getRGB(hp.x() + 2, hp.y() + hp.h() / 2) & 0xffffff;
		Check.isTrue(((fill >> 16) & 0xff) > 150 && ((fill >> 8) & 0xff) < 90, "health bar leading edge is red");
		int pastFill = canvas.getRGB(hp.x() + hp.w() - 2, hp.y() + hp.h() / 2) & 0xffffff;
		Check.isTrue(pastFill != fill, "13/20 bar does not reach the end");

		HudLayout.Rect sel = layout.slot(game.player.selectedSlot);
		int border = canvas.getRGB(sel.x(), sel.y()) & 0xffffff;
		Check.equal(0xffffff, border, "selected slot border is highlighted");
		HudLayout.Rect icon = layout.icon(0);
		int slotBackground = 0x1C1C20; // HudRenderer.SLOT_BG
		boolean iconInk = false;
		for (int y = 0; y < icon.h() && !iconInk; y += 2) {
			for (int x = 0; x < icon.w() && !iconInk; x += 2) {
				if ((canvas.getRGB(icon.x() + x, icon.y() + y) & 0xffffff) != slotBackground) {
					iconInk = true;
				}
			}
		}
		Check.isTrue(iconInk, "power glove icon drew pixels in slot 0");
	}

	private static int[] renderScreen(Game game) {
		Screen screen = new Screen(160, 120);
		screen.render(game, true);
		return screen.pixels.clone();
	}
}
