## Context

`VeinData` loads `data/<ns>/servus_alchemy/vein/*.json` with `samples` (item tags), `yield`, `chapter` and growth numbers; `GnomeHollowBlock` plants a vein when the used item is in a vein's samples and the team finished its chapter. Athanor recipes carry a minimum `tier` (0 Nigredo, 1 Albedo, 2 Citrinitas, 3 Rubedo). `m12-hollow-look` adds a `color` per vein. Chapters stay the gate for journal tasks only.

## Decisions

- **One item** `servus_alchemy:seed` with data component `servus_alchemy:vein` (an `Identifier`). The Athanor recipe result names it: `"result": {"id": "servus_alchemy:seed", "components": {"servus_alchemy:vein": "servus_alchemy:gold"}}`. The Hollow plants the vein the component names.
- **Name:** `getName` returns `Component.translatable("item.servus_alchemy.seed.of", veinName)`, with the vein name from `vein.<ns>.<path>` ("Gold"); unknown or missing vein: "Unknown Seed".
- **Tint:** the item model (datagen) has a tint source `servus_alchemy:vein_color` (a registered `ItemTintSource`) that reads the component and looks the colour up in the client's vein table; layer 0 is a greyscale seed texture, layer 1 an untinted husk.
- **Vein table on the client:** `VeinSyncPayload` (id, colour, yield) sent on `OnDatapackSyncEvent` (login and reload), kept in `ClientVeins`. The Hollow renderer of `m12-hollow-look` uses it too.
- **Gate:** the Athanor tier only. `chapter` and `samples` leave `VeinData`; the journal keeps its chapter tasks, and Wandering years' "ripen a gold vein" stays reachable because the Citrinitas Athanor needs no chapter.
- **Recipes** (datagen, data JSON): iron and copper tier 1 (raw + 250 Salt + 100 Mercury), gold tier 2 (Raw Gold + 250 Salt + 250 Sulfur + 250 Mercury), redstone tier 2 (Redstone Dust + 250 Sulfur + 250 Mercury), diamond tier 3 (Diamond + 500 of each). Example numbers.
- **Order at the Hollow:** `bound` no longer requires `ripe`. States: empty (no gnome), bound (gnome, no vein), planted (gnome and vein, ripening), ripe (digging). The Salt Crystal binds an empty Hollow; a Seed plants on a bound Hollow; a Seed of another vein clears vein and ripeness and plants the new one (store kept); ripening ticks only on a bound Hollow.
- **Store after a swap:** the store remembers the item it holds, so Raw Iron dug before a swap to gold comes out as Raw Iron. The gnome digs into the store only while it is empty or holds his vein's yield; the old ore has to be taken out first.
- **Messages:** without a gnome every item but a Salt Crystal, a bucket or a gift reads "a gnome must live here first"; with a gnome, an item that is no Seed reads "he wants a seed"; a Seed of the vein that grows, or of a vein not loaded, is refused with its own line.
- **Gifts** need only a gnome, not a vein.
- **Seeds to the gnome:** the gnome entity of `m12-gnomes` forwards a Seed used on him to his Hollow; until that change, seeds go to the Hollow.
- **Journal events:** `bind` is recorded without an item when the gnome moves in; `ripen` is recorded with the yield when the vein reaches its last step. `wandering_years.json` task `gold` becomes `{"event": "ripen", "filter": "minecraft:raw_gold"}`, `basel_salzburg.json` task `diamond` becomes `{"event": "ripen", "filter": "minecraft:diamond"}`; their lang names become "Ripen a gold vein" and "Ripen a diamond vein".
- **Existing Hollows** keep their vein and gnome; a planted Hollow without a gnome from before waits for a Salt Crystal.

## Assumptions to confirm with the user

- The input of each seed recipe is one raw material (Raw Iron, Raw Copper, Raw Gold, Redstone Dust, Diamond), principles as above.
- Seeds are not used up by a Principle Dropper or anything else; only planting uses them.
