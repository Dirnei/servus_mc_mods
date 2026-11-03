## Why

Players in a pack keep wishing for an in-game todo list they can keep for themselves and share with their team or the whole server (explore 2026-11-02). It becomes a new mod, Servus: Notes, and the first change gives it a project to land in, the notebook it opens and the documentation that it is the one family member without a Foundation dependency (PLAN.md, new milestone M16 Notes, `m16-notes-project`).

## What Changes

- New Gradle project `:notes` for Servus: Notes, mod id `servus_notes`, package `io.github.dirnei.mc.servus.notes`, version 0.1.0, wired like `:delicacies` but **without** Foundation: no `compileOnly project(':foundation')`, the runs load only the Notes mod, and `neoforge.mods.toml` names only NeoForge and Minecraft.
- Mod class, an `en_us` lang file through datagen, and a client key mapping "Open Notebook" (default N, own key category "Servus: Notes").
- The notebook screen: a leather book with two paper pages, a contents page on the left, a list page on the right and coloured paper tabs on the top edge of the right page, as approved in the mockup `temp/notes-notebook.html`. In this change it is empty ("No lists yet"); lists arrive with `m16-notes-lists`.
- The book's GUI sprites come from a generator script `tools/notes-textures/gen.py`.
- An own GameTest registration (Notes cannot use Foundation's helper) with a smoke test.
- CI uploads the Notes jar, the Release workflow accepts `notes-v*` tags with no required CurseForge dependency, the play test pack script packs the jar, README gets a row.
- `PLAN.md`: Servus: Notes in the mod table and repository layout, a vision section, the dependency sentence (Notes depends on nothing in the family), and "Open: M16 Notes" with the four following changes and the later points (board block, Project Crate, Shuttles stock counts, JEI recipe import, ops inspect command).
- `CLAUDE.md` and `openspec/config.yaml`: the new project and mod id, and the exception that Notes does not depend on Foundation.

## Capabilities

### New Capabilities
- `notes/mod-setup`: Servus: Notes loads on its own, without any other Servus mod, and names its content in English.
- `notes/notebook`: a key opens the notebook screen in the book look from anywhere, without an item.

### Modified Capabilities
None. `foundation/mod-family-loading` describes the mods built on Foundation; Notes states its own loading rule in `notes/mod-setup` (and `m14-delicacies-project`, still open, modifies that spec).

## Non-goals

- Lists, tasks and saving (`m16-notes-lists`).
- Scopes, FTB Teams, guests and rights (`m16-notes-sharing`).
- Item goals, coordinates, beams and JEI (`m16-notes-goals`).
- HUD pin and toasts (`m16-notes-hud`).
- A docs site page and a branding logo.

## Impact

- New: `notes/` (build files, sources, generated resources, GameTest structure), `tools/notes-textures/gen.py`.
- Changed: `settings.gradle`, root `build.gradle` (`allServusMods`), `.github/workflows/build.yml`, `.github/workflows/release.yml`, `server/pack.ps1`, `README.md`, `PLAN.md`, `CLAUDE.md`, `openspec/config.yaml`.
- No source changes in Foundation, Alchemy, Shuttles, Delicacies or Brewery.
