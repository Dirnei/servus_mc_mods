# Design

## Context

This change builds on `m0-port-minecraft-26-1-2`, `m11-controller-site`, `m11-chunk-loader-upgrade` and `m11-wireless-packing-station`, which are archived; its deltas modify requirements they added (`Controller GUI`, `Controller lists the team's sites`, `Other sites in the terminal`, `Transfers from another site`) and remove the whole `storage/wireless-packing-station` capability and the requirement `Record knows the wireless stations`.

Archive order of the open changes: `m11-drop-crates-pipe-access`, then `m8-cpu-buffer`, then `m11-controller-wireless`, then `m11-warehouse-power`. This change is written against the main specs as they are after `m11-drop-crates-pipe-access`: crates, the indexing of adjacent inventories, every instant pick and put-away, the terminal's pull to the cursor and pipe access to the Packing Station are gone, and the requirement "Sources are the warehouse's own racks" replaces "Sources are nodes and adjacent inventories". That replacement already keeps every Servus block out of the index, so this change no longer modifies a sources requirement (it did before, to name the Wireless Packing Station). Transfers pick only from racks by shuttle. No requirement header is shared with `m8-cpu-buffer`, so the order between the two only follows the milestones.

Current code (all applied on main):
- `WirelessPackingStationBlockEntity extends PackingStationBlockEntity` holds the team, a `SimpleEnergyHandler` buffer (`net.neoforged.neoforge.transfer.energy`), the powered rule (`WirelessCost.powered`), and overrides the protected `ship(ServerLevel)` hook to move items of its shipments every tick to a receiving station, paid by the receiver. `ServusStorageEvents` registers `Capabilities.Energy.BLOCK` for it.
- `WarehouseNetwork.shipWireless` and `shipThrough` plan picks into one of the network's Wireless Packing Stations of the team; `wirelessTeams` / `wirelessStationTeams` feed `SiteRegistry.updateWireless`, which writes `SiteRecord.wireless` and `unpowered`.
- `RemoteTransfers` lists sites with a station of the team, picks the receiving station, refuses with `no_wireless_here`, `no_power_here`, `no_power_there`, `site_asleep`, holds wakes through `SiteWakes.hold`. `TerminalRequests` shows `WAKING` and `NO_POWER`.
- The controller (`WarehouseControllerBlockEntity`) has `ChunkLoaderUpgradeSlots extends UpgradeSlots` (Foundation slot base, own `isItemValid`), a team, the span and `refreshSiteLamp`.

## Goals / Non-Goals

**Goals:**
- One antenna per site (the controller), one FE buffer per site, Packing Stations as the only doors between rails and any line.
- Reuse the shipment, ledger, pick planning, request, wake hold and terminal view code of `m11-wireless-packing-station`; move, do not rewrite.

**Non-Goals:**
- Any FE use besides wireless (shuttles, wakes, crafting): `m11-warehouse-power`.
- A general priority between consumers of the buffer: only wireless draws from it in this change.

## Decisions

### The controller's FE buffer
`ControllerEnergy extends SimpleEnergyHandler` in `storage/.../controller/`, built like the old `StationEnergy`: capacity `controller_energy_capacity`, max insert = capacity, max extract 0 (so `EnergyView.canExtract` is false and the Utility Duct's `EnergyChannelNetwork` only ever treats the controller as a sink), `onEnergyChanged` calls `setChanged` and the controller's `powerChanged()`. Package methods `stored()`, `spend(long)` (the owner spends regardless of the extract limit, like `MachineEnergyStorage.consume`), `setStored(int)` for load and GameTests. Saved as `Energy` in the controller's tag; not added to the item's components, so a dismantled controller comes back empty (the same rule as the span).

Capability: `event.registerBlockEntity(Capabilities.Energy.BLOCK, ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), (controller, side) -> controller.energy())` in `ServusStorageEvents.onRegisterCapabilities`, the call the Wireless Packing Station uses today (`ServusStorageEvents` line 101) and the Foundation machines use for the Chunk Loader and Coal Generator (`MachineEvents` lines 46 and 62). The duct's arm appears because `UtilityDuctBlockEntity` asks `Capabilities.Energy.BLOCK` on the neighbour (line 288), and `EnergyChannelNetwork` pushes through a `BlockCapabilityCache` of the same capability. No new Foundation API is needed. `m11-warehouse-power` reuses this buffer unchanged and adds draws.

Rejected: putting the buffer on a separate "power port" block (two connections per site), or using the Foundation `MachineEnergyStorage` (it lives in `foundation.machine`, a machine concern; the controller is no machine and needs no work ticks).

### Wireless Upgrade item and slot
`WirelessUpgradeItem` (plain item with tooltip, like `ChunkLoaderUpgradeItem`) and `WirelessUpgradeSlots extends UpgradeSlots` with one slot and `isItemValid` only for that item, stack limit 1. Slot count is fixed at one because the upgrade is a switch, not a balance number; more upgrades would add nothing (decided: presence only, no range tiers in this change). Saved as `WirelessUpgrade`, dropped in `preRemoveSideEffects` next to the Chunk Loader Upgrades. Not reachable by hoppers or ducts: the controller exposes no item capability for these slots, as for the Chunk Loader Upgrade slots.

### Wireless capable rule (pure)
`site/WirelessReach.java`: `capable(boolean upgrade, long energy, double fePerItem)` = upgrade and `WirelessCost.powered(energy, fePerItem)`, plus `needs(double fePerItem)` = `WirelessCost.minimumCharge`. The controller keeps `wasCapable` and on every energy change (insert through the capability, a spend, `setStored`, load) and every upgrade slot change compares and, on a flip, tells the site record and the network (`WarehouseNetwork.wirelessChanged()`). No scan, as before.

### Who pays: the receiver
The receiving site's controller pays the wireless cost, as the Wireless Packing Station did. Reasons: the requesting site is where the player stands and where a generator runs; a woken outpost often has little power; and in `m11-warehouse-power` the sending site already pays for its shuttles and its wake. The sender must still be wireless capable (upgrade and powered), which keeps "an unpowered site is offline for wireless". Distance is now between the two controllers, not two stations, because the controller is the antenna and its position is in the site record, so the cost of a transfer is known before any station is chosen.

### Packing Stations carry wireless shipments
The wireless sending code moves from `WirelessPackingStationBlockEntity` into `PackingStationBlockEntity`:
- `Shipment` gains a `wireless` flag and the receiving site key; saved with the shipment (`SavedShipment` gains both fields). Data shipments keep running through `DataPackets.move` every `push_interval`; wireless shipments are moved every tick by a new private `shipWireless(ServerLevel)` with the old loop: `WirelessCost.movable(waiting, missing, allowance(OUT), receiver.allowance(IN), receiver.intakeRoom, paid)`, the FE spent from the receiving controller, `credit(shipment, n)`, `consume(Flow.OUT/IN)`. Both kinds use the same two `RateBudget`s, so the station's rate is shared.
- The receiver is a Packing Station at the target position that is loaded, belongs to an online warehouse whose controller is wireless capable and of the same team. A receiver that is gone or no Packing Station ends the shipment (items return through `returnOrphans`); a receiver whose site is not loaded, or either controller not capable, makes it wait (status line says why).
- `status()` no longer raises "no data line" as an alert when the warehouse's controller is wireless capable; it adds the wireless lines (`sending` to a site name, `receiver_full`, `no_power_there`, `no_power_here`).

Choice of the receiving station (pure, `packing/ReceiverChoice.java`): among the receiving site's Packing Stations, the one with the fewest open incoming wireless shipments, lowest position on a tie. Incoming counts are kept per station as shipments are added and end (runtime map on the network, rebuilt from the senders' saved shipments as they load; a miss only spreads load worse, it never loses items). Rejected: always the lowest position (one station takes everything, the others idle), most free room (changes every tick, transfers jump).

### Network side
`WarehouseNetwork.shipWireless(receiverStation, key, amount, requestId, team)` keeps its body (`shipThrough`), with candidates = all Packing Stations of the network (`packingStations()`), refused with `no_wireless_here` when the controller is not capable and `no_packing_station` when the list is empty. `wirelessTeams`, `wirelessStationTeams` and `wirelessStations(team)` go; the dirty flag now marks the controller's wireless state and "has a Packing Station" (set when the first Packing Station joins or the last leaves), written by `SiteRegistry.updateWireless(controller, upgrade, powered, packing)`.

### Site record
`SiteRecord` drops `wireless` and `unpowered` (team sets) and gains `upgrade`, `powered`, `packing` booleans (`Wireless`, `Powered`, `Packing` in the saved tag). `canTrade()` = upgrade and powered. `RemoteSite.state` is unchanged (online, sleeping, unreachable); trading is a separate flag in `SiteRow` (`trade`), which drives the orange name. The controller panel lists all sites of the team in the dimension; the terminal lists those with `upgrade` (powered or not) so it can say "no power for wireless" for one.

### Visibility only from a capable site
`SiteStatusPayload` gains `capable` and `hint` (none, no upgrade, needs FE with the amount); while not capable it carries no site rows and the screen draws the hint in the panel. `StockSyncPayload` sends no site rows while this site's controller is not capable; `StorageTerminalMenu` resets `view` to this site when the rows go empty, which also reopens the Input row (`inputOpen`). Hover lines stay name and state only (as after the play test commit `c4561ebc`). Lang keys lose every "Wireless Packing Station" text; none says "team".

Refusal keys (`RemoteTransfers.request`): `no_wireless_here` ("This site cannot trade: insert a Wireless Upgrade"), `no_power_here` ("This site has no power for wireless"), `no_packing_here` ("This site has no Packing Station"), `no_power_there` ("That site has no power for wireless"), `no_packing_there` ("That site has no Packing Station"), `no_site`, `site_asleep`, and the online site's own refusals. A sleeping site whose record says not powered is refused, since its controller cannot gain FE while asleep.

### Removing the block
Deleted: `WirelessPackingStationBlock`, `WirelessPackingStationBlockEntity`, registry entries in `ServusStorageBlocks`, `ServusStorageItems`, `ServusStorageBlockEntities`, the creative tab entry and wood registration, its energy capability registration (its item capability is already gone with `m11-drop-crates-pipe-access`), `wireless_packing_station.json` and its fields in `StorageStatsManager`, datagen entries (block state and model in `ServusStorageBlockStateProvider`, loot, recipe, mineable tag, lang) and the textures. The `PackingStationBlockEntity` constructor taking a type and the protected hooks stay only if still used; otherwise they go back to private. `StorageStatsManager` reads the new `storage/power.json` (`controller_energy_capacity`, `wireless_fe_per_item`, `wireless_fe_per_item_per_block`) with the same default fallback as other files. Nothing is released, so nothing migrates; dev worlds lose the placed blocks.

### PLAN.md
Section 2.3 item 6 and the topology bullet "Late: the Wireless Packing Station" change to the controller as antenna with the Wireless Upgrade and Packing Stations; M11 item 3 gets a note that the Wireless Packing Station was replaced by `m11-controller-wireless`. No section 5 open decision is settled by this change.

### Pure logic and tests
Pure, JUnit: `WirelessCost` (kept), `WirelessReach`, `ReceiverChoice`, `RemoteSite` (kept). Minecraft side, GameTests in `ControllerWirelessGameTests` (two sites in one test area, same team) plus manual play check.

## Decisions made during apply

Recorded on 2026-10-12 while applying, where the code differs from or adds to the decisions above:

- **Task order.** Removing the Wireless Packing Station (3.1) left the record, the network's `shipWireless`, `RemoteTransfers` and the site rows without their old types, so the 3.1 commit already brought their new form (`SiteRecord` booleans, `SiteRegistry.updateWireless(controller, upgrade, powered, packing)`, `shipWireless` over all Packing Stations, the rewritten `RemoteTransfers`, `SiteRow(key, name, state, trade)`) to keep every commit building. 4.1 to 4.4 then added the sending, the incoming counts, the terminal states and the controller GUI.
- **Refusal keys of the sending warehouse.** `WarehouseNetwork.shipWireless` names the sending site as "there", since its refusal is shown at the requesting terminal: `no_wireless_there` (no Wireless Upgrade), `no_power_there`, `no_packing_there`, plus `offline`, `out_of_stock`, `needs_shuttle` and `no_rail`. `no_wireless_there` ("That site cannot trade: its controller has no Wireless Upgrade") is a key the design did not list; the terminal uses it for a site whose record or live controller has no upgrade. `no_wireless_station` is gone.
- **No Packing Station here.** A terminal reaches its warehouse only through a Packing Station on the data line, so `no_packing_here` cannot come up from a terminal request; it stays as a guard (and makes a waiting transfer wait). `noPackingStationRefused` therefore checks the outpost side through the terminal and the network, and that the base's record has its Packing Station.
- **Incoming counts.** Instead of a map on the receiving network, `packing/WirelessIncoming` keeps the open wireless shipments per receiving station (dimension and position) at run time: a sender adds a shipment when it opens it and again when it finds its saved shipments in the ledger after loading, and removes it when it ends or when the sender is removed or unloaded; `SiteEvents` clears it when the server stops. `WarehouseNetwork.incomingWireless(pos)` reads it for `ReceiverChoice`. Reason: the receiving network may be unloaded or rebuilt while the sender ends a shipment.
- **Record state flips.** The controller tracks both its capable state and its powered state and tells the network when either flips, and every change of the Wireless Upgrade slot marks the record dirty, so `Powered` in the record stays right also for a controller without the upgrade.
- **FE in items.** The dismantle path clears the buffer before the controller is saved into its item, and `setPlacedBy` clears it again, so neither the dismantled item nor a placed copy carries FE. Saved in the block entity as `Energy`, the slot as `WirelessUpgrade`.
- **Receiver checks.** A receiving block that is no longer a Packing Station, or a controller of another team, ends the shipment (items return through `returnOrphans`). A receiver whose chunk is not loaded or whose warehouse is offline makes it wait (status "Waiting for <site> to load"), as does a receiving controller that is not capable ("<site> has no power for wireless" or "cannot trade: no Wireless Upgrade"). A sender moves items of at most one shipment per tick, oldest first, as the Wireless Packing Station did.
- **Shipment data.** `Shipment` carries `wireless` and the receiving site's key (saved as `Wireless` and `Site`); the status line names the receiving site by the name in its record. `sending()` covers both the data line delivery and a wireless move in the last second; the data line path only waits for its own delivery, so both kinds share the station but not a lock. The protected constructor and the protected hooks of `PackingStationBlockEntity` are gone.
- **Packing Station status.** Without a data line and with a capable controller the idle line reads "No data line: serves wireless transfers only" as plain information; without a data line and without a capable controller it is still the "No data line" alert. Wireless waiting reasons come before "To storage is full".
- **Controller GUI layout.** The Chunk Loader Upgrade row moved to the left edge (x 8, label left aligned) and the Wireless Upgrade slot sits at the right edge in line with the last inventory column, labelled "Wireless", each with its tooltip. The FE line ("12,000 / 100,000 FE", with a tooltip) is in the status column below the chunk lines, red while the controller holds the upgrade but not enough FE. The hint replaces the site rows in the side panel, in orange. `SiteStatusPayload` carries `energy`, `energyCapacity`, `capable`, `hint` and `needs`.
- **Wireless Upgrade recipe.** Ender eye in the middle, copper ingot above, iron ingots at the sides, redstone, iron ingot, redstone below. Placeholder texture: the shared upgrade card with a blue antenna, made by script.
- **Tests.** The cost scenario "10 items at 500 blocks cost 140 FE" is pinned in JUnit (`WirelessCostTest`); the GameTest sites are 6 blocks apart with test costs of 2 FE plus 0.5 per block, so `transferMovesItemsAndCostsReceiverFe` expects 50 FE for 10 items at the base and nothing at the outpost. `recordKeepsTradeStateWhileAsleep` checks that the record follows upgrade, power and Packing Station changes while the site is online; the record is not written while a site sleeps, so nothing more is needed for the sleeping case. `PipeAccessGameTests` uses a controller in place of the removed block, and the Packing Station input test that stood beside a Wireless Packing Station now stands beside a second Packing Station without a data line.
- **Task 7.1.** The client launch with all three mods and the play check are left to the user; build, unit tests and the Storage and Foundation GameTests are green.

## Risks / Trade-offs

- [The controller now needs a power line] A site that only trades needs a generator near the controller. Mitigation: that is the decided "one power connection per site", and `m11-warehouse-power` needs it anyway; the GUI hint says what is missing.
- [Packing Stations shared between data line and wireless] A busy data line slows transfers and the other way round. Mitigation: more Packing Stations or Speed Upgrades, as the user decided throughput should come from them; the status line names what the station does.
- [Receiver choice from a runtime count] After a restart the counts start from the loaded senders only. Mitigation: it only affects spreading, never correctness; a full receiver makes the sender wait, as before.
- [Distance from the controllers] A Packing Station far from its controller pays as if it stood at the controller. Accepted: sites are compact compared to the distances between them.

## Answered Questions

Answered by the user on 2026-10-12:
- Wireless Upgrade recipe: ender eye in the middle, iron ingots, redstone and a copper ingot.
- Sites stay limited to the same team internally; only the GUI never says "team".
