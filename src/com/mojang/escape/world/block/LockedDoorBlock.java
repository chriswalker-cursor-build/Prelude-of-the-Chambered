package com.mojang.escape.world.block;

import com.mojang.escape.sim.*;
import com.mojang.escape.world.Level;

public class LockedDoorBlock extends DoorBlock {
	public LockedDoorBlock() {
		tex = 5;
	}

	public boolean use(Level level, Item item) {
		return false;
	}

	public void trigger(boolean pressed) {
		open = pressed;
	}

}
