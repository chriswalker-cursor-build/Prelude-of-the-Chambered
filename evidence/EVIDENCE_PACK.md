# Evidence pack — Prelude of the Chambered modernisation

Programme: `PLAN.md` (accepted 2026-08-15) · ADR 0001 (modular monolith + Java 21) · ADR 0002 (visible play slices).
Train: `cursor/modernisation-train-3359` (one slice per commit). Recommendation at the end.

## 1. Artefact inventory

| Artefact | Where |
| --- | --- |
| Legacy map | `docs/architecture/LEGACY_MAP.md` |
| Accepted plan + ADRs | `PLAN.md`, `docs/adr/0001-*.md`, `docs/adr/0002-*.md` |
| Runbook | `RUNBOOK.md` |
| Verify command | `ant test` (headless, zero-dep harness; CI runs identical command on JDK 21) |
| Dual-run reports | `evidence/reports/{legacy,modern}/ant-test.txt`, `evidence/reports/SUMMARY.md` |
| Recordings (V1–V4) | attached to the train PR |

## 2. Slice ledger

| Commit | Slice | Kind | Tests after |
| --- | --- | --- | --- |
| `e79be01` | S0 harness + CI + AGENTS/BUGBOT | infra | 2 |
| `f8c8292` | S1 movement characterisation | tests only | 14 |
| `098ffd9` | S2 level-load characterisation | tests only | 23 |
| `eaa6064` | S3 release=21, applet deleted, constructor reflection | structure | 23 |
| `047308a` | V1 integer-fill presenter (`escape.present.mode`) | behaviour, flagged | 29 |
| `7e49175` | V2 lerp mouse-look (`escape.look.mode`) | behaviour, flagged | 35 |
| `0dfabc9` | V3 torch radius (`escape.light.mode`) | behaviour, flagged | 39 |
| `d06ef97` | V4 hotbar/health HUD (`escape.hud.mode`) | behaviour, flagged | 44 |
| `8b9f003` | S4 package boundaries (app/input/session/world/sim/render/ui/audio) | moves only | 44 |

Gate compliance: `render/Screen.java` (née `gui/Screen.java`) was first edited in V4, after S1 (`f8c8292`) and S2 (`098ffd9`) existed — the hard gate from the accepted plan.

## 3. Behaviour locked (characterisation, flag-off)

Movement: spawn (26,27) rot π+0.4; 60-tick walk/turn goldens; in-JVM bit determinism; rotSpeed 0.05, damping 0.4; friction 0.6; radius 0.3; ice axis-lock ±0.08 vs skates; water/flippers; dead-input freeze + 120-tick lose. Level load: six PNGs + names; 20-entry colour→block table; alpha→id; cache identity; OOB solid wall; full switchLevel graph with pauseTime 30; ladder arrival. Legacy panel bytes verified identical in default HUD mode.

## 4. Flags (all default 2011)

| Property | Default | New behaviour |
| --- | --- | --- |
| `escape.present.mode` | `fixed4` | `integerFill` — window-filling integer scale, letterboxed |
| `escape.look.mode` | `keyboard` | `mouseLerp` — eased mouse look, keyboard intact |
| `escape.light.mode` | `depthFog` | `torchRadius` — light pools at torches, fog elsewhere |
| `escape.hud.mode` | `legacyPanel` | `hotbar` — window-resolution HUD after blit |

Kill switch: launch without properties. All four at once:
`java -Descape.present.mode=integerFill -Descape.look.mode=mouseLerp -Descape.light.mode=torchRadius -Descape.hud.mode=hotbar -jar dist/PoC.jar`

## 5. Dual-run

See `evidence/reports/SUMMARY.md`: baseline 23/23 and modern 44/44 with **zero drift** on the shared subset, identical command both sides.

## 6. Review + CI

- CI: GitHub Actions `ant test` on JDK 21 (required-check wiring is a repo-settings action for the owner).
- Bugbot / Security Reviewer: run on the train PR per org Automations; `.cursor/BUGBOT.md` carries the migration-specific rules. No secrets, no new dependencies, no network at build time.

## 7. Rollback

- Any slice: revert its single commit on the train (no cross-slice file overlap except `EscapeComponent`/`Screen` noted in their messages).
- Behaviour: flags off restores 2011 output (characterisation-verified).
- Whole train: do not merge PR; `master` remains untouched 2011 code + docs.

## 8. Residual risks

- Golden doubles use 1e-6 epsilons for trig accumulation; exotic JVMs could still differ — CI on Temurin 21 is the reference.
- V2 relies on `java.awt.Robot` for pointer re-centering; without it (rare), mouse-look degrades at window edges but keyboard is unaffected.
- Torch STRENGTH/RADIUS (220 / 3.5 tiles) are tuning constants; a human may want to taste-test before flipping `escape.light.mode` default.

## 9. Recommendation

**Go.** Merge the train, keep flags at 2011 defaults, and flip defaults per-flag after playing the recordings' scenarios manually. Suggested next (not in this train): make `Cursor Bugbot`/CI required checks on `master`; consider flipping `escape.present.mode` first — it is the least contentious.
