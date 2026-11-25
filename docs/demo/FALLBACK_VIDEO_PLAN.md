# Fallback Video Plan (M13 Phase 6)

Purpose: a 7-minute backup recording used only if the live demo cannot run. You record it; this is the shot list.

## Recording setup
- Screen recorder: Xbox Game Bar (Win+G) or OBS, 1920x1080, 30 fps, system audio off, microphone on.
- Fresh start: stop the app, drop and recreate nothing; just start it and use a new run. Browser full-screen, bookmarks bar hidden.
- Record the voice live while following `DEMO_SCRIPT.md`. If a take goes wrong, redo that act and join the clips; do not edit the app state.
- Save as `demo-backup.mp4`, keep one copy on the laptop, one on USB, one in cloud storage. Do not commit the video to git.

## Shot list (matches `DEMO_SCRIPT.md` section 1.3; counter values from the default seed 42 run)
| Time | Shot | Screen | Cue |
|---|---|---|---|
| 0:00 | Title and premise, click `[ INITIATE OBSERVATION PROTOCOL ]` | `index.html` | none |
| 1:00 | `START`, then 8 `STEP TICK` clicks, point at four regions and the counter | `story.html` | counter 0 to 8 |
| 2:00 | Open a survivor dossier (25 cards) | `people.html` | none |
| 2:30 | `Step-To 44`, 3 clicks: Chapter II label and first Observer intervention | `story.html` | counter 47 |
| 3:00 | `Step-To 55`, 2 clicks: pill `PAUSED`; `Answer CHOICE_FAMINE 0`, `RESUME`, 1 click, `F5`: consequence caption | `story.html` | counter 57 |
| 3:40 | `Step-To 65`, 2 clicks: Chapter III label | `story.html` | counter 67 |
| 4:00 | Comparator table and one `INVESTIGATE` | `records.html` | tick 46 row |
| 4:40 | `Step-To 133`, 2 clicks, `Answer CHOICE_OUTBREAK 0`, `Step-To 175`, 4 clicks: deaths and Chapter IV | `story.html` | counter 135 then 179 |
| 5:10 | `Step-To 219`, 1 click, open `FINAL RECAP`, show tiles (4 / 25 alive), jump to the closing line | `records.html` | counter 220 |
| 5:30 | Architecture: `docs/uml/rendered/` tick sequence and component diagram, then the test summary in a terminal | UML and terminal | none |
| 6:30 | Closing line | `records.html` or `index.html` | none |

The PowerShell window with the helpers may be on screen, but nothing in it may reveal a password: set `AFTERFALL_DB_PASSWORD` before recording starts, in a window that is not captured, or record in `inmemory` mode.

## Checks before accepting the take
- [ ] Length 6:30 to 7:15, audio clear, no password or secret visible on screen (blur the terminal environment variables)
- [ ] Every act from the shot list present, played from start to finish with no cuts mid-action
- [ ] File plays on a second device
