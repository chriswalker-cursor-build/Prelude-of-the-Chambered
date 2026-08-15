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

	public enum LookMode {
		KEYBOARD, MOUSE_LERP
	}

	public static LookMode lookMode() {
		return "mouseLerp".equals(System.getProperty("escape.look.mode"))
				? LookMode.MOUSE_LERP
				: LookMode.KEYBOARD;
	}

	public enum LightMode {
		DEPTH_FOG, TORCH_RADIUS
	}

	public static LightMode lightMode() {
		return "torchRadius".equals(System.getProperty("escape.light.mode"))
				? LightMode.TORCH_RADIUS
				: LightMode.DEPTH_FOG;
	}

	public enum HudMode {
		LEGACY_PANEL, HOTBAR
	}

	public static HudMode hudMode() {
		return "hotbar".equals(System.getProperty("escape.hud.mode"))
				? HudMode.HOTBAR
				: HudMode.LEGACY_PANEL;
	}
}
