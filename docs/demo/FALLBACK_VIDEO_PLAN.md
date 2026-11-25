# Fallback Video Plan (M13 Phase 6)

Purpose: a 7-minute backup recording used only if the live demo cannot run. You record it; this is the shot list.

## Recording setup
- Screen recorder: Xbox Game Bar (Win+G) or OBS, 1920x1080, 30 fps, system audio off, microphone on.
- Fresh start: stop the app, drop and recreate nothing; just start it and use a new run. Browser full-screen, bookmarks bar hidden.
- Record the voice live while following `DEMO_SCRIPT.md`. If a take goes wrong, redo that act and join the clips; do not edit the app state.
- Save as `demo-backup.mp4`, keep one copy on the laptop, one on USB, one in cloud storage. Do not commit the video to git.

## Shot list (matches the script timeline)
| Time | Shot | Screen |
|---|---|---|
| 0:00 | Title and premise | `index.html` Opening |
| 1:00 | Start simulation, narrate captions, point at four regions and tick counter | `story.html` |
| 2:00 | Open a survivor dossier | `people.html` |
| 2:30 | Observer intervention and its fiction-versus-fact marking | `story.html` |
| 3:15 | Choice moment: pause, decision, resume, consequence caption | `story.html` |
| 4:00 | Discover one piece of evidence | `records.html` |
| 5:00 | Final recording and recap | `records.html` |
| 5:30 | Architecture: show `docs/uml/tick-sequence.mmd` and `component-diagram.mmd` rendered, then the test run summary in a terminal | UML and terminal |
| 6:30 | Closing line | `index.html` |

## Checks before accepting the take
- [ ] Length 6:30 to 7:15, audio clear, no password or secret visible on screen (blur the terminal environment variables)
- [ ] Every act from the shot list present, played from start to finish with no cuts mid-action
- [ ] File plays on a second device
