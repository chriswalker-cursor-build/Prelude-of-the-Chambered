package com.mojang.escape.world;

import com.mojang.escape.session.Game;
import com.mojang.escape.world.block.AltarBlock;
import com.mojang.escape.world.block.BarsBlock;
import com.mojang.escape.world.block.Block;
import com.mojang.escape.world.block.ChestBlock;
import com.mojang.escape.world.block.DoorBlock;
import com.mojang.escape.world.block.FinalUnlockBlock;
import com.mojang.escape.world.block.IceBlock;
import com.mojang.escape.world.block.LadderBlock;
import com.mojang.escape.world.block.LockedDoorBlock;
import com.mojang.escape.world.block.LootBlock;
import com.mojang.escape.world.block.PitBlock;
import com.mojang.escape.world.block.PressurePlateBlock;
import com.mojang.escape.world.block.SolidBlock;
import com.mojang.escape.world.block.SpiritWallBlock;
import com.mojang.escape.world.block.SwitchBlock;
import com.mojang.escape.world.block.TorchBlock;
import com.mojang.escape.world.block.VanishBlock;
import com.mojang.escape.world.block.WaterBlock;
import com.mojang.escape.world.block.WinBlock;
import com.mojang.escape.support.Check;
import com.mojang.escape.support.Worlds;

/**
 * Characterisation of level loading (LEGACY_MAP §4/§7): PNG decode, colour →
 * block factory, id decode from alpha, cache identity, spawn discovery, and
 * the switchLevel graph. Locks 2011 behaviour — do not "fix".
 */
public final class LevelLoadCharacterisationTest {

	/** Test seam into the protected colour → block factory. */
	private static final class FactoryProbe extends Level {
		Block probe(int col) {
			return getBlock(0, 0, col);
		}
	}

	public static void testAllSixLevelsLoadWithNames() {
		Game game = new Game();
		game.newGame();
		String[][] expected = {
				{ "start", "The Prison" },
				{ "overworld", "The Island" },
				{ "dungeon", "The Dungeons" },
				{ "crypt", "The Crypt" },
				{ "temple", "The Temple" },
				{ "ice", "The Frost Cave" },
		};
		for (String[] pair : expected) {
			Level level = Level.loadLevel(game, pair[0]);
			Check.equal(pair[1], level.name, pair[0] + " display name");
			Check.isTrue(level.width > 0 && level.height > 0, pair[0] + " has dimensions");
		}
	}

	public static void testByNameReflectionConstructsSubclasses() {
		Game game = new Game();
		game.newGame();
		Check.instanceOf(StartLevel.class, Level.loadLevel(game, "start"), "start class");
		Check.instanceOf(OverworldLevel.class, Level.loadLevel(game, "overworld"), "overworld class");
		Check.instanceOf(DungeonLevel.class, Level.loadLevel(game, "dungeon"), "dungeon class");
	}

	public static void testCacheReturnsSameInstanceUntilCleared() {
		Game game = new Game();
		game.newGame();
		Level first = Level.loadLevel(game, "crypt");
		Level second = Level.loadLevel(game, "crypt");
		Check.same(first, second, "cache identity");
		Level.clear();
		Level third = Level.loadLevel(game, "crypt");
		Check.notSame(first, third, "clear() evicts");
	}

	public static void testStartSpawnFromYellowPixel() {
		Game game = new Game();
		game.newGame();
		Check.equal(26, game.level.xSpawn, "start xSpawn");
		Check.equal(27, game.level.ySpawn, "start ySpawn");
	}

	public static void testColourToBlockFactoryTable() {
		FactoryProbe probe = new FactoryProbe();
		Check.instanceOf(SolidBlock.class, probe.probe(0xFFFFFF), "white solid");
		Check.instanceOf(SolidBlock.class, probe.probe(0x93FF9B), "hedge solid");
		Check.instanceOf(PitBlock.class, probe.probe(0x009300), "pit");
		Check.instanceOf(VanishBlock.class, probe.probe(0x00FFFF), "vanish");
		Check.instanceOf(ChestBlock.class, probe.probe(0xFFFF64), "chest");
		Check.instanceOf(WaterBlock.class, probe.probe(0x0000FF), "water");
		Check.instanceOf(TorchBlock.class, probe.probe(0xFF3A02), "torch");
		Check.instanceOf(BarsBlock.class, probe.probe(0x4C4C4C), "bars");
		Check.instanceOf(LadderBlock.class, probe.probe(0xFF66FF), "ladder down");
		Check.instanceOf(LadderBlock.class, probe.probe(0x9E009E), "ladder up");
		Check.instanceOf(LootBlock.class, probe.probe(0xC1C14D), "loot");
		Check.instanceOf(DoorBlock.class, probe.probe(0xC6C6C6), "door");
		Check.instanceOf(SwitchBlock.class, probe.probe(0x00FFA7), "switch");
		Check.instanceOf(PressurePlateBlock.class, probe.probe(0x009380), "pressure plate");
		Check.instanceOf(IceBlock.class, probe.probe(0x3F3F60), "ice");
		Check.instanceOf(LockedDoorBlock.class, probe.probe(0xC6C697), "locked door");
		Check.instanceOf(AltarBlock.class, probe.probe(0xFFBA02), "altar");
		Check.instanceOf(SpiritWallBlock.class, probe.probe(0x749327), "spirit wall");
		Check.instanceOf(FinalUnlockBlock.class, probe.probe(0x00C2A7), "final unlock");
		Check.instanceOf(WinBlock.class, probe.probe(0x000056), "win");
		Check.equal(Block.class, probe.probe(0x123456).getClass(), "unknown colour is plain Block");
	}

	public static void testBlockIdDecodesFromAlpha() {
		int w = 4, h = 4;
		int[] pixels = new int[w * h];
		for (int i = 0; i < pixels.length; i++) pixels[i] = 0xFF000000 | Worlds.PLAIN;
		// alpha 0xFE → id = 255 - 254 = 1
		pixels[1 + 1 * w] = 0xFE000000 | Worlds.PLAIN;
		Game game = new Game();
		game.player = new com.mojang.escape.sim.Player();
		Worlds.SyntheticLevel level = new Worlds.SyntheticLevel();
		level.init(game, "synthetic", w, h, pixels);
		Check.equal(1, level.getBlock(1, 1).id, "id from alpha channel");
		Check.equal(0, level.getBlock(0, 0).id, "opaque pixel id 0");
	}

	public static void testOutOfBoundsIsSolidWall() {
		Level level = Worlds.bordered(4, 4, Worlds.PLAIN);
		Check.instanceOf(SolidBlock.class, level.getBlock(-1, 0), "west OOB");
		Check.instanceOf(SolidBlock.class, level.getBlock(0, -1), "north OOB");
		Check.instanceOf(SolidBlock.class, level.getBlock(4, 0), "east OOB");
		Check.instanceOf(SolidBlock.class, level.getBlock(0, 4), "south OOB");
	}

	public static void testSwitchLevelGraphAndPause() {
		Game game = new Game();
		game.newGame();

		game.level.switchLevel(1);
		Check.equal("The Island", game.level.name, "start id1 -> overworld");
		Check.equal(30, game.pauseTime, "pauseTime on switch");

		game.pauseTime = 0;
		game.level.switchLevel(2);
		Check.equal("The Crypt", game.level.name, "overworld id2 -> crypt");

		game.level.switchLevel(1);
		Check.equal("The Island", game.level.name, "crypt id1 -> overworld");

		game.level.switchLevel(3);
		Check.equal("The Temple", game.level.name, "overworld id3 -> temple");

		game.level.switchLevel(1);
		Check.equal("The Island", game.level.name, "temple id1 -> overworld");

		game.level.switchLevel(5);
		Check.equal("The Frost Cave", game.level.name, "overworld id5 -> ice");

		game.level.switchLevel(1);
		Check.equal("The Island", game.level.name, "ice id1 -> overworld");

		game.level.switchLevel(1);
		Check.equal("The Prison", game.level.name, "overworld id1 -> start");

		game.level.switchLevel(2);
		Check.equal("The Dungeons", game.level.name, "start id2 -> dungeon");

		game.level.switchLevel(1);
		Check.equal("The Prison", game.level.name, "dungeon id1 -> start");
	}

	public static void testSwitchArrivalSpawnsOnMatchingLadder() {
		Game game = new Game();
		game.newGame();
		game.level.switchLevel(1);
		Level overworld = game.level;
		Block spawnBlock = overworld.getBlock(overworld.xSpawn, overworld.ySpawn);
		Check.instanceOf(LadderBlock.class, spawnBlock, "arrival tile is the ladder");
		Check.isTrue(Math.abs(game.player.x - overworld.xSpawn) <= 0.25, "player near ladder x");
		Check.isTrue(Math.abs(game.player.z - overworld.ySpawn) <= 0.25, "player near ladder z");
	}
}
