## Why

`alchemy_plan.md` (Rubedo) names the end goal of Servus: Alchemy: the **Prelum**, the Press, built once with Dragon's Breath, presses the Caput Mortuum that piles up behind the Purifier since Nigredo into Netherite. Nothing uses Caput Mortuum yet, and the Rubedo stage has no product of its own.

## What Changes

- New recipe type `servus_alchemy:pressing`: a counted input pressed into one result in a time, all datapack data. Alchemy ships one: 16 Caput Mortuum into 1 Netherite Scrap in 600 ticks.
- New block **Prelum**: a heavy screw press in the family style, a polished deepslate base with a steel press bed, two dark oak posts and a dark oak head beam, a steel screw through the beam with a turning bar on top and a steel press plate. The screw, the bar and the plate are drawn by a block entity renderer: while it presses, the bar turns and the plate goes down onto the bed and back up.
- A Foundation machine like the Salamander Forge: it faces the player, opens a screen (input, progress arrow, output, side panel), takes only what a pressing recipe accepts, exposes its items to pipes and hoppers on every side but the front, pushes its output, is configured and dismantled with the carrot and drops its contents. It needs no fuel and no principles.
- A producer on the data line: the Foundation production capability lists every pressing recipe, so the Crafting CPU can order Netherite Scrap.
- Built once with Dragon's Breath: a Rubedo Athanor craft turns a bottle of Dragon's Breath into the **Prelum Screw** (a new item); the Prelum is crafted at a crafting table from the screw, a Heavy Weighted Pressure Plate, Stripped Dark Oak Logs and Polished Deepslate. The Dragon's Breath gates the machine behind the End, the Athanor tier gates it behind Rubedo.
- Journal: a new `press` event counts pressed results; the Basel and Salzburg chapter gets a fourth task, "press Netherite Scrap at the Prelum", and its task page lists it. A Workshop entry "The Prelum" opens with Basel and Salzburg beside the Rubedo entries, with a placeholder story line marked `[storyteller]`.
- JEI: a Pressing category with the Prelum as its catalyst.
- Docs: a Prelum section on the Alchemy page and the pressing recipe type and machine row in the datapack reference.

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts`, `m12-athanor-citrinitas-rubedo` (the Rubedo tier and the Basel and Salzburg chapter), `m12-principle-dropper`, `m12-elixir-dose`, `m12-salamander`, `m12-salamander-forge` (the machine pattern it follows) and `m12-receptaculum`. Its journal delta adds requirements next to the Basel and Salzburg requirement added by `m12-athanor-citrinitas-rubedo` and does not modify it.

## Capabilities

### New Capabilities
- `alchemy/prelum`: the Prelum block, the pressing recipe type, its screen, automation sides, production capability, the Prelum Screw and recipes, its look and animation, the JEI category and the Workshop entry.

### Modified Capabilities
- `alchemy/journal`: a `press` event, and the Basel and Salzburg task that counts it.

## Impact

- New package `alchemy/.../prelum/`: `PressingRecipe`, `PrelumBlock`, `PrelumBlockEntity`, `PrelumMenu`.
- Registries: recipe type and serializer (synced to clients), block, block item, Prelum Screw item, block entity, menu; capabilities in `ServusAlchemyEvents`; screen layout in `client/MachineScreens`; `PrelumRenderer` in `client/`; machine defaults in `AlchemyMachineDefaults` and `data/servus_alchemy/machine/prelum.json`.
- Datagen: pressing and Athanor rows in `AlchemyRecipeTable`, crafting recipe, models (block and item), loot, pickaxe tag, lang, Workshop entry, Basel and Salzburg task page.
- Data: `data/servus_alchemy/servus_alchemy/journal/basel_salzburg.json` gains the `press` task.
- Assets: GUI texture, Prelum Screw item texture.
- JEI: `PressingRecipeCategory`.
- Docs: `docs/src/content/docs/mods/alchemy.md`, datapack reference pages.
- Tests: JUnit for the defaults, GameTests in a new `PrelumGameTests`.
