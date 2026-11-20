
# AfterFall — Antigravity Development Instructions

## 1. Source of truth

Before every task, read:
- CLAUDE.md
- PROGRESS.md

The canonical specification is:
docs/blueprint/AfterFall-Blueprint-Revised-Solo.md

Technical constraints:
docs/architecture/TECHNICAL-CLARIFICATIONS.md

Follow Revision 2 (Revised Solo Blueprint). Never revert to Revision 1.

CLAUDE.md contains the detailed architecture, scope and coding rules. Follow it.

## 2. Current project state

- M0: Completed, tested and pushed to GitHub.
- M1 Session 1: Implemented, awaiting four targeted corrections.
- M1 Session 2: Not started.
- Application uses Java 21 and Spring Boot 4.1.2.
- Build system: Maven Wrapper.
- Database: PostgreSQL 17, integration planned for M9.

Preserve all existing M1 source files and tests.

## 3. Development rules

- Work on exactly one requested task.
- Inspect existing code before editing.
- Never overwrite existing work unnecessarily.
- Never implement future milestones without approval.
- Prefer small, focused changes.
- Run relevant Maven tests after modifications.
- Report exact test results.
- Update PROGRESS.md accurately.
- Stop after completing the requested task.

## 4. Architecture restrictions

- Domain and simulation core must remain independent of Spring and JPA.
- Prefer composition over unnecessary inheritance.
- Apply SOLID principles meaningfully.
- Preserve deterministic concurrency requirements.
- Do not introduce unnecessary dependencies.
- Follow the approved package architecture.

## 5. Git and security

- Never commit or push without explicit approval.
- Never delete existing project files without approval.
- Never use destructive Git or database commands without approval.
- Never expose credentials, tokens or passwords.
- Do not modify the canonical blueprint or technical clarification files unless explicitly requested.

## 6. Completion report

After each task, report:
1. Files changed.
2. Design decisions.
3. Tests executed and results.
4. Remaining issues.
5. Recommended next task.

Keep reports concise and suitable for an AOOP university project.

Stop after the assigned task.
