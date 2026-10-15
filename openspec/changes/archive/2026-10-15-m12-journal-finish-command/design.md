## Context

Journal progress lives in `JournalSavedData` per team; chapters finish only through events counted by `JournalLedger.record`, and `Journal.award` grants the chapter advancement that the Modonomicon research hooks turn into unlocked entries.

## Goals / Non-Goals

**Goals:** let an operator put a team at any chapter, for play tests and for worlds whose progress is stored under old ids.

**Non-Goals:** no migration of `servus_manufacturing` progress (nothing is released), no command to take a chapter back.

## Decisions

- `JournalLedger.finish(chapters, journal, chapterId)` is pure: it walks the parent chain from the root down, sets every task of each unfinished chapter to its count, marks it finished and returns the newly finished ids in order. An unknown id or a parent that is not loaded returns an empty list and changes nothing.
- `Journal.finish(server, team, chapter)` calls it, marks the saved data dirty and awards the advancements to online members with the same path as event finishing; offline members catch up at login as before.
- The command is a `finish` literal under `journal` with `requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` and a string argument suggested from `JournalChapters`. A greedy or quoted string is needed because chapter ids contain a colon: `StringArgumentType.greedyString()`.

## Risks / Trade-offs

- An operator can skip the story. Accepted: it is an operator command.

## Assumptions to confirm with the user

- Finishing a chapter also finishes its parents (otherwise the chapter would count as finished while still closed).
