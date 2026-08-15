# ADR 0002 — Visible play slices and characterisation gate

- Status: **proposed** (awaiting human approval)
- Date: 2026-08-14
- Relates to: ADR 0001, `PLAN.md` §5.6

## Decision

Land four **intentional** play-feel changes as separate, flagged PRs. They are the 30-second recording story. They are **not** the same PRs as package moves or the JDK pin.

**Gate:** characterisation of **movement** and **level load** must merge before any edit to `src/com/mojang/escape/gui/Screen.java`. Lighting and presenter slices must not hide physics/load regressions.

## Slice design

### V1 — Integer-scale framebuffer (fills the screen)

- Keep internal resolution **160×120** (raycaster contract).
- Replace fixed `SCALE=4` blit with `scale = max(1, min(windowW/160, windowH/120))`.
- Nearest-neighbour only (`Graphics.drawImage` with integer dest size). Letterbox leftover pixels.
- Window becomes resizable (and optionally fullscreen). `fixed4` remains the default flag.

Rejected: stretching to non-integer sizes; raising internal resolution (changes FOV/fog and every characterisation of the framebuffer).

### V2 — Lerp mouse-look

- `InputHandler` currently ignores mouse motion. Capture dx while focused.
- Accumulate a target yaw; each tick `player.rot += α * wrap(target - rot)` (α tuned so a flick eases over several frames, not one).
- Keyboard Q/E/arrows keep working (`keyboard` default).
- Do not lerp position — only look. Bob stays as characterised.

Rejected: raw snap-to-mouse; mouselock without a lerp (feels like a different camera).

### V3 — Real torch radius

- Today: `Bitmap3D.postProcess` is distance fog; `TorchBlock` is a sprite with **zero** lighting contribution.
- New model: for each pixel with a valid z, reconstruct world XZ and add falloff from torch positions (linear or inverse-square, clamped). Fog still dominates away from torches.
- Dark caves become readable **in torch pools**, not globally.

Rejected: multiplying all `brightness` by a constant; baking light into wall textures.

### V4 — Hotbar / health HUD

- Today: 29px `Art.panel` inside the 160×120 buffer (keys, loot, HP **text**, 8 item slots).
- After V1, that panel is only a larger mosaic. Recording-grade HUD is **composited after** the integer blit (Java2D at window resolution): health bar + hotbar + selection.
- `legacyPanel` default keeps the 2011 strip.

Rejected: redrawing the 29px panel at 3× inside the low-res buffer and calling it done; editing `Screen.java` in the same PR as V3 lighting.

## Recording rubric (computer-use, ~30s each)

| Slice | Camera script | Passes if |
| --- | --- | --- |
| V1 | Launch with `integerFill`, drag window / fullscreen | Framebuffer fills the short axis; pixels stay chunky; black letterbox ok |
| V2 | New game, move mouse left/right, also tap Q | View eases; no single-frame snap; WASD still walks |
| V3 | Enter dungeon or crypt, walk past wall torches | Floors/walls readable within ~few tiles of a torch; between torches still dark |
| V4 | Same session after V1 | HP and selected slot readable without leaning in |

## Consequences

- Orchestrator allowlists `gui/Screen.java` only for V4, and only after S1+S2 SHAs are recorded in `RUNBOOK.md`.
- Each V-slice is a behaviour PR: characterisation of the *old* path stays green with the flag off; new tests cover the flag on.
- Humans flip defaults (or not) at sign-off — agents do not.
