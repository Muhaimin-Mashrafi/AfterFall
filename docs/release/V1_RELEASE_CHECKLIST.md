# AfterFall V1 Release Checklist (M13 Phase 7)

| Item | Status |
|---|---|
| Feature freeze respected (no gameplay changes after Phase 1) | Done |
| Full suite green on PostgreSQL 17 (`.\mvnw.cmd test`, expect 314 tests) | Run on the presentation machine and tick here: [ ] |
| Event persistence defect fixed (events stored under run id; regression test) | Done (PR 1) |
| App starts offline with the README steps, PostgreSQL running | Verified in a fresh clone (Linux, PostgreSQL 16) |
| App starts with no database via `inmemory` profile | Done, covered by `InMemoryModeStartupTest` |
| README, demo script and UML match the code | Checked 2026-11-25; fixes in this PR |
| No secrets in the working tree | DB password removed from `DEMO_SCRIPT.md`; see note below |
| Three spoken rehearsals | [ ] Abdullah (`docs/demo/REHEARSAL_PACK.md`) |
| Fallback video recorded and copied to USB | [ ] Abdullah (`docs/demo/FALLBACK_VIDEO_PLAN.md`) |
| Merge to main, then tag `v1.0.0` | Proposed, not done: `git tag -a v1.0.0 -m "AfterFall V1"` after merge |

Note: a real local PostgreSQL password was committed earlier in `DEMO_SCRIPT.md` (it remains in git history). If that password is used anywhere else, change it.
