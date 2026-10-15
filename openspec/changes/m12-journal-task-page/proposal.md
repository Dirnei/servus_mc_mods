## Why

The journal hides what a chapter asks for in its prose, often on a later page, and progress shows only through `/servus_alchemy journal`. Players cannot see in the book what to do next or how far they are.

## What Changes

- New book page type: the chapter's task list. Every chapter entry shows it as the first page after its title, so the open chapter's work is visible on the first spread.
- Each task shows a short name, its progress as "40 / 100" with a small bar, and a check mark once done. A finished chapter says so above the list.
- The server sends each player their team's journal progress at login, after `/reload` and when it changes (collected and sent once per server tick).
- The task lists in the chapter prose go; the prose keeps the story and the how to.

## Capabilities

### New Capabilities

### Modified Capabilities
- `alchemy/journal`: the book shows each chapter's tasks with live team progress. Written against the main spec plus the unarchived m12 deltas (m12-journal-finish-command adds the finish command, untouched here).

## Impact

- New: journal progress payload (server to client), client side progress cache, a Modonomicon page type with codec and stream codec, its client renderer.
- `alchemy/.../journal/Journal.java`: marks teams whose progress changed; a server tick handler sends it.
- `alchemy/.../datagen/book/JournalEntries.java`: a task page per chapter, prose task lists removed; lang keys for every task name.
- Depends on Modonomicon's non API classes `BookPageTypeRegistry`, `BookPage` and `PageRendererRegistry` (2.17.0).
