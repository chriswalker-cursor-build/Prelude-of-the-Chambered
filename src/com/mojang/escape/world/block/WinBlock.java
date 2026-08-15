package com.mojang.escape.world.block;

import com.mojang.escape.sim.*;

public class WinBlock extends Block {
	public void addEntity(Entity entity) {
		super.addEntity(entity);
		if (entity instanceof Player) {
			((Player)entity).win();
		}
	}
}
