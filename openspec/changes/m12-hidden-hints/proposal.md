## Why

`alchemy_plan.md` ("Hints for hidden things") sets a rule: everything hidden behind Elemental Sight shows a hint without it, so the player knows when to drink. Today a working gnome, the homunculus in the Vas and (apart from one flame) the salamander give no sign at all, and Paracelsus' old gnome from the mines of Schwaz, the future House Gnome, does not exist yet.

## What Changes

- New sound event `servus_alchemy:hint.knock` (vanilla stone dig sounds at a low pitch, subtitle "Something knocks in the rock").
- A Gnome Hollow with a working gnome knocks softly now and then for everyone near it, played on the client from the block's animation tick (no scan, no server work).
- A wild salamander sometimes makes its flame dance: a small spiral of flames above the lava, next to the single flame it already shows.
- The liquid in an unfreed Vas Hermeticum sometimes moves by itself: a few bubbles and a splash on its surface, for everyone, from the block's animation tick.
- The old gnome, without world generation: once a team has finished the wake chapter (default Schwaz), each deepslate block one of its players breaks below a height (default Y 0) has a small chance (data) to wake him. The mod checks a fixed list of nearby positions and turns the first deepslate block with air beside it into an **Ancient Hollow**, which looks like deepslate without the sight. One per team, saved in world data; it never wakes again once found.
- The Ancient Hollow knocks (three knocks, from its place, so the sound has a direction) while a player is within knocking range, lets deepslate dust fall in front of its open face while a player is within a few blocks, leaves a trace beside it when it wakes, and deepslate broken near it by its team sometimes drops a trace.
- Three trace items: a Tiny Pickaxe, a Scrap of Red Felt and a Bowl of Salt (item tag `servus_alchemy:hint_traces`). Picking one up records a `find` event for the team and the journal says "They are near. Drink, and look."
- Under Elemental Sight the Ancient Hollow shows the old gnome in front of its open face: the red cap of the Hollow gnome plus a long white beard. The journal used on it under the sight gives a placeholder dialogue line (`[storyteller] ...`) and records the `find` event `servus_alchemy:old_gnome`; the team's old gnome is then found.
- Journal: new event `find` and a side chapter "The Old Gnome" (after Schwaz) with two tasks, find a trace and find the old gnome, with an Opus entry telling of the knockers (placeholder story in Paracelsus' voice). A Workshop entry "Knocking in the Rock" explains the hints.
- Data: `data/servus_alchemy/servus_alchemy/old_gnome/old_gnome.json` holds the wake chance, wake chapter, height, ranges, intervals and trace chance.
- Taking the old gnome home as the House Gnome companion is not part of this change; it is the next change (PLAN.md M12).

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts`, `m12-athanor-citrinitas-rubedo` (chapters), `m12-principle-dropper`, `m12-elixir-dose`, `m12-salamander` (the flame hint it extends), `m12-salamander-forge`, `m12-receptaculum` (the Vas it gives a hint) and `m12-prelum`. Its journal delta only adds requirements and modifies none of theirs.

## Capabilities

### New Capabilities
- `alchemy/hidden-hints`: the knock sound, the hints of the Gnome Hollow, the salamander and the Vas, the old gnome's waking, the Ancient Hollow with its knocking, dust, traces and reveal, the trace items and the Workshop entry.

### Modified Capabilities
- `alchemy/journal`: the `find` event and the side chapter The Old Gnome.

## Impact

- New package `alchemy/.../hint/`: `HintSounds`, `TraceItems` (trace items and tag), `OldGnomeSettings` and `OldGnomeData` (data loader), pure `AncientHollowSite` and `OldGnomeLedger`, `OldGnomeSavedData`, `OldGnome` (wake, traces, find), `AncientHollowBlock`, `AncientHollowBlockEntity`, `AncientHollowTracker`, `HintEvents` (block break and item pickup).
- Changed: `GnomeHollowBlock` (animateTick knock), `VasBlock` (animateTick bubbles), `Salamander` (dancing flame), registries, `AlchemyEvents` (listener), Jade plugin (Ancient Hollow shows as Deepslate), `client/sight` (old gnome reveal renderer).
- Datagen: block state and model, loot (Cobbled Deepslate), tags (pickaxe, `wakes_old_gnome`, `hint_traces`), sound definitions, lang (subtitle, items, block, messages), journal advancement and Opus entry, Workshop entry.
- Data: `old_gnome/old_gnome.json`, `journal/old_gnome.json`.
- Assets: three item textures from a script.
- Tests: JUnit for `AncientHollowSite`, `OldGnomeLedger`; GameTests in a new `HintGameTests`.
