## Why

Play tests cannot reach the late journal chapters without replaying every earlier one, and a world played before the rename to Alchemy keeps its progress under `servus_manufacturing` ids, so its journal shows nothing finished. An operator needs a way to put a team at a chosen chapter.

## What Changes

- New operator command `/servus_alchemy journal finish <chapter>`: finishes the chapter and every unfinished chapter above it for the player's team, fills their task counts and grants the chapter advancements to the online team members, so the book unlocks the following entries.
- The chapter argument suggests the shipped chapter ids.
- `/servus_alchemy journal` without arguments stays open to every player.

## Capabilities

### New Capabilities

### Modified Capabilities
- `alchemy/journal`: adds the operator command that finishes a chapter. Written against the main spec plus the unarchived m12 deltas, which do not touch the journal command.

## Impact

- `alchemy/src/main/java/io/github/dirnei/mc/servus/alchemy/journal/JournalLedger.java`: pure `finish` logic with JUnit tests.
- `alchemy/src/main/java/io/github/dirnei/mc/servus/alchemy/journal/Journal.java`, `JournalCommand.java`: the command and awarding.
- Lang keys for the command's messages (datagen).
