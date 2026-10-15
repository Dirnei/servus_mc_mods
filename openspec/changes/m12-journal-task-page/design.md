## Context

Chapters and counts live on the server (`JournalChapters`, `JournalSavedData`). The book is Modonomicon 2.17.0 data generated in `JournalEntries`; the tasks are text inside prose pages. Alchemy has no network payloads yet; Foundation registers its own with `RegisterPayloadHandlersEvent` (`ServusFoundationPayloads`).

## Goals / Non-Goals

**Goals:** the open chapter's tasks and progress visible on the first spread of its entry, always current.

**Non-Goals:** tasks of locked chapters (their entries stay locked), markers on the category map, changes to how tasks count.

## Decisions

- **Own page type** `servus_alchemy:journal_tasks` with one field `chapter`, registered with `BookPageTypeRegistry.register(id, MapCodec, StreamCodec)` during common setup, and a renderer registered with `PageRendererRegistry.registerPageRenderer(id, factory)` on the client. A dynamic text macro (`DynamicTextMacroRegistry`) was the alternative; its loader returns a fixed map and is evaluated when the book prerenders, so it cannot show live counts. Verify the `BookPage` constructor and `BookPageRenderer` methods in the 2.17.0 jar before coding.
- **Payload** `JournalProgressPayload` (play, server to client): for each chapter of the reader's team that is open or finished, the chapter id, finished flag and per task the id, count and needed. The client keeps the last one in `ClientJournalProgress`; the renderer reads it each frame, so an open book updates too.
- **Sending:** `Journal.record` and `Journal.finish` put the team in a dirty set; a server tick handler drains it once per tick and sends to the team's online members (mailbox pattern). Login and the datapack sync after `/reload` send to the affected players.
- **Page layout:** title "The Work", then per task: a box filled green when done, the task name (no numbers, the count shows them), and a bar with "count / needed" at its right. A finished chapter shows "Finished" under the title. Long names wrap. Prose task pages are titled "The Way" (or "The Errand") so they do not repeat the title.
- **Task names:** hand written in `ServusAlchemyLanguageProvider` as `journal.servus_alchemy.<chapter>.task.<id>`; a GameTest checks every loaded task has a key.
- **Prose:** the "To finish this chapter" lists and "To go on" sentences leave the story pages.

## Risks / Trade-offs

- `BookPageTypeRegistry` and `PageRendererRegistry` are not in Modonomicon's `api` package and may change in an update. Accepted: we pin 2.17.0 and the page is small.
- Counts like Salt mB change every Calciner cycle; the per tick drain sends at most one payload per team per tick.

## Assumptions to confirm with the user

- The task page is the first page of every chapter entry (left side of the first spread), the story starts on the right.
- Short task names, for example "Calcine 100 things", "Gather 1,000 mB of Salt", "Bind a gnome to a gold vein".
