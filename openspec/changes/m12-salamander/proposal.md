## Why

`alchemy_plan.md` (Citrinitas) sends the player to the Nether for the salamander: a fire elemental that lives in the lava, shows itself only under Elemental Sight, is befriended with Gold Ingots like a wolf with bones and digests gold into Sal Ammoniac. Sal Ammoniac and Sulfur at the Athanor make Ignis Perpetuus, the eternal flame that later builds the Salamander Forge and the Receptaculum. Nothing of this exists yet, and Alchemy has no entity at all.

## What Changes

- New entity **Salamander** (the mod's first entity): a lizard of glowing coals with a snout, glowing eyes, a three segment tail that sways, four legs and a crest of flickering flames, drawn full bright. Fire immune, walks on lava like a Strider and on land, harmless.
- It lives mainly in the Crimson Forest, a bit less in the Warped Forest, on the ground near crimson or warped stems, wart blocks and Nether Wart, and rarely on lava lakes in the Nether Wastes and Basalt Deltas. A NeoForge biome modifier holds the weights, a block tag the home blocks, and a crowd limit from data keeps it from crowding an area. (Revised after the play check: on lava only and at weight 5 it practically never spawned.)
- Hidden: a wild salamander is not drawn (no model, shadow or name) for a player without Elemental Sight, and Jade does not name it; a stray flame particle now and then is all that shows. A befriended salamander is drawn for everyone.
- Befriending: right click a wild salamander with a Gold Ingot. Each ingot is used up and befriends with a chance from data (example 1 in 3), hearts on success, smoke on failure.
- A befriended salamander follows its owner, teleports to them when far away, sits and stands up on an empty hand right click by the owner, never despawns, and walks to a nearby lit campfire to lie in it when idle.
- Sal Ammoniac: the owner feeds a befriended salamander a Gold Ingot by hand; it digests it and drops one Sal Ammoniac after a time from data (example 60 s), one ingot at a time.
- Ignis Perpetuus: new item made on a Citrinitas Athanor from one Sal Ammoniac and Sulfur. It is a building part, not a furnace fuel.
- Tooltips in Paracelsus' voice on Sal Ammoniac and Ignis Perpetuus; a Salamander spawn egg for testing; a Workshop entry "The Salamander" that unlocks with the Wandering years chapter.
- No journal task asks for the salamander: the plan keeps it optional for progression (see design).

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts`, `m12-athanor-citrinitas-rubedo` (the Citrinitas tier and the Wandering years chapter it uses), `m12-principle-dropper` and `m12-elixir-dose`. It adds a new capability only and does not modify their specs.

## Capabilities

### New Capabilities
- `alchemy/salamander`: the salamander entity, its spawning, hiding, befriending and pet behaviour, Sal Ammoniac digestion, the Ignis Perpetuus Athanor craft, tooltips, spawn egg and Workshop entry.

### Modified Capabilities
None.

## Impact

- New package `alchemy/.../salamander/`: entity, goals, pure `SalamanderTemper` and `Digestion`, reload listener for the temper data, spawn and attribute registration.
- New client classes: model, renderer, render state, layer registration; Jade ray trace callback.
- Registries: an entity type register, items Sal Ammoniac, Ignis Perpetuus, Salamander spawn egg.
- Data: `servus_alchemy/salamander_temper/salamander.json` (with the spawn numbers), `neoforge/biome_modifier/salamander.json`, block tag `servus_alchemy:salamander_home` from datagen, Athanor recipe through `AlchemyRecipeTable`.
- Spawning: a custom spawn placement type for ground or lava and a spawn rule with the home and crowd checks; pure `SalamanderSpawn` for the checks.
- Assets: entity texture, item textures, generated item models and lang.
- Journal: `WorkshopCategory` entry. Docs: Alchemy page and a datapack reference page.
- Tests: JUnit for the pure logic, GameTests in a new `SalamanderGameTests`.
