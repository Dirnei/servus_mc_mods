## Context

See proposal.md. The root `build.gradle` holds the shared ModDevGradle setup for every subproject (runs `client`, `server`, `gameTestServer`, `data`, the `neoforge.mods.toml` expansion) and the combined runs over `allServusMods`. Every content mod so far compiles against Foundation and loads `[ownMod, foundationMod]`; `:delicacies` is the newest template. Notes is the first mod that must not.

## Goals / Non-Goals

**Goals:** a `:notes` project that builds and loads alone, a key that opens the notebook in the approved book look, the project wired into CI, release and pack, and the documents saying Notes is the Foundation-free member of the family.

**Non-Goals:** any list content; see proposal.md, Non-goals.

## Decisions

### Project wiring without Foundation
`notes/build.gradle` has no Foundation dependency and sets `loadedMods = [ownMod]`. `notes/gradle.properties`: `mod_id=servus_notes`, `mod_name=Servus: Notes`, `mod_description=In-game todo lists for yourself, your team and your server.`, `mod_version=0.1.0`. The mods.toml template is the Delicacies one without the Foundation block. `allServusMods` gains `notes`, so the combined runs load it too.

### Rule exception
CLAUDE.md and `openspec/config.yaml` say "Every mod depends on servus_foundation". They get: "except Servus: Notes, which depends on no Servus mod so any pack can use it". PLAN.md's dependency sentence says the same. Decided in the explore of 2026-11-02 for reach: a todo list is useful in any pack.

### Key mapping
`client/NotesKeys` registers on `RegisterKeyMappingsEvent` (mod bus, client): a category `new KeyMapping.Category(Identifier.fromNamespaceAndPath("servus_notes", "main"))` through `event.registerCategory`, and `KeyMapping("key.servus_notes.open_notebook", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY)`. `ClientTickEvent.Post` drains `consumeClick()` and opens the screen when `minecraft.screen == null`. Verified in `neoforge-26.1.2.116-sources.jar` (`RegisterKeyMappingsEvent#registerCategory`, `KeyMapping.Category` record, `KeyConflictContext`). Category label key: `key.category.servus_notes.main`.

### Notebook screen
`client/NotebookScreen extends Screen`, `isPauseScreen() == false`. It closes on Escape (vanilla) and on the open key when no `EditBox` has focus (`keyPressed` checks `NotesKeys.OPEN.matches(...)`). The book is drawn from GUI sprites (`assets/servus_notes/textures/gui/sprites/notebook/*.png`: `cover` nine-slice, `page_left` 124 x 186, `page_right` 188 x 186 with ruled lines from y 28 every 12 pixels, and per colour a nine-slice tab `tab_<colour>` (dimmed) and `tab_<colour>_active`) through `GuiGraphicsExtractor#blitSprite`. The screen is sized to the GUI scale with a fixed book of 336 x 208 GUI pixels, centred; the tabs stand up to 15 pixels above it, behind the cover. Everything is drawn in book coordinates through the pose stack, and `bookX`/`bookY` map the mouse into them. The tabs (`client/NotebookTab`: Mine red, Shared green, Team blue, Server gold) are all shown in this change with Mine active; a click only moves the active tab, the content comes with `m16-notes-lists` and `m16-notes-sharing`. `isInGameUi()` is true, so the world shows behind a light dim like a container screen. The text uses the vanilla font (web fonts of the mockup do not exist in game); colours follow the mockup (ink `#3b2a1a`, faded ink `#8a6a40`, paper `#f1e4c3`).

### Texture generator
`tools/notes-textures/gen.py` draws the sprites with the family's own PNG writer (as in `tools/delicacies-textures/gen.py`), no Pillow: leather with a stitched inner border, paper with a page fold shadow and ruled lines, tabs in red `#a8322a`, green `#3f7f3a`, blue `#2f5aa0`, gold `#b5842f`.

### Datagen
`datagen/NotesDataGen` on `GatherDataEvent.Client` with a `LanguageProvider` for `en_us`. No items, blocks or models.

### Own GameTest registration
Notes cannot call Foundation's `ServusGameTests`. `gametest/NotesGameTests` registers its few test functions directly: a `DeferredRegister` for `Registries.TEST_FUNCTION` and `RegisterGameTestsEvent#registerEnvironment` / `registerTest` with `FunctionGameTestInstance`, the same calls as `ServusGameTests` without the annotation scan. The `empty_12x6x12` structure is copied into `data/servus_notes/structure/`. A smoke test checks the mod is loaded and that no other Servus mod is required (`ModList` lists `servus_notes`).

### CI, release, pack
`build.yml` uploads `notes/build/libs/*.jar`. `release.yml` adds `notes-v*`, `ID_NOTES` and no required dependency. `server/pack.ps1` adds `notes`.

### Settles from PLAN.md
None open in PLAN.md; it writes the M16 plan from the explore of 2026-11-02.

## Pure logic vs Minecraft

No pure logic yet, so no JUnit tests; the project has no test sources yet. GameTest: smoke. Manual: key opens and closes the book in the client.

## Risks / Trade-offs

- [The combined runs load every mod; a Notes bug that touches another mod would hide] -> the `:notes` runs load Notes alone and are the ones used for its checks.
- [Book drawn from sprites at a fixed size may not fit at GUI scale 4 on small windows] -> the screen falls back to the largest scale that fits by drawing at a smaller pose scale.

## Migration Plan

None. Nothing is released.

### Decided during the play check
- The book is no longer a fixed 336 x 208. Pure `client.BookLayout.fit(screenWidth, screenHeight, roomRight)` lays it out in GUI pixels: the screen minus an 8 px margin, clamped to 336 x 208 at least and 576 x 328 at most, with the tabs 15 px above; only a window smaller than the minimum scales it down. The left page takes two fifths of the paper. JUnit `BookLayoutTest`.
- `page_left` and `page_right` become 64 x 64 nine-slice sprites (border 4, 24 on the spine side for the fold shadow); the ruled lines are drawn by the notebook where rows are, not baked into the page. The hover icon sprites (`icon_*`) are gone.
- In the UI a list is called a project (lang texts only; code, payloads and saved data keep "list"). The notebook has three levels: Projects (left page) to a project's tasks (left page) to one task (right page); opening and closing a project turns both pages (`NotebookMotion.flipBoth`), opening another task turns the right page (`flipRight`).
- The screen is split into `ProjectsPage`, `TaskListPage`, `TaskPage`, `ItemPickerPopup`, `SharePopup`, `NotebookMotion`, `PaperButton` and `PageHits`; every page reads its places from the frame's `BookLayout`.
