# Design

## Context

The Sink shipped in released Foundation versions (tags `foundation-v0.x`). Its name lives only in lang: `MachineDataParts` adds the block name and four messages, and the Alchemy journal (`WorkshopCategory`) mentions it once. No texture or model carries the word.

## Goals / Non-Goals

**Goals:** players see "Incinerator" everywhere in game.

**Non-Goals:** renaming ids, classes, lang keys, the spec path `foundation/sink` or the docs site.

## Decisions

- **Lang only.** The block keeps the id `servus_foundation:sink`. *Alternative:* a new id with a registry alias (NeoForge `DeferredRegister.addAlias`). Rejected: it touches block, item, block entity and menu registrations and every recipe and tag for no gain the player sees.
- **Keys stay.** `block.servus_foundation.sink` and the `message.servus_foundation.sink.*` keys keep their names; only values change, so resource packs and translations keep working.
- **PLAN.md** says Incinerator in its tables and text in the same change.

No NeoForge API is involved (datagen `LanguageProvider.add`, already in use). Nothing here is pure logic; checks are the generated lang files and a look in game.

## Risks / Trade-offs

- Code and specs still say Sink. Acceptable: the Javadoc of `SinkBlock` can say it is shown as the Incinerator.
