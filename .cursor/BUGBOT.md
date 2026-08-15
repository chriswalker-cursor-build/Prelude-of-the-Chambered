# Review rules — Prelude of the Chambered modernisation

- Flag any PR that mixes structural refactoring (package moves, renames) with behaviour changes (gameplay, rendering, input). They must be split.
- Flag edits to `src/com/mojang/escape/gui/Screen.java` unless the PR is the V4 HUD slice and movement + level-load characterisation tests exist and pass.
- Flag changes to characterisation expectations (`test/java/**`) that alter locked 2011 values (walk/turn speeds, friction, ice slide `±0.08`, spawn rot `π + 0.4`, colour→block table) without an ADR reference in the PR description.
- Flag any new runtime dependency or download step: the build must stay offline-capable (`ant test` with no network).
- Flag flipped defaults of `escape.present.mode`, `escape.look.mode`, `escape.light.mode`, `escape.hud.mode` — defaults stay 2011 until human sign-off.
- Flag wholesale reformatting of Notch's source files.
- Tests are required for new behaviour: every flagged slice needs coverage of both the legacy default and the flag-on path where feasible headless.
