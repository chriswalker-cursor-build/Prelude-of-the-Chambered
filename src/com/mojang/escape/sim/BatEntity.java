package com.mojang.escape.sim;

import com.mojang.escape.EscapeSettings;
import com.mojang.escape.render.Art;
import com.mojang.escape.audio.Sound;
import com.mojang.escape.session.Game;

public class BatEntity extends EnemyEntity {
	public BatEntity(double x, double z) {
		super(x, z, 4 * 8, Art.getCol(0x82666E));
		this.x = x;
		this.z = z;
		health = 2;
		r = 0.3;

		flying = true;
	}
}
