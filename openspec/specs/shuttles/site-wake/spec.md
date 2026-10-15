# shuttles/site-wake Specification

## Purpose
TBD - created by archiving change m11-chunk-loader-upgrade. Update Purpose after archive.

## Requirements

### Requirement: Every site leaves a record
The world SHALL keep a record of every Warehouse Controller: its dimension and position, its display name, its team, its chunk span, its Anchor Upgrade count and its last known stock (every indexed item with its count). The record SHALL be refreshed while the controller's network is online: name, span and upgrades whenever they change, the stock at most every `site_snapshot_interval` ticks (example 100, `warehouse.json`). The record SHALL stay when the controller's chunk unloads and SHALL be removed when the controller is broken or dismantled. Records SHALL survive a restart.

#### Scenario: Site sleeps
- **WHEN** a player leaves an outpost holding 40 iron ingots and its chunks unload
- **THEN** the record still names the outpost and lists 40 iron ingots

#### Scenario: Controller broken
- **WHEN** the outpost's controller is broken
- **THEN** its record is gone

### Requirement: Wake loads the whole site
A request to wake a site SHALL load every chunk of the site's recorded span with a ticking chunk ticket owned by the controller's position, and SHALL keep them loaded while the wake is wanted or the site's own work runs: shuttles out on jobs, jobs waiting for a shuttle, items waiting in an inbound buffer, and deliveries held for a destination. When none of these remain, the site SHALL stay loaded for `wake_grace_seconds` (example 30, `warehouse.json`) and then release every chunk it loaded. New work during the grace time SHALL keep it awake. The warehouse network itself SHALL still never load a chunk; only a wake does.

#### Scenario: Wake and release
- **WHEN** a site of 3 chunks with enough upgrades is woken and nothing else happens
- **THEN** all 3 chunks load, stay loaded for the grace time, and are released afterwards

#### Scenario: Shuttles keep it awake
- **WHEN** a woken site has a shuttle bringing items from a rack to a station
- **THEN** the site stays loaded until the shuttle is done and the grace time has passed

### Requirement: Hard limit
A site SHALL NOT wake without a Anchor Upgrade or while its span exceeds the chunks its upgrades cover; the request SHALL wait with the reason "no Anchor Upgrade" or "over capacity". A site that turns out larger than its capacity while awake SHALL be released at once.

#### Scenario: Over capacity
- **WHEN** a wake is requested for a site of 12 chunks with one upgrade of 9
- **THEN** no chunk is loaded and the reason is over capacity

#### Scenario: Without upgrade
- **WHEN** a wake is requested for a site without upgrades
- **THEN** no chunk is loaded and the reason is no Anchor Upgrade

### Requirement: Team cap and FTB Chunks
A wake SHALL need the site's team. While awake, a site SHALL hold every chunk of its span in the team's load of the Foundation Ender Anchor cap `maxLoadedChunksPerTeam`, counted once with the chunks the team's anchors and other sites already hold, and SHALL leave the load when it goes back to sleep. A wake whose span would push the team's load over the cap, a site without a team, and every wake while FTB Chunks is installed and `enderAnchorRespectsFtbChunks` is on SHALL wait with the reason (team limit reached, no owner, use FTB Chunks) and SHALL be retried while it is wanted. A wake SHALL never load part of its span.

#### Scenario: Team cap
- **WHEN** a team's anchors hold 45 chunks with a cap of 49 and one of its sites spanning 6 chunks is to be woken
- **THEN** the site does not wake and the reason is team limit reached

#### Scenario: Cap frees up
- **WHEN** one of those anchors is broken while the wake is still wanted
- **THEN** the site wakes

#### Scenario: Counted while awake
- **WHEN** a team without anchors has one awake site spanning 3 chunks
- **THEN** the team's load is 3 chunks, and 0 after the site released its chunks

#### Scenario: Overlap with an anchor
- **WHEN** an anchor of the team already holds 2 of the 3 chunks of a site that wakes
- **THEN** the team's load grows by 1 chunk

### Requirement: Wakes do not survive a restart
Wake tickets SHALL be dropped when a world loads, and team registrations made by wakes SHALL be cleared when the server stops and when it starts, so a restart never leaves a site loaded or a team slot taken.

#### Scenario: Restart while awake
- **WHEN** the server stops while a site is awake and starts again
- **THEN** none of the site's chunks is force loaded and its team counts no loader for it

### Requirement: Wake command
`/servus_warehouse wake <pos>` SHALL request a wake for the site whose controller is at the position in the command's dimension and SHALL report whether it woke, why it waits, or that there is no site there. `/servus_warehouse sites` SHALL list every site record with its name, dimension and position, team, chunks and capacity, whether it is awake, and how many item types its last known stock holds.

#### Scenario: Op wakes an outpost
- **WHEN** an op runs the wake command on the controller position of a sleeping outpost with enough upgrades
- **THEN** chat says the outpost wakes and its chunks load

#### Scenario: Op lists sites
- **WHEN** an op runs the sites command with two sites recorded
- **THEN** chat lists both with their names and chunk counts

### Requirement: Record knows whether the site can trade
A site's record SHALL also keep whether its controller holds a Wireless Upgrade, whether the controller is powered for wireless, and whether the site has a Packing Station. The record SHALL be updated while the site is online when the upgrade is inserted or taken, when the powered state flips, and when the first Packing Station joins or the last one leaves, and SHALL be kept while the site sleeps. A sleeping site's controller keeps its FE, so the record stays right while it sleeps.

#### Scenario: Upgrade inserted
- **WHEN** a Wireless Upgrade goes into the controller of an online site with FE
- **THEN** the site's record says it holds the upgrade and is powered for wireless

#### Scenario: Controller loses power
- **WHEN** the controller of an online site runs out of FE and its chunks unload
- **THEN** the site's record says it is not powered for wireless, and stays so while the site sleeps

#### Scenario: Last Packing Station removed
- **WHEN** the only Packing Station of an online site is broken
- **THEN** the site's record says it has no Packing Station

### Requirement: Waking costs FE per chunk
While a wake holds a site loaded, awake or in its grace time, the woken site's controller SHALL pay `wake_fe_per_chunk_per_tick` (example 2, `storage/power.json`) for every chunk of the site each tick. A site loaded by players or other chunk loaders without a wake SHALL pay nothing for being loaded. When a tick of a wake cannot be paid, the wake SHALL end at once and release every chunk it loaded, with the reason no power. A refused draw of another consumer (a shuttle, a Crafting Station) SHALL NOT end the wake.

#### Scenario: Nine chunks
- **WHEN** a site of 9 chunks is woken for 30 seconds with 2 FE per chunk and tick
- **THEN** its controller pays 18 FE per tick, 10,800 FE in total

#### Scenario: Runs dry while awake
- **WHEN** a woken site's controller cannot pay the next tick
- **THEN** the site's chunks are released and its wake line says no power

### Requirement: No wake without power
A site SHALL NOT wake unless its controller holds at least the FE for one second awake: chunks times `wake_fe_per_chunk_per_tick` times 20. For a sleeping site the FE SHALL be taken from its record; a request SHALL then wait with the reason "no power". The site record SHALL keep the FE its controller held when its chunk unloaded, refreshed with the stock snapshot while online.

#### Scenario: Not enough stored
- **WHEN** a site of 9 chunks sleeps with 100 FE in its controller and 2 FE per chunk and tick
- **THEN** a wake is refused with the reason no power and no chunk is loaded

#### Scenario: Enough stored
- **WHEN** the same site slept with 5,000 FE
- **THEN** it wakes and its chunks load
