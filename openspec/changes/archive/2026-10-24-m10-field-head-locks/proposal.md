## Why

The shuttle job series made every warehouse job a promise: slot locks reserve the slots a put-away or pick fills or empties, put-aways go to the best bay by the put-away rule and travel cost, and cargo with nowhere to go parks in the garage instead of dropping. Field runs were left out. A supply run still counts on stock and buffer room nobody reserved, a Harvester or Spreader can be beaten to the head's buffer, harvest put-aways pick the first rack with room at no cost, leftovers wait on the home rail without the garage error state, and a supply run that cannot fit what it took puts it back into the source while ignoring what the source refused, so items can vanish.

## What Changes

- **Field Head buffer takes locks**: the seed, fertilizer and anti-corrosion parts of a Field Head buffer take slot locks like the station buffers. Because a part is a count rather than a slot, its locks only reserve: an IN lock reserves room in the part, an OUT lock reserves items in it, and both may sit on one part at once. Everyone else sees the part's room and items less what other jobs reserved.
- **Supply runs lock both ends**: when a supply run is planned it locks the items it takes in the cheapest racks holding them (OUT) and the room it fills in the head's buffer (IN). It drives to exactly those racks, takes exactly what it locked, and fills the head as its job. Its amounts come from unlocked stock above the reserve, as before.
- **Field shuttles lock what they take from the head**: a Harvester fetching seeds and a Spreader fetching fertilizer lock those items in the head's buffer (OUT) when they decide to fetch them, and take them as their job at the head.
- **Field put-away by the put-away rule**: what a Harvester, Spreader or supply run puts away (harvest, leftover fertilizer or supply, stale buffer items) goes to the racks the put-away rule ranks first, with the real travel cost from where the shuttle stands, and locks the room it fills (IN) in those racks. What a rack does not take on arrival is routed again by the same rule.
- **Field leftovers park in the garage**: cargo no rack takes parks its shuttle in the garage with the error state (red lamp, GUI line), like warehouse leftovers, and tries again when room may have appeared. The shuttle takes no other job meanwhile.
- **No ground drops on field runs**: harvest drops that do not fit an empty cargo and a seed that cannot be replanted ride along to be put away instead of popping at the plot. A supply run's take that the cargo cannot hold goes back to the rack and whatever the rack refuses rides along to be put away, so nothing vanishes. Only Recall, a lost rail and a lost way home still drop field cargo at the garage, as before.
- **GUI**: the Field Head's Stock tab washes a buffer cell that holds a lock (IN or OUT colour) and its tooltip names the shuttle and the job, like rack and station slots.
- Locks on the head are saved with it; a head saved by Shuttles 0.3.0 loads with its buffer unlocked. The orphan lock sweep covers heads on the warehouse's rails.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `shuttles/slot-locks` (from `m8-slot-locks`): "Which slots take locks" adds the Field Head buffer and how a counted part takes locks; "The GUI shows locks" adds the Field Head Stock tab.
- `shuttles/field-head`: "Supply buffer" names the locks that reserve room and items in the buffer.
- `shuttles/field-supply`: "Supply run" locks its racks and the head's buffer when planned and takes and fills as its job; leftovers follow the put-away rule.
- `shuttles/harvester`: "Field cargo" stops dropping overflow at the plot; "Seeds" locks seeds taken from the buffer; "Harvest put-away" uses the put-away rule with travel cost and locks, and parks in the garage with the error state.
- `shuttles/spreader`: "Fertilizer from stock above the reserve" locks the fertilizer it takes from the buffer; "Leftover goes back to a rack" uses the put-away rule and parks like the harvest.

## Impact

- Shuttles mod: `lock/LockedSlots` gains counted slots (IN and OUT on one slot, reservations instead of closing), per slot `insertAs`/`extractAs` and multi item `insertAllAs`/`extractAllAs`; `field/SupplyBuffer` keeps its parts in counted `LockedSlots`; `FieldHeadBlockEntity` becomes a `LockHolder`; `shuttle/FieldRuns` plans supply with locks, fetches from the head with locks and stores through the put-away rule; `ShuttleFleet` lets stranded field shuttles retry through their run; `WarehouseNetwork` gets the field host methods and sweeps head locks; `FieldHeadMenu` syncs the head's locks and `FieldHeadScreen` draws them.
- Pure logic with JUnit: counted slot room and stock, field cargo stowing.
- Saves: the head saves its locks next to its buffer; field runs save their store stops, supply sources and the blocks they hold locks at. Shuttles 0.3.0 heads and fleets load unchanged.
