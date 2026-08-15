package com.mojang.escape.world.block;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.render.Art;
import com.mojang.escape.audio.Sound;
import com.mojang.escape.session.Game;
import com.mojang.escape.sim.*;
import com.mojang.escape.render.Sprite;

public class LootBlock extends Block {
	private boolean taken = false;
	private Sprite sprite;

	public LootBlock() {
		sprite = new Sprite(0, 0, 0, 16 + 2, Art.getCol(0xffff80));
		addSprite(sprite);
		blocksMotion = true;
	}

	public void addEntity(Entity entity) {
		super.addEntity(entity);
		if (!taken && entity instanceof Player) {
			sprite.removed = true;
			taken = true;
			blocksMotion = false;
			((Player) entity).loot++;
			Sound.pickup.play();
			
		}
	}

	public boolean blocks(Entity entity) {
		if (entity instanceof Player) return false;
		return blocksMotion;
	}
}
