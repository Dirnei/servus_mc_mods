# Proposal: m9-side-config-tabs

PLAN.md milestone M9 (Manufacturing becomes alchemy), new item between `m9-tria-prima` and `m9-journal`, added after the play check of the alchemy machines: `m9-side-config-tabs`.

## Why

A machine side has one mode for everything today. An Output side of the Calciner offers all three principles, so one pipe arm carries Salt, Sulfur and Mercury mixed, and a Fluid Tank behind it fills with the first principle and blocks the rest. The play check asked for the Mekanism way: configure per side which fluid leaves or enters there, so each pipe arm and its channel carry one principle.

## What Changes

- The side configuration gets **layers**: an Items layer (today's modes, unchanged) and a Fluids layer for machines with tanks. Energy keeps today's behavior and gets no layer.
- In the Fluids layer each side is Off, All, or one of the machine's tanks (Calciner: Salt, Sulfur, Mercury out; Coagulator: Salt, Sulfur, Mercury in). A side assigned to a tank exposes only that tank; All behaves like today. Output tanks are pushed into touching fluid handlers on the sides they are assigned to.
- The side panel gets a tab per layer the machine has (Items, Fluids). Fluid cells show the tank's fluid colour.
- The Configuration Carrot gets a layer: right clicking the air switches it between Items and Fluids, a click on a face cycles that face in the carrot's layer, and the side markers on blocks draw the carrot's layer.
- The Calciner and the Coagulator use the Fluids layer.
- PLAN.md lists the item in M9.

The Fluid Pipe and its eight channels stay as they are: with one principle per machine side, the channel of each pipe arm decides where that principle goes.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/side-config`: layers, the Fluids layer with tank assignment, panel tabs, the carrot's layer and markers per layer.
- `manufacturing/calciner`: principles leave only through sides assigned to their tank or to All.
- `manufacturing/coagulator`: principles enter only through sides assigned to their tank or to All.

## Non-goals

- No Energy layer. Powered machines keep accepting FE on every side but the front.
- No pipe buffer and no single fluid lock per pipe network; channels keep fluids apart.
- No Mekanism style eject switch; output tanks push on their assigned sides.
- Storage stations, pumps, the Fluid Tank, the Athanor and the Gnome Hollow get no Fluids layer in this change.

## Impact

- Foundation: `side` package (pure layer types and tests), `block/SidedBlockEntity`, `SideConfigWrench`, `SideConfigMenus`, `SideConfigData`, `client/SideConfigPanel`, `client/SideMarkerRenderer`, `item/ConfigurationCarrotItem`, lang.
- Manufacturing: Calciner and Coagulator block entities, menus and capability registration, `AlchemyMachines.push`.
- Machine menus sync six more data values for machines with a Fluids layer.
- Archive order: after `m9-tria-prima`, whose Calciner and Coagulator specs this change modifies.
