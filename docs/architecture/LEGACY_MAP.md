# Legacy map — Prelude of the Chambered

Status: discovery complete (docs only). No production code changed.

Source: Notch’s 2011 Ludum Dare 48h game. Java 6-era Swing/AWT software raycaster. ~3.4k lines across 61 `.java` files. Entry: `ant run` → `com.mojang.escape.EscapeComponent`.

## 1. Process tree and entrypoints

| Process | Entrypoint | Notes |
| --- | --- | --- |
| Desktop JAR | `EscapeComponent.main` | `JFrame` + `Canvas`, 60 Hz tick, render-on-tick |
| Applet (dead path) | `EscapeApplet` | `java.applet.Applet` — compiles on JDK 21 with `[removal]` warning; gone on newer JDKs |

Runtime loop (`EscapeComponent.run`):

1. Accumulate real time; tick at `1/60s`.
2. `game.tick(inputHandler.keys)` only while focused.
3. `screen.render(game, hasFocus)` into a 160×120 `int[]`.
4. Copy to `BufferedImage`, `drawImage` scaled by `SCALE=4` (window **640×480**, `setResizable(false)`).

There is no server, no database, no network. All “data” is classpath PNGs and WAVs.

## 2. Package map (as shipped)

```
com.mojang.escape
├── EscapeComponent   # window, loop, blit
├── EscapeApplet      # unused in ant run
├── Game              # session, input mapping, level switch
├── InputHandler      # keys only; mouse listeners are empty
├── Art / Sound       # static classpath loaders
├── entities/         # Player, enemies, bullets, Item enum
├── gui/              # Bitmap, Bitmap3D (raycaster), Screen (HUD+compose), sprites
├── level/            # Level + six *Level subclasses
├── level/block/      # ~20 block types, colour-decoded from PNG
└── menu/             # Title/Pause/Win/Lose/About/Instructions/GotLoot
```

Hottest coupling (extraction risk, with paths):

1. `gui/Screen.java` — composes 3D viewport, hurt overlay, 29px panel HUD, menus, focus prompt. **Do not touch until movement + level-load characterisation lands.**
2. `gui/Bitmap3D.java` — wall/floor/sprite rasteriser + `postProcess` distance fog. Lighting is **not** torch-aware.
3. `entities/Player.java` + `entities/Entity.java` — movement, ice axis-lock, collision, inventory.
4. `level/Level.java` — PNG decode, colour→block factory, spawn, entity cache, `Class.forName` + `newInstance()`.
5. `EscapeComponent.java` — fixed internal resolution and integer `SCALE`.

## 3. Binaries, manifests, verify command

| Artefact | Path | Role |
| --- | --- | --- |
| Build | `build.xml` | Ant: `compile` → `jar` → `run` / `clean` |
| Main-Class | `com.mojang.escape.EscapeComponent` | JAR manifest |
| Resources | `res/{gui,level,snd,tex}` | Copied onto classpath at compile |
| Lockfile | none | No Maven/Gradle; no third-party jars |
| Tests | **none** | No `test/` tree, no JUnit, no CI |
| CI | **none** | No `.github/workflows` |

**Verify command today:** `ant compile` (this Cloud Agent image: OpenJDK 21.0.10 + Ant 1.10.14 — **BUILD SUCCESSFUL**, 15 warnings).

**Verify command to introduce (post-plan):** `ant test` (fast four: compile + unit/characterisation). Recorded smoke is `ant run` plus a 30s computer-use clip per visible slice.

Onboarding diagnostic (framework §13):

| Question | Answer |
| --- | --- |
| What does a developer run before push? | `ant run` at best. CI does not exist. Agents cannot check behaviour until we add tests. |
| One verify command? | No. First high-value win after ADR approval. |
| How long is the suite? | 0s — there isn’t one. Target &lt; 2 min headless. |
| Languages? | Java only. |
| Registry? | Public Maven Central for JUnit 5 once the test target exists. |

## 4. Data stores and connectivity

- **Levels:** `res/level/{start,overworld,dungeon,crypt,temple,ice}.png`. RGB = block type; alpha channel encodes trigger `id` (`id = 255 - alpha`).
- **Textures:** `res/tex/{walls,floors,sprites,font,gamepanel,items,sky}.png` via `Art.loadBitmap` (4-bit crush + magenta colorkey).
- **Audio:** `res/snd/*.wav` via `javax.sound.sampled`. `Sound.play()` already swallows failures (headless-safe enough for characterisation).
- **Persistence:** none. `Level.loaded` is an in-memory `Map<String,Level>` so revisiting a floor restores entities/block state.

Level graph (`switchLevel`):

```
start (The Prison)
  id 1 → overworld spawn 1
  id 2 → dungeon spawn 1
overworld (The Island)
  id 1 → start spawn 1
  id 2 → crypt spawn 1
  id 3 → temple spawn 1
  id 5 → ice spawn 1
dungeon ↔ start (id 1 → start spawn 2)
crypt  → overworld spawn 2
temple → overworld spawn 3
ice    → overworld spawn 5
```

Colour → block (from `Level.getBlock(x,y,col)`):

| RGB | Block |
| --- | --- |
| `0xFFFF00` | spawn (decorate), otherwise floor |
| `0xFFFFFF` / `0x93FF9B` | `SolidBlock` |
| `0x009300` | `PitBlock` |
| `0x00FFFF` | `VanishBlock` |
| `0xFFFF64` | `ChestBlock` |
| `0x0000FF` | `WaterBlock` |
| `0xFF3A02` | `TorchBlock` (sprite only) |
| `0x4C4C4C` | `BarsBlock` |
| `0xFF66FF` / `0x9E009E` | `LadderBlock` down/up |
| `0xC1C14D` | `LootBlock` |
| `0xC6C6C6` / `0xC6C697` | door / locked door |
| `0x00FFA7` | `SwitchBlock` |
| `0x009380` | `PressurePlateBlock` |
| `0xff0005` / `0x3F3F60` | `IceBlock` |
| `0xFFBA02` | `AltarBlock` |
| `0x749327` | `SpiritWallBlock` |
| `0x00C2A7` | `FinalUnlockBlock` |
| `0x000056` | `WinBlock` |
| `0xAA5500` | boulder entity |
| `0xff0000`–`0xff0007` | bat / bosses / ogre / eye / ghost |

## 5. Existing test layout and CI

Empty. Characterisation must be invented. ImageIO PNG load works headless; AWT `Canvas`/`JFrame` does not belong in unit tests.

## 6. How the game actually plays (seams for the visible slices)

The dungeon is dark **on purpose**. Three systems change play, not chrome:

### 6.1 Framebuffer and scale

- Internal: `WIDTH=160`, `HEIGHT=120` (`EscapeComponent`).
- Viewport in `Screen`: `height - PANEL_HEIGHT` (29px HUD strip).
- Present: `g.drawImage(img, 0, 0, 640, 480, null)` — nearest-neighbour integer scale of **4**, window locked.

Filling the screen is a **presenter** change (`EscapeComponent`), not a raycaster change. Non-integer stretch would smear the software renderer and change aim.

### 6.2 Look / camera

- Turn: `Player.tick` `rotSpeed = 0.05`, integrated into `rot` with `rota *= 0.4` damping.
- Keyboard: Q/E or arrows (unless strafe).
- `InputHandler.mouseMoved` / `mouseDragged` are **no-ops**. There is no mouse-look.
- Camera in `Bitmap3D.render`: `rot = game.player.rot` every frame — **no lerp**. Bob is `sin(bobPhase * 0.4) * 0.01 * bob`.

Lerp mouse-look is an **input + player.rot** change. Snapping `rot` to mouse delta would feel like a different game; lerp is the play-feel.

### 6.3 Lighting vs torches

`Bitmap3D.postProcess`:

```
brightness = (300 - zl * 6 * (xx² * 2 + 1)) quantized to 16
```

Distance fog on the z-buffer. `TorchBlock` only adds a flickering sprite (`tex` 3/4) and **does not contribute light**. Caves read as soup because fog, not because torches failed.

Real torch radius is a **lighting model** change in `postProcess` (plus a query of torch world positions). Raising global brightness is an anti-pattern — it erases the designed dark.

### 6.4 HUD

`Screen.render` draws `Art.panel` at `y = height - 29`, then keys / loot / health **numbers** and an 8-slot item row. After integer-scaling the whole 160×120 buffer, that HUD is just bigger mosaic. A recording-grade hotbar/health bar almost certainly means **screen-space HUD after blit** (see ADR 0002).

## 7. Movement and level-load behaviour to freeze

Must be characterised **before** `Screen.java` (and before any lighting/HUD PR that could hide physics bugs).

### Movement (`Player.tick` + `Entity.move`)

| Behaviour | Current rule |
| --- | --- |
| Radius | `Player.r = 0.3` |
| Walk | `walkSpeed = 0.03 * block.getWalkSpeed` |
| Turn | `rotSpeed = 0.05`; `rota *= 0.4` after `rot += rota` |
| Friction | default `0.6` |
| Gravity / floor | `ya -= 0.01`; approach floor with `* 0.2` when below |
| Ice (no skates) | axis-lock slide at `±0.08`; snap other axis toward tile centre `* 0.2`; friction `1` |
| Ice (skates) | walk `0.05`, friction `0.98` |
| Water | blocks player without flippers; floor `-0.5`; walk `0.4` |
| Collision | 4-corner block test + nearby entity `blocks()` |
| Dead | inputs zeroed; after 120 ticks `level.lose()` |
| `newGame` spawn | `Level.loadLevel(..., "start")`; `x/z` = yellow pixel; `rot = π + 0.4` |

### Level load (`Level.loadLevel` / `init` / `Game.switchLevel`)

| Behaviour | Current rule |
| --- | --- |
| Cache | second load of the same name returns the same instance |
| `clear()` | wipes cache (`newGame`) |
| Factory | `byName`: capitalize + `Class.forName("...level.XLevel").newInstance()` |
| Spawn | yellow `0xFFFF00`, or `findSpawn(id)` on matching `LadderBlock` |
| Switch | `pauseTime = 30`; remove player; load; offset `0.2` along facing; `LadderBlock.wait = true` |
| Dungeon init | `trigger(6,true)` and `trigger(7,true)` on first init |

## 8. Suggested vertical slices (non-overlapping paths)

Path ownership for Cloud Agents after the human gate. **Do not spawn conversion agents until characterisation PRs merge.**

| ID | Slice | Allowlist | Parallel with |
| --- | --- | --- | --- |
| S0 | Verify command + CI + `AGENTS.md` | `build.xml`, `.github/**`, `AGENTS.md`, `.cursor/**`, `test/support/**` | serial first |
| S1 | Characterise movement | `test/java/com/mojang/escape/movement/**` (+ minimal harness only) | S2 |
| S2 | Characterise level load | `test/java/com/mojang/escape/level/**` | S1 |
| S3 | Java 21 platform hygiene | `build.xml`, `EscapeApplet.java` (delete), `Level.java` (`byName` only) | after S1+S2 |
| S4 | Package boundaries (modular monolith) | moves under `src/com/mojang/escape/**` — **serial shepherd** | after S3 |
| V1 | Integer-scale presenter | `EscapeComponent.java`, new `app/` presenter type | after S1+S2 |
| V2 | Lerp mouse-look | `InputHandler.java`, `Game.java`, `Player.java` | after S1 |
| V3 | Torch radius lighting | `Bitmap3D.java`, `TorchBlock.java`, new light query type | after S1+S2 |
| V4 | Hotbar / health HUD | `gui/Screen.java` (+ new HUD type). **Blocked on S1+S2.** | after V1 |

Merge-train branch: `refactor/integration`. One conflict/CI agent when files collide. Humans merge.

## 9. What not to do

- Do not rewrite in TypeScript / web / LWJGL “while we’re here”.
- Do not mix a package move with mouse-look or HUD pixels in one PR.
- Do not brighten `postProcess` globally and call it torches.
- Do not touch `Screen.java` until S1 and S2 are green on CI.
