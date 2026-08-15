package com.mojang.escape.movement;

import com.mojang.escape.session.Game;
import com.mojang.escape.sim.Item;
import com.mojang.escape.sim.Player;
import com.mojang.escape.world.Level;
import com.mojang.escape.world.block.Block;
import com.mojang.escape.world.block.WaterBlock;
import com.mojang.escape.ui.LoseMenu;
import com.mojang.escape.support.Check;
import com.mojang.escape.support.Worlds;

/**
 * Characterisation of 2011 movement behaviour (LEGACY_MAP §7). These values
 * are locked on purpose — including the odd ones. Do not "fix" them.
 * Golden doubles use a small epsilon because trig accumulation may differ
 * by ulps across JVM builds; determinism within one JVM is asserted exactly.
 */
public final class MovementCharacterisationTest {

	private static Game freshGame() {
		Game game = new Game();
		game.newGame();
		return game;
	}

	private static void tick(Player p, boolean up, boolean down, boolean left, boolean right, boolean tl, boolean tr) {
		p.tick(up, down, left, right, tl, tr);
	}

	public static void testSpawnPositionAndRotation() {
		Game game = freshGame();
		Check.equal("The Prison", game.level.name, "start level name");
		Check.equal(26, game.level.xSpawn, "xSpawn");
		Check.equal(27, game.level.ySpawn, "ySpawn");
		Check.close(Math.PI + 0.4, game.player.rot, 1e-12, "spawn rot");
		Check.close(26.0, game.player.x, 1e-12, "spawn x");
		Check.close(27.0, game.player.z, 1e-12, "spawn z");
	}

	public static void testWalkForward60TicksGolden() {
		Game game = freshGame();
		for (int i = 0; i < 60; i++) tick(game.player, true, false, false, false, false, false);
		Check.close(24.805459234947136, game.player.x, 1e-6, "x after 60 forward ticks");
		Check.close(25.804912836703355, game.player.z, 1e-6, "z after 60 forward ticks");
	}

	public static void testWalkIsDeterministicWithinJvm() {
		Game a = freshGame();
		for (int i = 0; i < 60; i++) tick(a.player, true, false, false, false, false, false);
		Game b = freshGame();
		for (int i = 0; i < 60; i++) tick(b.player, true, false, false, false, false, false);
		Check.equal(Double.doubleToLongBits(a.player.x), Double.doubleToLongBits(b.player.x), "x bits identical");
		Check.equal(Double.doubleToLongBits(a.player.z), Double.doubleToLongBits(b.player.z), "z bits identical");
	}

	public static void testTurnLeftSingleTickAddsRotSpeed() {
		Game game = freshGame();
		double before = game.player.rot;
		tick(game.player, false, false, false, false, true, false);
		Check.close(before + 0.05, game.player.rot, 1e-12, "rotSpeed 0.05 applied then damped");
		Check.close(0.05 * 0.4, game.player.rota, 1e-12, "rota damping factor 0.4");
	}

	public static void testTurn60TicksGolden() {
		Game game = freshGame();
		for (int i = 0; i < 60; i++) tick(game.player, false, false, false, false, true, false);
		Check.close(8.486037098034235, game.player.rot, 1e-6, "rot after 60 turn ticks");
	}

	public static void testPlainFloorAccelerationAndFriction() {
		Level level = Worlds.bordered(8, 8, Worlds.PLAIN);
		Player p = Worlds.placePlayer(level, 3.5, 3.5, 0.0);
		tick(p, true, false, false, false, false, false);
		// rot=0, forward: za = -(zm=-1)*cos(0)*0.03 = +0.03 before friction
		Check.close(3.5 + 0.03, p.z, 1e-9, "one-tick forward step 0.03");
		Check.close(0.0, p.x - 3.5, 1e-12, "no lateral drift");
		Check.close(0.03 * 0.6, p.za, 1e-12, "default friction 0.6");
		Check.close(0.6, new Block().getFriction(p), 1e-12, "Block.getFriction");
	}

	public static void testCollisionNeverEntersSolidBlocks() {
		Level level = Worlds.bordered(8, 8, Worlds.PLAIN);
		Player p = Worlds.placePlayer(level, 3.5, 3.5, 0.0);
		for (int i = 0; i < 200; i++) {
			tick(p, true, false, false, false, false, false);
			assertOutsideWalls(level, p, i);
		}
		Check.isTrue(p.z < 7.0 && p.z > 0.0, "player stays inside the room");
	}

	private static void assertOutsideWalls(Level level, Player p, int tickNo) {
		double r = p.r;
		int x0 = (int) Math.floor(p.x + 0.5 - r);
		int x1 = (int) Math.floor(p.x + 0.5 + r);
		int z0 = (int) Math.floor(p.z + 0.5 - r);
		int z1 = (int) Math.floor(p.z + 0.5 + r);
		Check.isTrue(!level.getBlock(x0, z0).blocks(p), "corner NW free at tick " + tickNo);
		Check.isTrue(!level.getBlock(x1, z0).blocks(p), "corner NE free at tick " + tickNo);
		Check.isTrue(!level.getBlock(x0, z1).blocks(p), "corner SW free at tick " + tickNo);
		Check.isTrue(!level.getBlock(x1, z1).blocks(p), "corner SE free at tick " + tickNo);
	}

	public static void testPlayerRadiusIsPointThree() {
		Check.close(0.3, new Player().r, 1e-12, "player radius");
	}

	public static void testIceAxisLockWithoutSkates() {
		Level level = Worlds.bordered(11, 11, Worlds.ICE);
		Player p = Worlds.placePlayer(level, 5.0, 5.0, 0.0);
		p.xa = 0.05;
		p.za = 0.01;
		tick(p, false, false, false, false, false, false);
		Check.close(0.08, p.xa, 1e-12, "dominant axis locked to +0.08");
		Check.close(0.0, p.za, 1e-12, "other axis zeroed");
	}

	public static void testIceWithSkatesDoesNotSlide() {
		Level level = Worlds.bordered(11, 11, Worlds.ICE);
		Player p = Worlds.placePlayer(level, 5.0, 5.0, 0.0);
		p.items[p.selectedSlot] = Item.skates;
		p.xa = 0.05;
		p.za = 0.01;
		tick(p, false, false, false, false, false, false);
		Check.close(0.05 * 0.98, p.xa, 1e-12, "skates friction 0.98 on x");
		Check.close(0.01 * 0.98, p.za, 1e-12, "skates friction 0.98 on z");
	}

	public static void testWaterRules() {
		WaterBlock water = new WaterBlock();
		Player p = new Player();
		Check.isTrue(water.blocks(p), "water blocks player without flippers");
		p.items[p.selectedSlot] = Item.flippers;
		Check.isTrue(!water.blocks(p), "flippers allow swimming");
		Check.close(-0.5, water.getFloorHeight(p), 1e-12, "water floor height");
		Check.close(0.4, water.getWalkSpeed(p), 1e-12, "water walk speed");
	}

	public static void testDeadPlayerIgnoresInputThenLoses() {
		Game game = freshGame();
		Player p = game.player;
		double x = p.x, z = p.z;
		p.dead = true;
		for (int i = 0; i < 122; i++) tick(p, true, false, false, false, true, false);
		Check.close(x, p.x, 1e-12, "dead player x frozen");
		Check.close(z, p.z, 1e-12, "dead player z frozen");
		Check.instanceOf(LoseMenu.class, game.menu, "lose menu after 120 dead ticks");
	}
}
