package com.mojang.escape.support;

/**
 * Zero-dependency assertions. Maven Central is not reachable from the Cloud
 * Agent VM, and CI must run the identical command, so the harness is plain
 * Java on the Ant classpath instead of JUnit.
 */
public final class Check {
	private Check() {
	}

	public static void isTrue(boolean condition, String what) {
		if (!condition) throw new AssertionError(what);
	}

	public static void equal(Object expected, Object actual, String what) {
		boolean ok = expected == null ? actual == null : expected.equals(actual);
		if (!ok) throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
	}

	public static void equal(long expected, long actual, String what) {
		if (expected != actual) throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
	}

	public static void close(double expected, double actual, double eps, String what) {
		if (Double.isNaN(actual) || Math.abs(expected - actual) > eps) {
			throw new AssertionError(what + ": expected <" + expected + "> +/-" + eps + " but was <" + actual + ">");
		}
	}

	public static void same(Object expected, Object actual, String what) {
		if (expected != actual) throw new AssertionError(what + ": expected same instance");
	}

	public static void notSame(Object unexpected, Object actual, String what) {
		if (unexpected == actual) throw new AssertionError(what + ": expected different instances");
	}

	public static void instanceOf(Class<?> type, Object actual, String what) {
		if (!type.isInstance(actual)) {
			throw new AssertionError(what + ": expected instance of " + type.getSimpleName()
					+ " but was " + (actual == null ? "null" : actual.getClass().getSimpleName()));
		}
	}
}
