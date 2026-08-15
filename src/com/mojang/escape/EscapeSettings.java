package com.mojang.escape;

/**
 * Feature flags for the modernisation slices (ADR 0002). Read once from
 * system properties; defaults preserve 2011 behaviour. Humans flip defaults
 * at sign-off — code must not.
 */
public final class EscapeSettings {
	public enum PresentMode {
		FIXED4, INTEGER_FILL
	}

	private EscapeSettings() {
	}

	public static PresentMode presentMode() {
		return "integerFill".equals(System.getProperty("escape.present.mode"))
				? PresentMode.INTEGER_FILL
				: PresentMode.FIXED4;
	}
}
