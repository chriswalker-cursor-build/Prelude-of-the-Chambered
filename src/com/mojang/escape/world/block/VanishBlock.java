package com.mojang.escape.world.block;

import com.mojang.escape.audio.Sound;
import com.mojang.escape.sim.Item;
import com.mojang.escape.render.RubbleSprite;
import com.mojang.escape.world.Level;

public class VanishBlock extends SolidBlock {
	private boolean gone = false;

	public VanishBlock() {
		tex = 1;
	}

	public boolean use(Level level, Item item) {
		if (gone) return false;

		gone = true;
		blocksMotion = false;
		solidRender = false;
		Sound.crumble.play();

		for (int i = 0; i < 32; i++) {
			RubbleSprite sprite = new RubbleSprite();
			sprite.col = col;
			addSprite(sprite);
		}

		return true;
	}
}
