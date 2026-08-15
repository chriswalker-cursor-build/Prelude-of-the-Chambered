package com.mojang.escape.dev;

import com.mojang.escape.session.Game;
import com.mojang.escape.support.Check;
import com.mojang.escape.world.block.LadderBlock;

/** Dev spawn utility: unset property keeps 2011 newGame; set property starts in the named level. */
public final class DevSpawnTest {

	public static void testDefaultNewGameStillStartsInThePrison() {
		System.clearProperty("escape.dev.spawnLevel");
		Game game = new Game();
		game.newGame();
		Check.equal("The Prison", game.level.name, "default start level");
		Check.equal(26, game.level.xSpawn, "yellow-pixel spawn untouched");
	}

	public static void testDevSpawnStartsInNamedLevelAtLadder() {
		System.setProperty("escape.dev.spawnLevel", "dungeon");
		try {
			Game game = new Game();
			Check.isTrue(game.menu == null, "dev boot skips the title menu");
			Check.equal("The Dungeons", game.level.name, "dev spawn level");
			Check.instanceOf(LadderBlock.class,
					game.level.getBlock(game.level.xSpawn, game.level.ySpawn), "spawns at the id-1 ladder");
			Check.close(game.level.xSpawn, game.player.x, 1e-12, "player at ladder x");
			Check.close(game.level.ySpawn, game.player.z, 1e-12, "player at ladder z");
		} finally {
			System.clearProperty("escape.dev.spawnLevel");
		}
	}
}
