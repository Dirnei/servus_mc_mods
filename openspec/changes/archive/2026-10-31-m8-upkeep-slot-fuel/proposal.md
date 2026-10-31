# Proposal

## Why

The play test found a Coal Generator ("Ofen 2") with 11 Coal Blocks in its fuel slot that kept asking upkeep for coal: the count counted any fuel, the level was not reached, and coal cannot join a slot of Coal Blocks. Every shipment was refused at the generator and waited forever at the Packing Station, keeping coal wanted in its To network buffer, and nothing at the station ever gave such a shipment up. Every 1200 ticks the request timed out and asked again without ending the old shipment (25 open shipments piled up), a restart forgot every open request so every generator asked again, and generators topped up after every 1 or 2 coal burned. This fixes M8 upkeep (PLAN.md 2.4 "Upkeep is not part of a recipe", milestone M8 On-demand autocrafting), a bug in a built feature, not a new open point.

## What Changes

- A burner machine asks upkeep for the item already in its fuel slot; an empty fuel slot falls back to the machine stats `upkeep_item` (coal by default). A fuel slot holding something that does not burn asks for nothing. The level is capped at what one stack of the item holds.
- The open upkeep request is saved on the upkeep holder's block entity, so it survives a restart, and arrivals keep counting against it after the reload.
- A request also ends once the supplier reports its shipment as no longer open, so a lost or finished shipment never blocks the next ask.
- New machine stat `upkeep_batch` (example 8): a machine asks again only once at least a batch is missing (or it holds none), not after every coal it burns, and then asks for everything missing. The Machine Adapter follows the same rule, with its batch from the machine profile's `upkeep` object (`batch`, default 8), so it stays datapack driven without a new GUI field.
- **BREAKING (API)**: the supplier capability answers a request with the amount and a ticket, can be asked whether a ticket is still open, and can cancel a ticket. A timed out upkeep request cancels its ticket, so the warehouse ends the shipment, drops its queued picks and puts picked items back into storage.
- The upkeep capability gains the batch and the saved open request; every holder (burner machines, Calciner, Machine Adapter) stores the request and names its batch.
- Safety net at the Packing Station: a shipment whose target is removed (no block at a loaded position takes items any more) ends at once. A shipment whose target is cut from the data line (no route, route broken, or no longer a member of a line reaching the station) starts a refusal clock and ends only once it has been cut, or refused, without a break for longer than a timeout, so a duct broken briefly while rebuilding does not end it. The refusal by the target itself counts only for machine targets (upkeep, Machine Adapter, CPU steps, every data member that is not a Storage Terminal); a terminal request whose Output slot the player keeps full keeps waiting as today, with no timeout. A target in an unloaded chunk is neither removed nor cut; its shipment waits. An ended shipment ends short: its items in the To network buffer go back into storage, its queued picks are dropped, and its requester sees it ended and may ask again.
- New Shuttles warehouse stat `refused_shipment_timeout` next to `push_interval` (example 1200 ticks).
- This rule also clears the coal stuck in Packing Stations of existing worlds: after the update those old refused shipments time out and their coal goes back into storage, so no one-time save cleanup is needed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/upkeep`: upkeep item follows the fuel slot, batch threshold, persisted open request, request ends on supplier ticket state, timed out request is cancelled at the supplier, new `upkeep_batch` stat, pure bookkeeping updated.
- `foundation/machine-adapter`: machine profiles may name an upkeep batch; the adapter waits until at least a batch is missing (or the machine holds none) and then asks for everything missing.
- `shuttles/packing-station`: the warehouse supplier answers with a ticket, reports whether it is open and cancels it, putting picked items back into storage; a shipment to a removed target ends short at once, one to a target cut from the line, or to a machine target that refuses it, ends short past the timeout; terminal requests into a full Output slot keep waiting.

## Non-goals

- Packing Station slot stacking of picks (separate change `m8-packing-stack-picks`).
- A batch setting in the Machine Adapter GUI; the batch comes from the machine profile.
- A timeout for terminal requests into a full Output slot; they wait for the player as today.
- Showing open upkeep requests in the Production Terminal or the Data Potato.
- A one-time cleanup of existing saves; the refusal timeout ends the shipments that already piled up.
- A setting per request or per target for the refusal timeout; it is one warehouse stat.

## Impact

- Foundation: `production/ItemSupplier`, `production/UpkeepHolder`, `data/UpkeepBook` (pure, JUnit), `data/DataNetwork` (upkeep check), `machine/BurnerMachineBlockEntity`, `machine/MachineStats`, `adapter/MachineAdapterBlockEntity`, `adapter/MachineProfile` (optional `upkeep.batch`), default machine stat JSON via datagen if stats are generated.
- Shuttles: `packing/WarehouseSupplier`, `packing/PackingStationBlockEntity` (shipment remembers its pick request id and since when it is refused or cut, ends shipments to removed targets at once and to cut or refusing machine targets past the timeout), `network/WarehouseNetwork` (find and cancel a shipment by ticket), `stats/ShuttlesStatsManager` and `data/servus_shuttles/shuttles/warehouse.json` (`refused_shipment_timeout`).
- Alchemy: the Calciner inherits the burner upkeep change; `machine/AlchemyMachineDefaults` passes the new stat.
- Saves: block entities gain an optional saved upkeep request; old saves load without one and ask again once. Saved Packing Station shipments gain an optional refusal start; old ones start the clock at their first refusal after the load.
- Players: a terminal request whose Output slot the player keeps full still waits, as today. A shipment whose duct is broken now ends only after the timeout instead of at once, so rebuilding a duct quickly keeps it. A Machine Adapter tops up in batches.
