## MODIFIED Requirements

### Requirement: Every site leaves a record
The world SHALL keep a record of every Warehouse Controller: its dimension and position, its display name, its team, its chunk span, its Anchor Upgrade count and its last known stock (every indexed item with its count). The record SHALL be refreshed while the controller's network is online: name, span and upgrades whenever they change, the stock at most every `site_snapshot_interval` ticks (example 100, `warehouse.json`). The record SHALL stay when the controller's chunk unloads and SHALL be removed when the controller is broken or dismantled. Records SHALL survive a restart.

#### Scenario: Site sleeps
- **WHEN** a player leaves an outpost holding 40 iron ingots and its chunks unload
- **THEN** the record still names the outpost and lists 40 iron ingots

#### Scenario: Controller broken
- **WHEN** the outpost's controller is broken
- **THEN** its record is gone

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
