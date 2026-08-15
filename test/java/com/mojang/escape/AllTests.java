package com.mojang.escape;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Test entrypoint for `ant test`. Discovers public static no-arg methods
 * named test* on the registered classes, runs them in name order, and exits
 * nonzero on any failure. Register new characterisation classes here.
 */
public final class AllTests {
	private static final Class<?>[] TEST_CLASSES = {
			com.mojang.escape.SmokeTest.class,
			com.mojang.escape.movement.MovementCharacterisationTest.class,
			com.mojang.escape.world.LevelLoadCharacterisationTest.class,
			com.mojang.escape.present.PresenterTest.class,
			com.mojang.escape.look.MouseLookTest.class,
			com.mojang.escape.light.TorchLightTest.class,
			com.mojang.escape.hud.HudTest.class,
	};

	public static void main(String[] args) {
		int passed = 0;
		List<String> failures = new ArrayList<String>();

		for (Class<?> testClass : TEST_CLASSES) {
			List<Method> tests = new ArrayList<Method>();
			for (Method m : testClass.getDeclaredMethods()) {
				if (m.getName().startsWith("test") && m.getParameterCount() == 0 && Modifier.isStatic(m.getModifiers())) {
					tests.add(m);
				}
			}
			tests.sort(Comparator.comparing(Method::getName));

			for (Method test : tests) {
				String name = testClass.getSimpleName() + "." + test.getName();
				try {
					test.invoke(null);
					passed++;
					System.out.println("PASS " + name);
				} catch (Throwable t) {
					Throwable cause = t.getCause() != null ? t.getCause() : t;
					failures.add(name + ": " + cause);
					System.out.println("FAIL " + name + ": " + cause);
				}
			}
		}

		System.out.println(passed + " passed, " + failures.size() + " failed");
		if (!failures.isEmpty()) {
			System.exit(1);
		}
	}
}
