# AGENTS.md — Prelude of the Chambered

Modernisation programme: modular monolith + Java 21. Governing docs: `PLAN.md`, `docs/adr/0001-north-star.md`, `docs/adr/0002-visible-play-slices.md`, `RUNBOOK.md`.

## Verify command

```
ant test
```

Runs compile (`-Xlint`) plus the characterisation/unit suite headless. It must be green before any commit. CI (`.github/workflows/ci.yml`) runs the identical command on JDK 21.

Run the game: `ant run`. Fresh build: `ant clean test`.

## Test harness

Zero-dependency on purpose — Maven Central is not reachable from the Cloud Agent VM, and the agent loop and CI must run the same command. Tests are plain Java in `test/java/**`:

- public static no-arg methods named `test*`
- assertions via `com.mojang.escape.support.Check`
- register new classes in `com.mojang.escape.AllTests`
- everything must pass with `-Djava.awt.headless=true` (no `JFrame`, no `Canvas` in tests)

## Hard rules

1. Do not rewrite in another language, framework, or renderer. North star is ADR 0001.
2. Characterisation locks behaviour. Never "fix" 2011 quirks (ice axis-lock, friction, spawn rot `π + 0.4`) in a test or structural PR.
3. Play-feel changes ship behind system-property flags with 2011 defaults (`escape.present.mode`, `escape.look.mode`, `escape.light.mode`, `escape.hud.mode`). Do not flip defaults. `escape.dev.spawnLevel=<name>` is a dev/test utility that boots straight into a named level for demos and manual testing.
4. Split behaviour from structure — a package move and a gameplay change never share a commit.
5. No drive-by reformatting of Notch's code. Match surrounding style (tabs, brace placement).
6. Respect the path allowlists in `RUNBOOK.md` for whichever slice you are executing.
7. Level PNGs in `res/level/` are the source of truth. The colour→block table lives in `docs/architecture/LEGACY_MAP.md`; keep it accurate if you touch `Level.getBlock`.

## Cursor Cloud specific instructions

- `.cursor/environment.json` installs Ant and pre-builds the jar.
- The VM has `DISPLAY=:1` for manual GUI testing and recordings; unit tests must not need it.
- Outbound network to package registries is blocked — do not add dependencies that require downloads at build time.
