# AfterFall — Visual Direction
## Monogatari-Inspired Art Direction

**Status:** Approved direction  
**Scope:** Game presentation and visual design  
**Implementation target:** M10–M13 (frontend and presentation integration)  
**Reference:** The visual language of the *Monogatari Series* and SHAFT's experimental direction  
**Important:** This is an artistic reference, not a request to reproduce copyrighted assets, characters, scenes, logos, or exact layouts.

---

## 1. Core Direction

AfterFall should feel like a survival simulation presented through a surreal, psychologically charged, highly stylized visual language.

The game must retain its own identity. Use Monogatari as the primary visual inspiration—not as a template to copy.

The simulation, rules, data, and player decisions remain clear and functional. The presentation may be unconventional, dramatic, abstract, and cinematic, but it must never make important gameplay information difficult to understand.

## 2. Visual Principles

### 2.1 Colour

Use a restrained palette built around:
- **Crimson red:** emphasis, danger, critical events, selected states.
- **Near-black:** primary backgrounds and dramatic contrast.
- **Warm ivory / off-white:** primary text and quiet surfaces.
- **Muted greys:** secondary information and inactive UI.
- **Occasional accent colours:** only when they communicate a distinct gameplay state or narrative meaning.

Do not make every screen uniformly red and black. Use colour deliberately, with negative space and contrast.

### 2.2 Typography

- Use bold, high-impact typography for titles, chapter cards, event names, and major state changes.
- Use clean, highly readable typography for gameplay data, controls, logs, and long-form text.
- Allow oversized text, vertical arrangements, cropped lettering, and unconventional alignment in non-critical presentation areas.
- Never sacrifice legibility for stylistic effect.
- Establish a consistent type hierarchy for headings, labels, values, alerts, and body text.

### 2.3 Composition and Layout

- Prefer asymmetrical compositions over generic centred layouts when appropriate.
- Use strong negative space and deliberate visual imbalance.
- Use geometric blocks, thin rules, panels, and sharply defined divisions.
- Allow important events to temporarily take over the screen through cinematic compositions.
- Keep persistent gameplay controls and essential information in predictable, accessible locations.

### 2.4 Backgrounds and Imagery

- Use stark architecture, empty spaces, silhouettes, graphic patterns, symbolic imagery, and surreal environmental compositions.
- Use perspective and framing to create unease, isolation, scale, or tension.
- Keep backgrounds subordinate to gameplay information.
- Avoid generic anime decoration, excessive gradients, random particles, and visual clutter.
- Do not use copyrighted Monogatari screenshots, characters, logos, or traced compositions. Create original assets that evoke the broad design principles.

## 3. Motion, Transitions, and Effects

Use motion as a deliberate part of the visual identity:
- Sharp cuts and sudden graphic transitions for high-impact events.
- Controlled fades, wipes, slides, and brief flashes where they serve a clear purpose.
- Typography and geometric elements may animate into or out of the composition.
- Use restrained motion for routine interactions and stronger motion for rare, consequential events.
- Respect reduced-motion settings and avoid rapid flashing or strobing.

Animations must not delay essential controls, obscure simulation state, or interfere with input.

## 4. Application to AfterFall

Apply the art direction to the actual game interface, not only promotional material.

Priority areas:
1. Main menu and title presentation.
2. In-game HUD and simulation overview.
3. Event notifications and important encounters.
4. Character or entity introductions, where applicable.
5. Logs, status panels, and information screens.
6. Death, failure, restart, and end-state screens.
7. Settings, pause, and navigation screens.

### Gameplay readability rules

- Health, resources, time, status, objectives, and other critical values must remain immediately readable.
- Use consistent colour and icon semantics for gameplay states.
- Do not hide essential information inside decorative animation.
- Avoid unnecessary interaction steps.
- Maintain usable layouts at the project's supported window sizes and aspect ratios.
- Provide clear hover, focus, selected, disabled, and error states.

## 5. Design System Requirements

Before implementing multiple screens, establish reusable design tokens and components.

Define:
- Colour tokens.
- Typography scale and font roles.
- Spacing scale.
- Border, divider, and panel treatments.
- Button and input styles.
- Notification and alert variants.
- Transition and animation timings.
- Responsive layout rules.

Build reusable components rather than styling each screen independently. Keep presentation logic separate from simulation logic wherever the current architecture allows.

## 6. Technical and Project Constraints

These requirements are mandatory:
- Do not rewrite, weaken, or bypass the simulation engine to achieve a visual effect.
- Do not change gameplay rules, deterministic behaviour, data models, or simulation timing as part of visual work unless a separate task explicitly requires it.
- Do not introduce visual dependencies without checking project compatibility and bundle impact.
- Keep the application buildable and runnable after each implementation step.
- Follow the existing project architecture, naming conventions, and code style.
- Do not perform broad refactors while implementing the visual direction.
- Preserve existing functionality and tests.

## 7. Implementation Guidance

The visual direction is approved now, but implementation is scheduled for M10–M13.

Until those milestones:
- Treat this file as the source of truth for the intended visual identity.
- Do not start a premature full UI rewrite.
- Continue the existing engineering roadmap and milestone sequence.
- If a future implementation decision conflicts with this document, identify the conflict before changing the direction.

When visual implementation begins:
1. Inspect the existing frontend and identify its current rendering architecture.
2. Propose a small design-system foundation.
3. Implement one representative screen first.
4. Review it against this document before expanding to other screens.
5. Apply the system incrementally across the remaining screens.
6. Test functionality, readability, responsiveness, and performance.

## 8. Quality Bar

A screen is aligned with this direction when it:
- Feels cinematic, graphic, surreal, and psychologically expressive.
- Uses typography, contrast, composition, and motion intentionally.
- Looks like part of one coherent AfterFall design system.
- Remains readable and usable as a real game interface.
- Is original rather than a direct reproduction of Monogatari assets or layouts.

**Guiding principle:** Monogatari-inspired presentation; unmistakably AfterFall identity; uncompromised gameplay clarity.
