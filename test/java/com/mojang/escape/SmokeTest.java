package com.mojang.escape;

import com.mojang.escape.render.Art;
import com.mojang.escape.sim.Item;
import com.mojang.escape.support.Check;

/** Proves the harness, classpath, and resources work before real characterisation lands. */
public final class SmokeTest {
	public static void testItemEnumIsStable() {
		Check.equal(8, Item.values().length, "Item count");
		Check.equal("Pistol", Item.pistol.name, "pistol name");
	}

	public static void testArtResourcesLoadHeadless() {
		Check.equal(128, Art.walls.width, "walls sheet width");
		Check.equal(128, Art.sprites.width, "sprites sheet width");
	}
}
