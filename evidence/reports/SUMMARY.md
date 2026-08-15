# Dual-run summary

Identical command on both refs: `ant clean test` (headless, OpenJDK 21.0.10, Ant 1.10.14, Cloud Agent VM).

| Ref | SHA | Result | Report |
| --- | --- | --- | --- |
| Characterisation baseline (tests exist, no conversion) | `098ffd9482a3d2f7ab10d9d9c559c046f07da56e` | 23 passed, 0 failed | `legacy/ant-test.txt` |
| Modern (S3 + V1–V4 + S4) | train HEAD at dual-run time (`8b9f003`) | 44 passed, 0 failed | `modern/ant-test.txt` |

Drift check: every PASS/FAIL line from the baseline run appears verbatim in the modern run (`comm -23` over sorted test outcomes = **0 lines**). The 21 additional modern tests are the V1–V4 slice suites added on the train.

The 2011 source ref (`ec62c8c`) predates the harness, so it cannot execute the suite; the baseline above is the first commit where the locked behaviours are executable, which is the earliest honest "legacy" side. Flag-off behaviour on the modern ref is additionally locked by the characterisation subset itself (movement goldens, level-load table, legacy panel bytes).

Verdict: **no dual-run drift**. Locked 2011 behaviour is intact with all flags at defaults.
