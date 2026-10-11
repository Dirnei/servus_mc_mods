## Context

`m11-controller-site` and `m11-chunk-loader-upgrade` (both applied, not archived; this change's deltas build on theirs) gave every Warehouse Controller a site with a name, team, chunk span and Chunk Loader Upgrades, a `SiteRegistry` with last known stock that outlives the site's chunks, and `SiteWakes` with `request`, `hold(site, token)` and `release(token)`.

The Packing Station (`PackingStationBlockEntity`, extends `InboundStationBlockEntity`) is the warehouse's door to the data line: shuttle and instant picks for a shipment land in its To network buffer (`outgoing`, the `DeliveryTarget`), `ship(ServerLevel)` starts one `DataPackets.move` at a time for its open `Shipment`s (progress is a `ProductionLedger.Transfer`, so a requester books it by id), arriving packets land in the inherited To storage buffer and are put away. `WarehouseNetwork.ship(target, key, amount, requestId)` picks the station with the shortest shuttle trip among stations whose data line reaches the target, plans with `RequestPlan` and starts the picks; `pickLoaded`, `pickDropped`, deferred picks and `outgoingRoom` all talk to the `PackingStationBlockEntity` at the destination. Storage Terminal requests (`TerminalRequests`) track shipments as `Tracked(packingStation, shipmentId, asked)` and book them when the ledger transfer ends.

## Goals / Non-Goals

**Goals:**
- A Wireless Packing Station that reuses the Packing Station's warehouse side, shipments, ledger transfers and pick planning.
- A remote request at a terminal that reuses the request list, booking, retry and cancel.
- Sleeping sites listed with last known stock; wakeable ones woken and held for the transfer.

**Non-Goals:**
- No cross dimension transfers, no items straight into the terminal's output slots (they arrive in the site's storage; the player takes them from the terminal as local stock).
- No SEND and KEEP rules between sites (later, per `PLAN.md`).
- No Wireless Terminal, no shuttles between sites.

## Decisions

### The Wireless Packing Station is a Packing Station without the data line
`WirelessPackingStationBlockEntity extends PackingStationBlockEntity` in the `packing` package, with its own block, item and block entity type. Capabilities are registered per block entity type, so it gets the item handler (packets and hoppers into To storage, like the Packing Station) and `Capabilities.EnergyStorage.BLOCK`, but not `DATA_LINK`, `TRANSFER_GATE` or `SUPPLIER`: it never joins a data line, and every data path (`WarehouseNetwork.ship`, `CpuOrders.network`, the CPU's collection point, the terminal's nearest Packing Station) asks `DataNetworks.networkAt` for the station, which is empty for a block that is no member. Everything on the warehouse side (rails only, put-away, picks into `outgoing`, shortfalls, deferred picks, `returnOrphans`, buffers saved and dropped, the GUI) is inherited unchanged.

`PackingStationBlockEntity.ship(ServerLevel)` becomes a protected hook; the wireless subclass overrides it and runs every tick instead of every `push_interval`, because it moves items itself instead of starting a delivery. Package private helpers on the Packing Station (`openShipments`, `delivered(shipment, n)`, `missing`) let it book its own shipments in the ledger. `rate()` is overridden to read `items_per_second` from `storage/wireless_packing_station.json`; its Speed Upgrade slot count is the Packing Station's `upgrade_slots` (the slot handler is built in the superclass constructor).

### Wireless shipments
A wireless shipment is a normal `Shipment` whose target is the receiving Wireless Packing Station's position. Each tick the sending station takes the first open shipment with items waiting in `outgoing` and moves `WirelessCost.movable(...)` of them: the minimum of what waits, what the shipment still misses, the sender's out allowance, the receiver's in allowance (both `RateBudget`s of the Packing Station, so Speed Upgrades apply), the room in the receiver's To storage buffer, and what the receiver's FE pays for. The items go into the receiver's intake (which posts the put-away event), the FE is taken from the receiver, the ledger transfer is credited and ends once nothing is missing. A receiver that is not loaded, of another team or dimension, or gone, ends the shipment; the items in `outgoing` then go back into storage through `returnOrphans`.

The receiver pays because the requesting site is where the player is and where power runs; a woken outpost often has no generator running. Distance is the straight line between the two stations in blocks. `WirelessCost` (pure) computes the cost per item, the cost of n items (rounded up) and how many items a buffer pays for.

### Network side
`WarehouseNetwork.shipWireless(receiver, key, amount, requestId, team)` shares the body of `ship` (choice by shortest shuttle trip, `RequestPlan`, refusals, `startJobs`) through a private `shipThrough(candidates, ...)`; its candidates are the network's Wireless Packing Stations of the team. `wirelessTeams()` lists the teams of the network's Wireless Packing Stations; a membership change marks it dirty and the next tick writes it into the site record (`SiteRegistry.updateWireless`), so a sleeping site keeps it.

### Sites as another site sees them
`RemoteSite.state(online, capacityState)` (pure) gives `ONLINE`, `SLEEPING` (asleep, can wake) or `UNREACHABLE` (asleep, no upgrade or over capacity). Online means the controller's chunk is loaded and its network has exactly one controller. The terminal and the controller list the team's other sites in the same dimension from the registry; the terminal only those with a Wireless Packing Station of the team. The team is that of the first reachable warehouse's controller, not the player's, so a site's terminals agree and GameTest mock players work.

### Terminal: site view and remote requests
The menu keeps a `view` (empty for this site, else a site key) set by `SelectSitePayload`; the title area of the screen becomes the selector (left click next, right click previous, tooltip). `StockSyncPayload` gains the site list (`SiteRow`: key, name, state), the view and whether the shown stock is live; for a remote view `entries` are the site's index (online) or its record's last known stock (asleep), craftables and rules are empty, and the screen greys every entry while the view is asleep. Pull to cursor and the craft dialog do nothing for a remote view.

A request while viewing another site goes to `RemoteTransfers.request`: it finds this site's Wireless Packing Station of the team (the first reachable warehouse, lowest position station), makes a `Request` marked with the site key (`remote`, saved with the request), and ships: online, through `shipWireless` of the remote network, tracked like any shipment (`Tracked` with the remote station's position); asleep and wakeable, `SiteWakes.hold` with the token `RemoteHold(terminal, requestId)` and the request waits (state `WAKING`); unreachable, refused. The terminal's existing retry ships again once the site is online. While a remote request is open its hold stays; it is released when the request leaves the list (done, cancelled, the terminal removed). Holds are runtime only; after a restart the retry takes the hold again. A site that just woke may not have joined its stations or indexed its storage when the retry asks it: a refusal then counts only after three retries (about six seconds); a site that was already online answers at once, and a request for an item it does not hold is refused right away.

Booking: a remote request counts the transfer's delivered items as delivered (they do not come to the output), so it ends when everything arrived at this site. Remote requests are skipped when the output slots credit arrivals and when automation asks what the terminal waits for. The row state adds `WAKING`; `SENDING` comes from the remote station's `isSending`. Every lookup of the remote station checks `isLoaded` first, so the terminal never loads the other site's chunk.

### Controller site list
`SiteStatusPayload` gains the `SiteRow` list; the screen draws it in a side panel right of the GUI (name, coloured state mark, a wireless mark), so the main layout stays.

### Block, recipe, look
Recipe (datagen, wood recipe): ender eye above, iron ingots at the sides, the Packing Station in the middle, planks below with redstone in the centre. Placeholder textures: the Packing Station's with a purple tint and an antenna. Mineable with a pickaxe like the Packing Station.

## Risks / Trade-offs

- [Items arrive in storage, not at the terminal] One more step for the player; it keeps the wireless hop independent of the data line and matches the decision that a request arrives at the requesting site. Forwarding to the output can follow.
- [Subclassing the Packing Station] Code that checks `instanceof PackingStationBlockEntity` also sees the wireless one; every data path filters by data line membership, which the wireless station never has. GameTests cover a data request next to a wireless station.
- [A site woken for a transfer stays awake while the request is open] A request whose stock never arrives (missing) keeps the hold; cancelling releases it. The retry only ships when stock exists.
