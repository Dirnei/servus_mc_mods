# Proposal

## Why

PLAN.md M8 item 10, `m8-native-machines`: Manufacturing machines report upkeep, and burner machines join the data line with fuel upkeep. Today only the Machine Adapter keeps its machine's fuel stocked, with its own loop, and the only Manufacturing producer the Crafting CPU can use is the Assembler. Burner machines on a data line still need hand fed coal, and a Burner Smelter or Crusher cannot run an order. This is the last M8 item; with it, every Servus machine takes part in on-demand production the same way.

## What Changes

- New Foundation capability `servus_foundation:upkeep`: a block names its upkeep item, the level to keep and how many it holds now, and is told when a refill was asked for.
- A shared upkeep keeper on every data network: every 40 ticks it looks at the members with the new upkeep role (found like the other roles, never by scanning) and asks a supplier on the same line for the missing amount, one request in flight per member, refill always, independent of orders. Arrivals are counted from the holder's count; a request that is not complete after a while is given up.
- New data role `UPKEEP`: a block exposing the upkeep capability is a data member, so the duct shows a data arm toward a Coal Generator, Burner Extractor or Burner Pump although they neither produce for orders nor hold stock.
- The Machine Adapter exposes the upkeep capability and uses the shared keeper instead of its own loop. Its behaviour is unchanged.
- Burner Extractor, Burner Smelter, Burner Pump and Coal Generator expose upkeep for their fuel slot. The upkeep item and level live in the machine stats JSON (`upkeep_item`, `upkeep_level`; coal and 16 for the burner machines, none for the others).
- Burner Smelter and Crusher expose the production capability with their recipes, so the CPU uses them as producers like the Assembler: the smelter reports every single item smelting recipe at its own work time, the crusher its crushing recipes with their input count and time.
- Production Terminal Machines tab: upkeep is shown for every member with the upkeep capability, and members with upkeep but no production (Coal Generator, extractors, pumps) are listed with their upkeep.
- Docs: Manufacturing, Foundation and the machine stats reference; the game data export carries the two new stats.

## Capabilities

### New Capabilities
- `foundation/upkeep`: the upkeep capability, the upkeep role and the shared keeper on the data network.
- `manufacturing/data-line`: Manufacturing machines on the data line: fuel upkeep for burner machines (item and level as machine stats), Burner Smelter and Crusher as producers.

### Modified Capabilities
- `foundation/data-cable`: members by capability include the upkeep role.
- `foundation/machine-adapter`: upkeep goes through the upkeep capability and the shared keeper.
- `storage/production-terminal`: the Machines tab shows upkeep for every upkeep member and lists upkeep only members.

## Non-goals

- Changing a native machine's upkeep item or level in its GUI: datapack JSON only for now.
- Extractors as order producers (they make items from deposits without inputs; on-demand extractors are a later item).
- Electric machines' energy as upkeep.
- Saving requests in flight across a restart.

## Impact

- `foundation`: `production` (new capability and interface), `data` (role, membership, network probe, keeper), `adapter` (refactor), Utility Duct data arms through `DataMembership`.
- `manufacturing`: `MachineStats`, `MachineDefaults`, burner machines, Burner Smelter, Crusher, capability registration, game data export, stats JSON.
- `storage`: `ProductionView`, `ProductionTerminalScreen`, lang.
- `docs`: Manufacturing, Foundation and machines reference pages, `gamedata.ts` schema, regenerated `gamedata.json`.
