## Why

Fields grow on their own pace, and the only way to speed them up is bone meal by hand. Field work in M10 already sends Harvester shuttles out to till, plant and harvest; a second tool module that spreads fertilizer lets a warehouse speed up its fields without the player standing on them, while a per field reserve keeps the fertilizer other players and machines need in stock. The crop picker of `m10-crop-picker` was built as a reusable widget for exactly this tab.

## What Changes

- New tool module **Spreader**, crafted like the Harvester and fitted in the Shuttle Workshop into the shuttle's one tool place. Field only and wide like the Harvester (never takes warehouse picks or put-aways, never rides a lift), with the same tooltip lines. Drawn on the composed shuttle as a hopper box on the front with a spreader bar, vanilla placeholder textures.
- New field job kind next to the harvest run: the **fertilize run**. The fleet is shared per warehouse; any free Spreader shuttle serves any field. The Spreader picks the needed fertilizer from the warehouse before it drives out, doses unripe crops rail by rail with bone meal semantics (every item of the fertilizer tag is one bone meal application), and puts what is left back into storage like the harvest put-away.
- The Field Head decides when its field needs fertilizing at the warehouse's existing field check, never per tick. Modes per head: Off, After planting (plots planted since their last dose that are not ripe get one round of `doses_per_plot` doses) and Until ripe share (while the ripe share is below the head's own fertilize threshold, unripe planted plots get doses). A field calls a Spreader at most every `fertilize_interval` ticks. Ripe plots are never dosed.
- Per head **Keep in stock** reserve: the Spreader never takes the stock at or below it; it picks min(plots x doses, stock minus reserve, cargo room). Default from `fertilizer_reserve` in `field.json`. A field with reserve 0 may empty the stock.
- New item tag `servus_storage:fertilizer` (datagen), default `minecraft:bone_meal`.
- Field Head GUI: a fourth tab **Fertilizer** between Water and Upgrades (bone meal icon) with the reusable `ItemPicker` (tag items with warehouse stock, "Off" first), mode buttons, the fertilize threshold with - and +, the reserve with -10, -1, +1 and +10, and a status line (Spreader at work with its name plate, waiting, no Spreader shuttle, no fertilizer in stock, below reserve). The tab gets the red marker while fertilizing is on and the status is a problem. Choices travel by payload or button id and are validated on the server.
- New balance numbers in `storage/field.json`: `fertilize_interval`, `doses_per_plot`, `fertilizer_reserve`, `fertilize_share`.
- Pure logic (when a field needs fertilizing, what to fetch, the reserve, the interval, the status) without Minecraft imports and with JUnit tests; GameTests for the module, the runs, the reserve, the threshold, ripe plots and the leftover put-away.
- Lang via datagen; docs (`docs/src/content/docs/mods/storage.md`) and the `PLAN.md` M10 list.

## Capabilities

### New Capabilities

- `storage/spreader`: the Spreader module, fertilize jobs, the fertilize run (fetch with reserve, dosing, put-away of the leftover) and the runs that cannot go on.

### Modified Capabilities

- `storage/field-head`: the Field Head GUI requirement (in the version `m10-crop-picker` writes) gets the Fertilizer tab; new requirements for the fertilize settings per head and for tracking which plots were planted since their last dose.
- `storage/shuttle-workshop`: tool modules that mark field work name the Spreader next to the Harvester.
- `storage/shuttle-looks`: the tool part of a Spreader shuttle.

## Impact

- New: `storage/.../field/FertilizeMode.java`, `FertilizePlan.java`, `FertilizeStatus.java` (pure), `ChooseFertilizerPayload.java`, `FieldFertilizerStockPayload.java`; `storage/.../shuttle/FertilizeJob.java`, `FieldRun.java`, `SpreadRun.java`; `storage/.../client/FertilizerPickerEntries.java`; `gametest/SpreaderGameTests.java`; JUnit tests under `storage/src/test/.../field/`.
- Changed: `ShuttleFleet` (fertilize runs, shared field put-away), `HarvestRun` (common part moved to `FieldRun`), `ShuttleJob`, `JobNeeds`, `ModuleSpec`, `WarehouseNetwork.tickFields`, `FieldHeadBlockEntity` (settings, fresh plots, status), `FieldRipeness`, `FieldHeadMenu`, `FieldHeadScreen`, `FieldWorkerPayload`, `ServusStoragePayloads`, `StorageStatsManager`, `ServusStorageItems`, `ShuttleModels`, the item tag, recipe and language providers, `ServusStorageCommands` (field print), `storage/field.json`.
- Docs page, `PLAN.md`, docs gamedata regenerated with `:manufacturing:runData`.
