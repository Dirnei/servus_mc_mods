# Tasks

## 1. Plan and rules

- [x] 1.1 `PLAN.md`: Servus: Notes in the mod table and repository layout, vision section 2.7, the dependency sentence (Notes depends on no Servus mod), "Open: M16 Notes" with `m16-notes-lists`, `m16-notes-sharing`, `m16-notes-goals`, `m16-notes-hud` and the later points (board block, Project Crate, Shuttles stock counts, JEI recipe import, op inspect command); verify no em dash with a grep
- [x] 1.2 `CLAUDE.md` Stack and dependency rule with the Notes exception, `openspec/config.yaml` context; verify `openspec validate m16-notes-project`

## 2. Gradle project

- [x] 2.1 `settings.gradle`, `notes/gradle.properties`, `notes/build.gradle` (no Foundation, `loadedMods = [ownMod]`), `notes/src/main/templates/META-INF/neoforge.mods.toml`, `allServusMods` in the root `build.gradle`; mod class `ServusNotes` and `ServusNotesIds` in `io.github.dirnei.mc.servus.notes`; verify `gradlew build`
- [x] 2.2 `.github/workflows/build.yml`, `.github/workflows/release.yml` (`notes-v*`, `ID_NOTES`, no dependency), `server/pack.ps1`, `README.md`; verify `gradlew build`

## 3. Notebook

- [x] 3.1 `tools/notes-textures/gen.py` writes the notebook sprites to `notes/src/main/resources/assets/servus_notes/textures/gui/sprites/notebook/`; verify by viewing the PNGs
- [x] 3.2 `client/NotesKeys` (category, Open Notebook key, tick handler) and `client/NotebookScreen` (book, contents heading, "No lists yet", tabs strip, no pause, key closes); verify `gradlew build`
- [x] 3.3 `datagen/NotesDataGen`, `datagen/NotesLanguageProvider` (key, category, screen texts); run `gradlew :notes:runData`, commit `src/generated/resources`; verify `gradlew build`

## 4. GameTest

- [x] 4.1 `data/servus_notes/structure/empty_12x6x12.nbt`, `gametest/NotesGameTests` (own registration) and a smoke test, registered from the mod class; verify `gradlew :notes:runGameTestServer` and `gradlew build test`

## 5. Final

- [x] 5.1 Full gradle build, tests green, `:notes:runClient` launches with Notes alone and `clientAll` with the whole family; the key opens and closes the empty book
- [x] 5.2 Play check by the user: Mods screen lists "Servus: Notes", the key is under "Servus: Notes" in Key Binds, the book looks like the mockup
