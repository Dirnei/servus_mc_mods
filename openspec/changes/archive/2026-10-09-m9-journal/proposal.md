# Proposal: m9-journal

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 3: `m9-journal`.

## Why

PLAN.md 2.1 makes Paracelsus' journal the tech tree: no Research Lab and no packs, progress comes from doing things, and the story of his life is told chapter by chapter. After `m9-tria-prima` the Research Lab is left without techs; this change replaces it.

## What Changes

- **Modonomicon** becomes a required dependency of Manufacturing (`com.klikli_dev:modonomicon-1.21.1-neoforge`, Maven at `dl.cloudsmith.io/public/klikli-dev/mods/maven`).
- **Paracelsus' Journal**: a Modonomicon book `servus_manufacturing:journal`, generated with Modonomicon's datagen API. One entry per chapter of his life with the story, the chapter's tasks and what the chapter unlocks. A chapter's entry stays locked until the team finished the chapter before it.
- **Chapters and tasks are data**: `data/<namespace>/servus_manufacturing/journal/<chapter>.json` with a parent chapter, an order and a list of tasks. A task is "do something N times" or "make something the first time", counted from events the alchemy machines report (calcine an item, gain mB of a principle, coagulate a result), with an optional item filter.
- **Per team progress**: task counts and finished chapters are stored per team (FTB Teams when present, else per player) in world data and survive restarts. When a chapter finishes, every member of the team receives the advancement `servus_manufacturing:journal/<chapter>`, now or at their next login; the book reads these advancements to unlock entries.
- **Unlocks**: a coagulation recipe can name a `chapter`; the Coagulator only lets a team select it after that chapter is finished.
- **Owner**: the Calciner and Coagulator remember the player who placed them, and their events count for that player's team.
- **Getting the book**: crafted from a book and a piece of coal, and given once to every player on their first login.
- **Command** `/servus_manufacturing journal` lists the team's chapters and task progress.
- **First chapters**: Villach (Nigredo, the first fire) and Schwaz (Albedo, the mines). Villach's tasks use the Calciner and Coagulator; Schwaz is the target for the gnome and Athanor changes, its entry opens when Villach is done.
- **Removed**: Research Lab, techs, tech tree screen, research payloads and the research spec.

## Capabilities

### New Capabilities
- `manufacturing/journal`: chapters, tasks, per team progress, advancements, command and the book.

### Modified Capabilities
- `manufacturing/coagulator`: recipes can be locked behind a chapter; the Coagulator remembers its owner.
- `manufacturing/calciner`: the Calciner remembers its owner and reports its work to the journal.
- `manufacturing/research`: every requirement removed.

## Non-goals

- Athanor, Elixir of Sight and Gnome Hollow tasks (their own changes add tasks and chapters).
- Final story text and art; the shipped chapters are a first draft.
- Live task progress inside the book pages (Modonomicon pages are static); progress is in the advancements screen and the command.

## Impact

- Code: `manufacturing/.../research`, `machine/lab`, `ResearchLabMenu`, `ResearchLabScreen`, `TechTreeScreen`, research payloads removed; new `journal` package; Calciner and Coagulator gain an owner and events.
- Build: Cloudsmith Maven repository and Modonomicon dependency in the Gradle build and `neoforge.mods.toml`; the combined dev runs load it.
- Data: journal chapter JSON, advancements, Modonomicon book JSON and lang from datagen.
- Docs: Manufacturing page, techs reference replaced by a journal reference, gamedata techs section stays empty.
