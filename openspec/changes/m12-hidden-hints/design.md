# Design

## Context

See proposal.md for the why. Elemental Sight is a mob effect (`ElementalSight.active(player)`); hidden things are drawn by reveal renderers in `client/sight` (`RevealRenderers` calls them only under the sight; `HollowRevealRenderer` draws ore buds and a red cap from a client side `HollowTracker`). The Gnome Hollow is disguised as Stone (model, pick block, Jade ray trace callback in `ServusAlchemyJadePlugin`). The salamander already spawns one `FLAME` particle about every two seconds on the client (`Salamander.aiStep`). The Vas Hermeticum (`VasBlock` with `FREED`) draws its homunculus only under the sight. The journal counts named events per team (`Journal.recordItem`, `recordEvent`), chapters are JSON in `data/servus_alchemy/servus_alchemy/journal/`, advancements come from `JournalAdvancements`, the Opus and Workshop entries from `datagen/book`. Tunables live in small reload listeners like `ElixirDoseData`. NeoForge 26.1 fires `net.neoforged.neoforge.event.level.block.BreakBlockEvent` (cancellable, before the block is removed, on both sides, `getPlayer()`) and `ItemEntityPickupEvent.Post(player, itemEntity, originalStack)`; Storage already listens to `BreakBlockEvent` in `FieldEvents`. `ServerLevel.sendParticles(ServerPlayer, particle, overrideLimiter, alwaysShow, x, y, z, count, dx, dy, dz, speed)` sends particles to one player. `Block.animateTick(state, level, pos, random)` runs on the client for random blocks within 32 blocks of the player (`ClientLevel.animateTick`, 667 picks per tick in 16 and in 32 blocks). Sound definitions come from NeoForge's `SoundDefinitionsProvider` (`definition().subtitle(...).with(sound(id).pitch(...))`).

## Goals / Non-Goals

**Goals:**
- Every hidden thing has a hint without the sight, cheaply: client animation ticks, a block entity tick of the one Ancient Hollow, and reactions to block breaks and item pickups.
- The old gnome appears wherever a team mines deep after Schwaz, once per team, without world generation.
- The `find` event and a side chapter so the journal reacts and the next change can build on it.

**Non-Goals:**
- Befriending the old gnome and taking him home as the House Gnome companion: the next change (`m12-house-gnome`, added to PLAN.md M12).
- Real story text: dialogue and story lines are placeholders for the storyteller.
- New audio files, new particle types, light flicker as a hint.

## Decisions

### D1 Knock sound
`hint/HintSounds` registers `SoundEvent.createVariableRangeEvent(servus_alchemy:hint.knock)` in a `DeferredRegister<SoundEvent>`. `ServusAlchemySoundsProvider extends SoundDefinitionsProvider` writes `assets/servus_alchemy/sounds.json`: the four vanilla files `minecraft:dig/stone1..4` at pitch 0.6, subtitle key `subtitles.servus_alchemy.hint.knock` ("Something knocks in the rock"). Variable range: audible over 16 blocks times the volume.

### D2 Gnome Hollow knocking
`GnomeHollowBlock.animateTick`: if the client block entity shows a bound gnome and `random.nextInt(6) == 0`, `level.playLocalSound(center, KNOCK, BLOCKS, 0.35, 0.8 to 1.0, false)`. Given how often the client picks a block, a player next to the Hollow hears a soft knock every 15 to 20 s, rarer farther away. No server cost, no scan, everyone nearby hears it.

### D3 Salamander dancing flame
In `Salamander.aiStep` on the client, a wild salamander besides the existing flame rolls `random.nextInt(DANCE_CHANCE = 200) == 0` and spawns six `FLAME` particles on a small rising spiral (angle steps of 60 degrees, radius 0.3, rising speed 0.05). About every ten seconds.

### D4 Vas stirring
`VasBlock.animateTick`: unless `FREED`, `random.nextInt(5) == 0` spawns two `BUBBLE_POP` and one `SPLASH` particle at the liquid surface (block centre, y 5/16, spread 2 px) and, rarely (one in four of those), a soft `BUBBLE_COLUMN_BUBBLE_POP` sound. Every Vas holds a homunculus from the moment it is placed, so "not freed" is the whole condition.

### D5 Settings
`hint/OldGnomeSettings(double wakeChance, String wakeChapter, int maxY, int knockRange, int knockInterval, int dustRange, int traceRange, double traceChance)` with `DEFAULTS` (0.003, `servus_alchemy:schwaz`, 0, 24, 100, 6, 8, 0.15). `hint/OldGnomeData` loads `data/<ns>/servus_alchemy/old_gnome/*.json` (codec with optional fields defaulting to `DEFAULTS`); `settings()` returns the file `servus_alchemy:old_gnome` or the defaults. Registered in `AlchemyEvents`. Methods that act take the settings as a parameter, so GameTests pass a chance of 1 and an unlimited height (like `SalamanderTemper` in the salamander tests). Chance 0.003: about one in 330 deepslate blocks, a few minutes of strip mining.

### D6 Old gnome ledger (pure)
`hint/OldGnomeLedger` holds per team an `Entry(State state, long pos, String dimension)` with `State` NONE, AWAKE, FOUND. Rules: `mayWake(team)` only from NONE; `wake(team, pos, dim)` NONE to AWAKE; `found(team)` AWAKE to FOUND (FOUND stays); `lost(team, pos)` AWAKE at that pos to NONE, FOUND unchanged. `OldGnomeSavedData extends SavedData` (overworld data storage, name `servus_old_gnome`) saves the ledger. JUnit: `OldGnomeLedgerTest` (`wakesOnlyOnce`, `lostBeforeFoundMayWakeAgain`, `foundNeverWakesAgain`, `lostAtOtherPosIsIgnored`, `foundOnlyFromAwake`).

### D7 Choosing the wall (pure)
`hint/AncientHollowSite.choose(int startDirection, Predicate<int[]> isWall, Predicate<int[]> isAir)` walks a fixed ordered list of offsets: distances 3, 2, 4 from the broken block along each of the four horizontal directions (starting at `startDirection`, turning clockwise), each with a sideways shift of +1, -1, 0 and a height of 0, +1, -1 (at most 108 positions). The first offset that `isWall` and that has `isAir` on one of its four horizontal sides (checked in the order north, east, south, west) wins and returns `Site(dx, dy, dz, side)`. Only runs after the chance hit, so at most a few hundred block reads once per team. JUnit `AncientHollowSiteTest`: `picksTheTunnelWall`, `startDirectionChangesTheChoice`, `noneWithoutAir`, `skipsTheBrokenBlockItself` (offset 0 is never a candidate), `sideFacesTheAir`.

### D8 Ancient Hollow block
`hint/AncientHollowBlock extends BaseEntityBlock` with `HorizontalDirectionalBlock.FACING` (the open face), properties like Deepslate (`MapColor.DEEPSLATE`, strength 3.0/6.0, `requiresCorrectToolForDrops`, `SoundType.DEEPSLATE`), pickaxe tag, loot Cobbled Deepslate, no block item. Model: `cube_column` with `block/deepslate` and `block/deepslate_top`, same for every facing. Pick block returns Deepslate without the sight. `hint/AncientHollowBlockEntity` saves the team, `Found` and a knock timer.
- Server tick: unless found, every `knockInterval` ticks (first at a random offset) if a non spectator player is within `knockRange` it starts a series of three knocks five ticks apart (`level.playSound(null, pos, KNOCK, BLOCKS, 1.5, 0.55 to 0.65)`, so audible from 24 blocks). Every 10 ticks it sends two `FALLING_DUST` particles of Deepslate from the top edge of the open face to each player within `dustRange` (`sendParticles(ServerPlayer, ...)`).
- `preRemoveSideEffects` calls `OldGnome.lost(level, team, pos)`.
- Client: `onLoad` adds it to `AncientHollowTracker`, removal and chunk unload remove it.
- Jade: a ray trace callback shows it as Deepslate without the sight, like the Gnome Hollow.

### D9 OldGnome service
`hint/OldGnome`:
- `onBroken(ServerLevel, UUID team, BlockPos, BlockState, OldGnomeSettings, RandomSource)`: if the state is in `servus_alchemy:wakes_old_gnome` (block tag, datagen: Deepslate): `tryWake`, then `maybeDropTrace`.
- `tryWake`: team not null, `pos.getY() < maxY`, `Journal.isFinished(team, wakeChapter)`, ledger `mayWake`, `random.nextDouble() < wakeChance`, a site from D7 (wall = block in the tag, air = `isAir()`). Then `setBlock(ancient_hollow facing side)`, team on the block entity, ledger `wake`, a trace item in front of the open face, and a first knock.
- `maybeDropTrace`: ledger AWAKE in this dimension, broken block within `traceRange` of the Ancient Hollow (and not the hollow itself), `random.nextDouble() < traceChance`: pop a random trace at the broken block.
- `find(ServerLevel, Player, AncientHollowBlockEntity)`: dialogue line, `Journal.recordNamed(level, team, "find", "servus_alchemy:old_gnome")`, block entity found, ledger `found(owner team)`.
- `lost`: ledger `lost`.
`hint/HintEvents` (game bus): `BreakBlockEvent` on the server with a `ServerPlayer` calls `onBroken` with `Teams.teamOf(player)` and `OldGnomeData.INSTANCE.settings()`; `ItemEntityPickupEvent.Post` with a server player and an original stack in `hint_traces` records `Journal.recordItem(level, team, "find", stack, 1)` and sends the overlay line `message.servus_alchemy.hint.trace`.

### D10 Trace items
`TraceItems` registers `tiny_pickaxe`, `red_felt_scrap` and `salt_offering` (plain items, stack 16) in `ServusAlchemyItems` and the creative tab; tag `servus_alchemy:hint_traces`. 16x16 textures drawn by a Python script in the job directory: a little iron pick with a wooden haft, a ragged red felt scrap, a small wooden bowl heaped with white salt.

### D11 Old gnome reveal
`client/sight/OldGnomeRevealRenderer` (registered with `RevealRenderers`): for every tracked, not found Ancient Hollow within 32 blocks, it draws, 0.25 blocks in front of the middle of the open face and turned to face out, a red mushroom item as the cap (like the Hollow gnome, item scale 0.5, centre 0.3 above the face middle) and below it a beard: a white wool block item scaled to 0.22 x 0.36 x 0.08 blocks (centre 0.15 below the face middle), both swaying slowly. Overlap check: the cap's sprite spans about 0.05 to 0.55 above the face middle, the beard -0.33 to 0.03, so they do not overlap and share no plane; both stand at least 0.21 blocks off the block face, so nothing lies on its plane.

### D12 Journal
`Journal.recordNamed(level, team, event, name)` counts tasks whose filter is empty or equal to the name. Chapter `old_gnome.json`: parent `servus_alchemy:schwaz`, order 4, tasks `trace` (`find`, `#servus_alchemy:hint_traces`) and `gnome` (`find`, `servus_alchemy:old_gnome`). `JournalAdvancements` gets a side chapter list (`old_gnome` with parent `schwaz`, icon the Tiny Pickaxe). Opus entry `old_gnome` (icon Tiny Pickaxe) under Schwaz, open with the Schwaz research node, story about the knockers. Workshop entry `knockers` "Knocking in the Rock" (icon Tiny Pickaxe) beside the Gnome Hollow, open with the Albedo entries. Lang: chapter name and finished text, subtitle, items, block, trace line, dialogue line.

### D13 Folklore and story text
The Opus text tells, as Paracelsus, that the miners of Schwaz heard the Bergmaennlein knock in the rock before a rich vein or a collapse and left them bread, and that miners elsewhere (the Knockers of Cornwall) tell the same. Marked to check: the Knockers are Cornish and Welsh folklore, best documented from the 18th and 19th centuries, so Paracelsus (1493 to 1541) could not have known the Cornish name; the text keeps the Cornish knockers as "miners in far lands" without dates. The Bergmaennlein of the German speaking mines and Paracelsus' own writings on gnomes and pygmies (*Liber de nymphis, sylphis, pygmaeis et salamandris*, published 1566) are to check before the final text.

## Assumptions to confirm with the user

- The chapter that wakes the old gnome is Schwaz (finished), from `wake_chapter` in `old_gnome.json`.
- Wake chance 0.003 per deepslate block below Y 0; knocking within 24 blocks every 5 s; dust within 6 blocks; traces within 8 blocks with chance 0.15; one trace lies in front of him when he wakes.
- Only plain Deepslate wakes him (tag `servus_alchemy:wakes_old_gnome`); deepslate ores, Cobbled Deepslate and Tuff do not.
- Breaking the Ancient Hollow before finding him lets him wake again elsewhere; after he is found, nothing wakes again, and the found state stays in the team data for the House Gnome change even if the block is gone.
- Anyone near hears the knocking and sees the dust; traces drop only for his own team; any player under the sight with the journal can find him, and the find counts for that player's team while the old gnome is marked found for the team he woke for.
- The trace pickup line shows on every pickup; the journal task counts the first.
- The dialogue line and the Opus story are placeholders marked `[storyteller]` (dialogue) or kept short for a later pass.
- The gnome knocking in a Hollow, the salamander dance and the Vas stirring use fixed chances in code, not data: they are looks, not balance.

## Risks / Trade-offs

- `BreakBlockEvent` fires before the break and can be cancelled later by another listener; a woken hollow then stands next to an unbroken block, which is harmless.
- Client animation ticks depend on random block picks, so the gnome knocking is irregular by design; far away it is rare.
- The dialogue only shows once a player has the sight and the journal in hand; without the Workshop entry players might not know to try, so the trace line and the Opus entry both say to drink and look.
