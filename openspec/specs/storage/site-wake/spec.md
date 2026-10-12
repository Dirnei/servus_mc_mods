# storage/site-wake Specification

## Purpose
TBD - created by archiving change m11-chunk-loader-upgrade. Update Purpose after archive.

## Requirements

### Requirement: Every site leaves a record
The world SHALL keep a record of every Warehouse Controller: its dimension and position, its display name, its team, its chunk span, its Chunk Loader Upgrade count and its last known stock (every indexed item with its count). The record SHALL be refreshed while the controller's network is online: name, span and upgrades whenever they change, the stock at most every `site_snapshot_interval` ticks (example 100, `warehouse.json`). The record SHALL stay when the controller's chunk unloads and SHALL be removed when the controller is broken or dismantled. Records SHALL survive a restart.

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
A site SHALL NOT wake without a Chunk Loader Upgrade or while its span exceeds the chunks its upgrades cover; the request SHALL wait with the reason "no Chunk Loader Upgrade" or "over capacity". A site that turns out larger than its capacity while awake SHALL be released at once.

#### Scenario: Over capacity
- **WHEN** a wake is requested for a site of 12 chunks with one upgrade of 9
- **THEN** no chunk is loaded and the reason is over capacity

#### Scenario: Without upgrade
- **WHEN** a wake is requested for a site without upgrades
- **THEN** no chunk is loaded and the reason is no Chunk Loader Upgrade

### Requirement: Team cap and FTB Chunks
A wake SHALL need the site's team. While awake, a site SHALL count as one active chunk loader of its team against `maxChunkLoadersPerTeam` of the Foundation Chunk Loader, and SHALL be released from that count when it goes back to sleep. A wake that would exceed the cap, a site without a team, and every wake while FTB Chunks is installed and `chunkLoaderRespectsFtbChunks` is on SHALL wait with the reason (team limit reached, no owner, use FTB Chunks) and SHALL be retried while it is wanted.

#### Scenario: Team cap
- **WHEN** a team already has 4 active Chunk Loaders with a cap of 4 and one of its sites is to be woken
- **THEN** the site does not wake and the reason is team limit reached

#### Scenario: Cap frees up
- **WHEN** one of those Chunk Loaders is broken while the wake is still wanted
- **THEN** the site wakes

#### Scenario: Counted while awake
- **WHEN** a team with no Chunk Loaders has one awake site
- **THEN** the team counts one active loader, and none after the site released its chunks

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

### Requirement: Record knows the wireless stations
A site's record SHALL also keep the teams that have a powered Wireless Packing Station in the site's warehouse and, apart from them, the teams whose stations there are all unpowered, updated while the site is online when such a station joins, leaves or its powered state changes, and kept while it sleeps.

#### Scenario: Station placed
- **WHEN** a Wireless Packing Station of a team joins an online site by rail
- **THEN** the site's record lists that team as having a Wireless Packing Station there

#### Scenario: Station loses power
- **WHEN** the only Wireless Packing Station of a team at an online site runs out of FE
- **THEN** the site's record lists that team as having only unpowered stations there, and the site stays so while it sleeps
