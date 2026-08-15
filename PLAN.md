# PLAN.md — Agentic modernisation of Prelude of the Chambered

Status: **awaiting_human_approval**  
North star: **modular monolith + Java platform upgrade** (not a language rewrite).  
Companion: `docs/architecture/LEGACY_MAP.md`, `docs/adr/0001-north-star.md`, `docs/adr/0002-visible-play-slices.md`, `RUNBOOK.md`.  
Framework: `modernisation-framework.html` (Plan & Design before conversion).

This document is the conversion contract. Cloud Agents must not edit production game code until a human accepts this plan and the ADRs.

## 0. Outcome

Keep Notch’s 2011 raycaster as one Java deployable. Make it a codebase agents can change safely, then land four play-feel slices that are obvious on a 30-second recording:

1. Integer-scale the framebuffer so it **fills the window/screen** (chunky pixels, no blur).
2. **Lerp mouse-look** (camera eases; keyboard look remains).
3. **Real torch radius** so caves go from soup to readable near torches, still dark between them.
4. **Hotbar / health HUD** drawn so inventory and HP read at a glance.

**Hard sequencing:** characterise **movement** and **level load** before anyone touches `src/com/mojang/escape/gui/Screen.java`.

## 1. Options (Plan Mode style)

| Option | What it is | Upside | Downside | Verdict |
| --- | --- | --- | --- | --- |
| A. TypeScript / web port | Canvas/WebGL rewrite | Easy to demo in a browser | Highest-risk stack migration; forbidden | Out |
| B. Service extraction | Split sim vs render processes | None — there is no ops domain | Invented microservices | Out |
| C. Big-bang “modern Java game” | One agent, LWJGL + ECS + new renderer | Looks impressive in a slide | Unreviewable; destroys characterisation | Out |
| D. Modular monolith + JDK 21, same renderer | Packages + seams + tests; Swing blit stays | Matches size (~3.4k LOC); dual-run is real | Visible slices are still product PRs | **Recommended** |
| E. Platform-only (no play slices) | JDK hygiene, drop applet | Safer | Misses the recording north star | Incomplete |

Recommended path: **D**. Architecture and runtime first as *structure*; the four recording slices as *flagged behaviour* on top of characterisation.

Java LTS choice: **21**. This Cloud Agent image already compiles the tree (`ant compile` green, 15 warnings). Java 25 can be a later, separate platform PR after dual-run is boring.

## 2. Target shape (modular monolith)

Same JAR, same `ant run` (or `ant test && ant run`). Packages become bounded contexts with one-way dependencies:

```
app        →  session, input, render, audio
session    →  world, sim, ui
input      →  (JDK only)
world      →  sim (entities), audio
sim        →  world (collision queries)
render     →  session (read-only view), world (tiles), sim (sprites)
ui         →  render (Bitmap), session
audio      →  (JDK only; no-op under tests)
```

Start by *drawing* the boundaries (package moves, no behaviour) after tests exist. Do not introduce Maven modules or JPMS `module-info.java` in the pilot — one Ant (or later Gradle) project is enough.

Coexistence pattern: **branch by abstraction** (framework §8). Each visible slice adds a seam and a default-off flag. Legacy path stays until a human flips the default.

There is **no data cutover**. Level PNGs remain the source of truth. Expand/contract only if we extract the colour→block table to a named palette (add table + dual-read, then delete the switch).

## 3. Feature flags and kill switches

Local game: system properties (and a tiny `EscapeSettings` reader). Defaults preserve 2011 behaviour.

| Property | Legacy (default) | New | Owner slice |
| --- | --- | --- | --- |
| `escape.present.mode` | `fixed4` | `integerFill` | V1 |
| `escape.look.mode` | `keyboard` | `mouseLerp` | V2 |
| `escape.light.mode` | `depthFog` | `torchRadius` | V3 |
| `escape.hud.mode` | `legacyPanel` | `hotbar` | V4 |

Kill switch: omit the property or set legacy. No production traffic, so “rollback” is restart without flags, or revert the slice commit on `refactor/integration`.

Do not delete the legacy branch of a seam until the recording is accepted **and** characterisation + dual-run stay green with the new default.

## 4. Verification ladder (what we must build)

Today the ladder is missing rungs. Introduce them in this order:

| Rung | Command | When |
| --- | --- | --- |
| Compile + `-Xlint` | `ant compile` | every agent write loop |
| Unit + characterisation | `ant test` | every write loop / CI |
| Dual-run | same `ant test` on `LEGACY_SHA` vs `HEAD` | after each conversion PR |
| Headless PNG load | part of `ant test` | S2 |
| Recorded smoke (30s) | computer-use on `ant run` | each V-slice PR |
| Bugbot + Security Reviewer | Automations on PR | every conversion PR |
| Performance | fps line already printed; compare 60 Hz keep-up at integer-fill | V1 and V3 |

Formatter/linter: add `spotless` or `google-java-format` only in S0 if it can be made a no-op on untouched files; do not reformat the whole Notch tree in a behaviour PR.

**PR definition of done** (from the framework): `ant test` green locally; CI green; characterisation for kept behaviour; no drive-by files; Bugbot + Security Reviewer have run. Humans merge.

## 5. Phase plan (Cloud Agents)

State machine (orchestrator never writes production code):

```
discover ✓ → plan_design (this PR) → HUMAN GATE
  → S0 env/CI → S1+S2 test_gen (parallel) → test_exec_baseline
  → S3 platform
  → V1 → V2 → V3 → V4   (merge order; V1∥V3 allowed after S1+S2 if paths stay disjoint)
  → S4 packages (serial, last structural slice)
  → dual-run → bugbot → security → validation → HUMAN SIGNOFF
```

S4 runs **after** the visible slices: every V allowlist names today's paths, and a package move first would leave the orchestrator enforcing stale allowlists while adding nothing to the recordings. Once V4 merges, S4 is a moves-only PR over a fully characterised tree.

### 5.1 S0 — Cloud Agent environment and verify command (serial)

**Goal:** a hello agent can open a trivial PR that stays green.

- Pin JDK 21 in CI (GitHub Actions: `actions/setup-java`).
- Add JUnit 5 to the Ant classpath (Ivy or checked-in test jars — prefer Ivy/`maven-ant-tasks` so we get a lock-like resolution log).
- `ant test` runs headless (`-Djava.awt.headless=true`).
- Write `AGENTS.md` (verify command, path rules, “no Screen.java until S1+S2”).
- Write `.cursor/BUGBOT.md` (no drive-by reformat; tests required; do not “fix” Notch bugs during characterisation).
- Optional: `.cursor/environment.json` so Cloud Agents install Ant/JDK the same way as CI.

Allowlist: `build.xml`, `.github/**`, `AGENTS.md`, `.cursor/**`, `test/lib/**` or Ivy files. **No `src/`.**

### 5.2 S1 — Characterise movement (tests-only)

Allowlist: `test/java/com/mojang/escape/movement/**` plus *minimal* harness if Sound/Random block tests.

Lock (including oddities — do not “fix” ice physics):

- Walk/strafe/turn deltas over N ticks on a solid floor (`walkSpeed`, `rotSpeed`, friction `0.6`).
- Collision: cannot walk into `SolidBlock`; radius `0.3`.
- Ice without skates: axis-lock at `±0.08`.
- Ice with skates: slow walk, high friction.
- Water: blocked without flippers; `getFloorHeight == -0.5` with flippers.
- `newGame`: spawn on yellow pixel of `start.png`; `rot == Math.PI + 0.4`.
- Dead: movement ignored; lose after 120 ticks (can stub `Game.lose`).

Do not touch `Player.java` — no test seams are needed. A headless spike (JDK 21, `-Djava.awt.headless=true`) confirmed `Game.newGame()` + `player.tick(...)` runs without a display, spawn is exactly `π + 0.4`, and a 60-tick walk is bit-identical across runs. Test through public `tick` + a loaded `StartLevel`.

### 5.3 S2 — Characterise level load (tests-only, parallel with S1)

Allowlist: `test/java/com/mojang/escape/level/**`.

Lock:

- All six PNGs load; width/height; `name` strings.
- Colour→block class for a fixture pixel per type (table in LEGACY_MAP).
- Cache: `loadLevel` twice returns the same instance (spike-verified headless); `Level.clear()` then a new instance. Note `newGame()` itself calls `Level.clear()` — test cache identity via direct `loadLevel` calls.
- `switchLevel` graph (string + spawn id) via a table test on each `*Level.switchLevel` (inject a fake `Game` if needed — if that requires production edits, wait and use a tiny package-visible setter listed in the PR).
- Dungeon `init` fires triggers 6 and 7.
- `byName` reflection still constructs `StartLevel` from `"start"`.

**S1 and S2 must merge before any `Screen.java` edit.** Orchestrator refuses V4 and any “drive-by HUD” until the runbook lists both SHAs.

### 5.4 S3 — Java platform hygiene (structure, no play change)

- `build.xml`: `source/target` or `--release 21`.
- Delete `EscapeApplet` (dead; blocks a future 25+ bump).
- Replace `Class.newInstance()` in `Level.byName` with `getDeclaredConstructor().newInstance()` — same behaviour, dual-run via S2 tests.
- Do not reformat the repo.

### 5.5 S4 — Modular monolith packages (structure, serial, **after V4**)

One conversion agent (or a shepherd). Moves only. Keep public types working; update tests’ imports. Runs last so the V-slice allowlists (which name today's paths) never go stale mid-flight.

Suggested packages (names can shift at implementation if imports stay consistent):

| Package | Types |
| --- | --- |
| `...app` | `EscapeComponent`, settings, presenter |
| `...input` | `InputHandler` |
| `...session` | `Game` |
| `...world` | `Level`, `*Level`, `level.block.*` |
| `...sim` | `entities.*` |
| `...render` | `gui/Bitmap*`, `Art`, sprites |
| `...ui` | `menu.*`, later HUD |
| `...audio` | `Sound` |

If a move collides with an in-flight V-slice, **stop** and serialise. Overlapping path ownership is an anti-pattern.

### 5.6 Visible slices (behaviour PRs — split from structure)

See ADR 0002 for exact acceptance and recording scripts. Merge order matches the 30-second story:

| ID | Slice | Intentional change | Flag default |
| --- | --- | --- | --- |
| V1 | Integer-scale presenter | Window resizable/fullscreen; scale = `min(w/160, h/120)` integer; letterbox; nearest-neighbour | `fixed4` |
| V2 | Lerp mouse-look | Mouse delta → target yaw; `rot = lerp(rot, target, α)` per tick; keyboard still works | `keyboard` |
| V3 | Torch radius | `postProcess` adds torch falloff in world XZ; fog remains away from torches | `depthFog` |
| V4 | Hotbar / health HUD | Screen-space hotbar + HP after blit; **`Screen.java` allowed only now** | `legacyPanel` |

V1 and V3 do not share files (`EscapeComponent` vs `Bitmap3D`/`TorchBlock`) and may run in parallel **after** S1+S2. V2 needs `EscapeComponent` too (mouse listeners attach there; relative capture needs `java.awt.Robot` re-centering at window level), so **V1 must merge before V2 spawns** — same file, serial by design. V4 waits for V1 so the HUD is judged at the new scale.

Each V-slice PR includes a 30s computer-use recording (walkthrough artefact) as evidence, not as a substitute for `ant test`.

## 6. Agent roster and parallelism

| Phase | Agents | Notes |
| --- | --- | --- |
| Scout / this plan | 1 | Done |
| S0 | 1 | Serial |
| S1 / S2 characterisation | 2 | Non-overlapping `test/` trees |
| S3 | 1 | Small |
| V1 ∥ V3 | 1–2 | After tests; disjoint allowlists |
| V2, V4 | 1 each | V2 after V1 (shares `EscapeComponent`); V4 after V1 |
| S4 packages | 1 | Serial shepherd, after V4 |
| Conflict / CI fix | 1 | Always serial |
| Dual-run / validation | 1 | Docs + `evidence/` only |
| Bugbot / Security | Automations | Beside CI, not instead |

Orchestrator updates `RUNBOOK.md` only. It refuses to spawn conversion if `human_gates_pending` is non-empty or dual-run drifts.

## 7. Prompt skeletons (copy into Cloud Agents)

Replace bracketed values. Always attach this PLAN + LEGACY_MAP.

### Orchestrator

```
You are the orchestration agent for Prelude of the Chambered modernisation.
You never edit production code. You only update RUNBOOK.md and spawn specialists.

State machine: plan_design → (human_gate) → S0 → S1+S2 → test_exec_baseline
→ S3 → V1 → V2 → V3 → V4 → S4 → test_exec_dual → bugbot → security → validation → human_signoff.

Rules:
- Refuse to advance if PLAN/ADRs are awaiting_human_approval.
- Refuse any agent whose allowlist includes gui/Screen.java until S1 and S2 SHAs are in the runbook.
- Spawn slice agents with non-overlapping path allowlists only.
- Conflict/CI fix: one agent at a time.
- Verify command is `ant test` once S0 has landed; until then `ant compile`.
- Do not rewrite in TypeScript. North star is modular monolith + Java 21.
```

### S1 movement characterisation

```
Add characterisation tests for player movement and collision capturing current behaviour
(including ice axis-lock and water/flippers). Cover Player.tick + Entity.move.
Do not “fix” production bugs in this PR.
Path allowlist: test/java/com/mojang/escape/movement/** and minimal harness only.
Do not edit gui/Screen.java.
Run `ant test` until green. PR = tests (+ minimal harness) only.
List behaviours locked in the PR description (table in LEGACY_MAP §7).
```

### S2 level-load characterisation

```
Add characterisation tests for Level.loadLevel, colour→block mapping, cache, spawn,
and the switchLevel graph. Do not “fix” production bugs.
Path allowlist: test/java/com/mojang/escape/level/** only.
Do not edit gui/Screen.java.
Run `ant test` until green. PR = tests only.
```

### V1 integer fill

```
Goal: integer-scale the 160×120 framebuffer so it fills the window (letterboxed),
nearest-neighbour, no non-integer stretch.
Branch: refactor/v1-integer-fill
Path allowlist: src/com/mojang/escape/EscapeComponent.java, new app presenter + tests ONLY.
Flag: escape.present.mode=integerFill (default remains fixed4).
After every edit: ant test.
Record a 30s computer-use clip: resize/fullscreen, pixels stay chunky, image fills the short axis.
Do not touch Screen.java, Bitmap3D, or Player.
```

### V2 lerp mouse-look

```
Goal: mouse delta turns the camera with lerp smoothing; keyboard look still works.
Path allowlist: InputHandler.java, EscapeComponent.java, Game.java, Player.java, matching tests.
Requires V1 merged first (shares EscapeComponent.java — serial, never parallel with V1).
Relative capture: accumulate deltas and re-center the pointer (java.awt.Robot) while focused.
Flag: escape.look.mode=mouseLerp (default keyboard).
Characterisation of keyboard rotSpeed must stay green.
Record 30s: mouse move → camera eases, not snaps.
```

### V3 torch radius

```
Goal: torches contribute a real light radius in Bitmap3D.postProcess.
Caves readable near TorchBlock; still dark between torches. Do not raise global brightness.
Path allowlist: Bitmap3D.java, TorchBlock.java, new light-query type, tests.
Flag: escape.light.mode=torchRadius (default depthFog).
Record 30s: walk a dark dungeon/crypt past wall torches — pools of light, soup between.
```

### V4 HUD

```
Goal: hotbar + health HUD that reads on the integer-scaled window.
Path allowlist: gui/Screen.java, new ui HUD type, tests.
ONLY legal because S1+S2 have merged.
Flag: escape.hud.mode=hotbar (default legacyPanel).
Record 30s: HP and selected item obvious without squinting.
Do not mix in renderer or movement refactors.
```

## 8. MCP / Automations (suggested, not required)

| Phase | Integration | Purpose |
| --- | --- | --- |
| Plan | GitHub | This PR |
| Verify | GitHub Checks + computer-use | `ant test`; 30s recordings |
| Review | Bugbot + Security Reviewer Automations | Every slice PR |
| Nightly | Vulnerability Scanner on `refactor/integration` | Once deps exist (JUnit) |

No Datadog/PagerDuty — there is no production service. Observability = fps stdout + dual-run summary.

Secrets: none expected. Do not add API keys for a local JAR.

## 9. Rollback

| Event | Action |
| --- | --- |
| Flagged slice feels wrong | Restart with legacy property; do not delete code yet |
| Dual-run drift | Orchestrator stops; one CI-fix agent; no new slices |
| S4 package move explodes | Revert that PR on the merge train; V-slices stay |
| JDK 21 compile break | `ant compile` is the gate; revert |
| Want 2011 behaviour forever | Leave all flags at defaults; S3 applet delete is the only hard removal — restore from git if a downstream JDK still needs it (none do for `ant run`) |

## 10. Acceptance criteria (human sign-off)

- [ ] ADRs 0001 and 0002 accepted.
- [ ] `ant test` exists and is the required CI check on `master` / merge train.
- [ ] S1+S2 merged; movement and level-load tables in LEGACY_MAP are locked.
- [ ] Dual-run `evidence/reports/SUMMARY.md` for legacy SHA vs integration SHA — no unexplained diffs.
- [ ] V1–V4 each have a 30s recording showing the slice (flags on).
- [ ] Defaults still 2011-like until a human chooses to flip them.
- [ ] No TypeScript, no new renderer, no service split.
- [ ] Bugbot + Security summaries attached to the validation pack.
- [ ] `evidence/EVIDENCE_PACK.md` with go/no-go.

## 11. Stop conditions (agents halt)

- Plan / ADR not accepted.
- Any task that edits `gui/Screen.java` before S1+S2 merge.
- Dual-run drift on locked movement or level-load behaviour.
- Overlapping path allowlists.
- Intentional behaviour mixed into a package-move PR — split first.
- Unresolved high-severity Bugbot/Security findings (org policy).
- Proposal to rewrite in another language.

## 12. What this PR is not

This PR is discovery + plan + ADRs + runbook only. It does not add tests, CI, or game code. Conversion agents wait.
