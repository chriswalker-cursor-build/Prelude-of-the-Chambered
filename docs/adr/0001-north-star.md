# ADR 0001 — North star: modular monolith + Java 21

- Status: **proposed** (awaiting human approval)
- Date: 2026-08-14
- Context: `docs/architecture/LEGACY_MAP.md`, `PLAN.md`

## Decision

Modernise Prelude of the Chambered as a **modular monolith** on **Java 21**, keeping the software raycaster and a single `ant run` JAR.

We will **not**:

- rewrite in TypeScript, JavaScript, or any other language;
- extract microservices;
- replace the software renderer with OpenGL/Vulkan/LWJGL in this programme;
- mix product play-feel changes into structural PRs.

Visible play slices (scale, mouse-look, torches, HUD) are **flagged behaviour PRs** after characterisation — see ADR 0002.

## Why this north star

The domain is one 48-hour game (~3.4k LOC, 61 types, no network, no database). Service extraction has no bounded operational context. A language migration would throw away characterisation and the designed darkness/feel.

Java 21 is already the Cloud Agent / CI-shaped runtime: `ant compile` succeeds today (Applet `[removal]` and `Class.newInstance()` deprecation are the platform leftovers). Pinning 21 is an honest upgrade from the Java 6 *source era*, not a heroic port.

Modular monolith means **package boundaries and dependency direction** inside one deployable, so later Cloud Agents can own non-overlapping allowlists.

## Coexistence and data

- Pattern: **branch by abstraction** behind `EscapeSettings` system properties (defaults = 2011 behaviour).
- Data: none to migrate. Level PNGs stay canonical.
- Rollback: flags off, or revert the slice on `refactor/integration`.

## Consequences

- First conversion after approval is tests + `ant test` CI, not `Screen.java`.
- `EscapeApplet` can die in the platform slice (`ant run` never used it).
- A later JDK 25 bump is a separate platform PR once 21 + tests are boring.

## Alternatives considered

| Alternative | Reason rejected |
| --- | --- |
| TypeScript rewrite | User constraint; framework “language migration” risk |
| LWJGL / new engine | Changes lighting, scale, and camera at once — unreviewable |
| Stay on unpinned javac | Agents and CI drift; Applet explodes on newer JDKs |
| Multi-module Maven from day one | Process theatre for 3.4k lines |
