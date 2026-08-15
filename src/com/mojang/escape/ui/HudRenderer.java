package com.mojang.escape.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import com.mojang.escape.render.Art;
import com.mojang.escape.session.Game;
import com.mojang.escape.IntegerScaler;
import com.mojang.escape.sim.Item;
import com.mojang.escape.sim.Player;

/**
 * V4 hotbar/health HUD (ADR 0002). Composited after the integer blit at
 * window resolution, replacing the 29px legacy panel which only mosaics when
 * scaled. Icons are rebuilt from Art.items with the item palette so the look
 * stays 2011.
 */
public final class HudRenderer {
	private static final Color STRIP = new Color(10, 10, 12, 235);
	private static final Color SLOT_BG = new Color(28, 28, 32);
	private static final Color SLOT_EDGE = new Color(70, 70, 78);
	private static final Color SELECTED = new Color(255, 255, 255);
	private static final Color HEALTH_BG = new Color(70, 10, 10);
	private static final Color HEALTH_FG = new Color(220, 40, 40);
	private static final Color TEXT = new Color(235, 235, 235);
	private static final Color GOLD = new Color(230, 200, 60);

	private final Map<Item, BufferedImage> icons = new EnumMap<Item, BufferedImage>(Item.class);

	public void render(Graphics g, Game game, IntegerScaler.Placement placement) {
		Player player = game.player;
		if (player == null) return;

		HudLayout layout = new HudLayout(placement);
		int s = layout.scale();

		HudLayout.Rect strip = layout.strip();
		g.setColor(STRIP);
		g.fillRect(strip.x(), strip.y(), strip.w(), strip.h());

		HudLayout.Rect hp = layout.healthBar();
		g.setColor(HEALTH_BG);
		g.fillRect(hp.x(), hp.y(), hp.w(), hp.h());
		g.setColor(HEALTH_FG);
		g.fillRect(hp.x(), hp.y(), hp.w() * Math.max(0, player.health) / 20, hp.h());
		g.setColor(SLOT_EDGE);
		g.drawRect(hp.x(), hp.y(), hp.w(), hp.h());

		g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 5 * s));
		g.setColor(TEXT);
		g.drawString(player.health + "/20", hp.x() + hp.w() + 3 * s, hp.y() + 5 * s);
		g.setColor(GOLD);
		String counts = "KEYS " + player.keys + "/4   GOLD " + player.loot;
		g.drawString(counts, hp.x() + hp.w() + 24 * s, hp.y() + 5 * s);

		Item selected = player.items[player.selectedSlot];
		if (selected != Item.none) {
			g.setColor(TEXT);
			g.drawString(selected.name, strip.x() + 4 * s, strip.y() + strip.h() - 21 * s + 3 * s);
		}

		for (int i = 0; i < HudLayout.SLOTS; i++) {
			HudLayout.Rect slot = layout.slot(i);
			g.setColor(SLOT_BG);
			g.fillRect(slot.x(), slot.y(), slot.w(), slot.h());
			g.setColor(i == player.selectedSlot ? SELECTED : SLOT_EDGE);
			for (int t = 0; t < Math.max(1, s / 2); t++) {
				g.drawRect(slot.x() + t, slot.y() + t, slot.w() - 2 * t, slot.h() - 2 * t);
			}

			Item item = player.items[i];
			if (item == Item.none) continue;
			HudLayout.Rect icon = layout.icon(i);
			g.drawImage(icon(item), icon.x(), icon.y(), icon.w(), icon.h(), null);

			String badge = null;
			if (item == Item.pistol) badge = String.valueOf(player.ammo);
			if (item == Item.potion) badge = String.valueOf(player.potions);
			if (badge != null) {
				g.setColor(TEXT);
				g.drawString(badge, slot.x() + slot.w() - (badge.length() * 3 + 2) * s, slot.y() + slot.h() - 2 * s);
			}
		}
	}

	/** Rebuilds the 16x16 sheet icon with the item palette (same math as Bitmap.draw). */
	private BufferedImage icon(Item item) {
		BufferedImage cached = icons.get(item);
		if (cached != null) return cached;
		int tint = Art.getCol(item.color);
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int src = Art.items.pixels[(x + item.icon * 16) + y * Art.items.width];
				img.setRGB(x, y, src < 0 ? 0 : 0xFF000000 | (src * tint));
			}
		}
		icons.put(item, img);
		return img;
	}
}
