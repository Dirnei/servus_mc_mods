## MODIFIED Requirements

### Requirement: Loaded while powered
An active Ender Anchor SHALL keep every chunk of its area loaded and ticking and SHALL spend its `energy_per_tick` for every chunk of its area each tick while active (example 10 FE per chunk, `machine/ender_anchor.json`), whether or not another anchor holds some of those chunks too. Without enough energy for a tick it SHALL release its whole area until energy returns.

#### Scenario: Overnight
- **WHEN** a powered Ender Anchor stands in a chunk with producing machines and no player is nearby
- **THEN** the machines keep producing

#### Scenario: Power lost
- **WHEN** the anchor's buffer runs dry
- **THEN** every chunk of its area is released and machines stop until power returns

#### Scenario: Cost of a full square
- **WHEN** an anchor with two upgrades is active at 10 FE per chunk
- **THEN** it spends 250 FE per tick

### Requirement: Per team cap
A team's load SHALL be the number of distinct chunks held by its active anchors and its awake warehouse sites, each chunk counted once however many of them hold it. The load SHALL never exceed `maxLoadedChunksPerTeam` (config, example 49). An anchor whose area would push the load over the cap SHALL stay inactive with the status "team limit reached" and SHALL load none of its area. When an active anchor's area changes, the new area SHALL be checked against the load without the anchor's old area; if it does not fit, the anchor SHALL release its area and show "team limit reached". Lowering the cap SHALL NOT release anchors that are already active.

#### Scenario: Fifth anchor
- **WHEN** a team's four active anchors hold 49 chunks with a cap of 49 and the team places a fifth anchor without upgrades
- **THEN** the fifth stays inactive with the status "team limit reached"

#### Scenario: Within the cap
- **WHEN** a team's load is 14 chunks with a cap of 49 and the team places an anchor with two upgrades whose area overlaps none of them
- **THEN** the anchor becomes active and the load is 39 chunks

#### Scenario: Over the cap
- **WHEN** a team's load is 40 chunks with a cap of 49 and the team places an anchor with two upgrades
- **THEN** the anchor stays inactive with "team limit reached" and the load stays 40

#### Scenario: Fair overlap
- **WHEN** two active anchors of one team with one upgrade each stand in neighbouring chunks
- **THEN** the team's load is 12 chunks, not 18

#### Scenario: Grow while active
- **WHEN** an active anchor with one upgrade in a team with a load of 45 of 49 gets its second upgrade
- **THEN** its new area of 25 chunks is checked against a load of 36, does not fit, and the anchor releases its area with "team limit reached"

### Requirement: Tickets survive and are cleaned up
Chunk tickets SHALL survive a restart while the anchor stays active, and SHALL be released for every chunk of its area when the anchor is broken or dismantled, or when its area changes and a chunk is no longer part of it.

#### Scenario: Removed
- **WHEN** an active anchor with one upgrade is broken
- **THEN** none of its 9 chunks is force loaded by it any more

#### Scenario: Shifted
- **WHEN** an active anchor with one upgrade is shifted one chunk east
- **THEN** the three chunks on its west edge are released and three on the east are loaded

### Requirement: Crafting
The Ender Anchor SHALL be crafted at a crafting table from an ender pearl, iron ingots, copper ingots, redstone and obsidian, with obsidian in the two bottom corners, and SHALL need no research.

#### Scenario: Recipe
- **WHEN** the player crafts the Ender Anchor pattern at a crafting table
- **THEN** one Ender Anchor is produced, without an assembler

#### Scenario: Old name gone
- **WHEN** a player searches the creative inventory or JEI for "Chunk Loader"
- **THEN** nothing is found; the block is listed as "Ender Anchor" and its upgrade as "Anchor Upgrade"

## ADDED Requirements

### Requirement: Anchor area
An Ender Anchor's area SHALL be a square of chunks with radius equal to the number of Anchor Upgrades it holds: 1 chunk without upgrades, 3 by 3 with one, 5 by 5 with two. The square SHALL be centred on the anchor's chunk shifted by an offset; each axis of the offset SHALL stay within the radius, so the anchor's own chunk is always inside the area. When upgrades are taken out, the offset SHALL shrink to fit the smaller radius. The offset SHALL be saved with the anchor and SHALL travel with a dismantled anchor. Area and offset limits are pure logic.

#### Scenario: No upgrade
- **WHEN** an anchor without upgrades is active
- **THEN** exactly its own chunk is loaded

#### Scenario: Offset
- **WHEN** an anchor with one upgrade is shifted one chunk north
- **THEN** its area is the 3 by 3 square whose southern row holds the anchor's chunk

#### Scenario: Offset limit
- **WHEN** a player tries to shift an anchor with one upgrade two chunks north
- **THEN** the offset stays at one chunk north

#### Scenario: Upgrade removed
- **WHEN** an anchor with two upgrades and an offset of two chunks east loses one upgrade
- **THEN** its offset becomes one chunk east and its area 3 by 3

### Requirement: Anchor Upgrade
The Anchor Upgrade SHALL be a Foundation item crafted at a crafting table from an Ender Anchor in the middle with ender pearls, iron ingots and redstone around it (example), with a tooltip saying it widens an Ender Anchor's area and lets a warehouse site wake. The Ender Anchor SHALL have 2 slots that take only Anchor Upgrades, one per slot; upgrades SHALL drop when the anchor is broken and travel with a dismantled anchor. Hoppers and ducts SHALL NOT reach the slots. It SHALL NOT be a Foundation multiplier upgrade.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with an Ender Anchor in the middle
- **THEN** one Anchor Upgrade is produced

#### Scenario: Wrong item
- **WHEN** the player tries to put a Speed Upgrade into an anchor slot
- **THEN** the slot refuses it

### Requirement: Anchor GUI
Right clicking an Ender Anchor SHALL open its GUI with the energy bar, the two upgrade slots, four arrow buttons that shift the offset one chunk north, south, east or west (a button that would leave the limit is greyed out and does nothing), the area size ("3 x 3"), the status line, the team's load against the cap ("14 / 49 chunks") and a "Show range" toggle. The GUI SHALL refresh while open. The toggle SHALL be the viewing player's own choice, off whenever the game starts, and SHALL NOT change the anchor for anyone else.

#### Scenario: Team load
- **WHEN** a team's anchors hold 14 chunks with a cap of 49 and a player of the team opens an anchor
- **THEN** the GUI shows 14 / 49 chunks

#### Scenario: Shift
- **WHEN** the player clicks the east arrow on an anchor with one upgrade and no offset
- **THEN** the area moves one chunk east and the east arrow is greyed out

### Requirement: Range outline
While a player has "Show range" on for an anchor, that player SHALL see the area's outline in the world: the outer chunk borders of the square as a purple line at the anchor's height and a purple column at each corner from below the anchor to well above it. The outline SHALL follow upgrade and offset changes, SHALL show while the anchor is inactive too, and SHALL disappear when the toggle is turned off, the anchor is broken, or the player is far away.

#### Scenario: Show
- **WHEN** a player turns on "Show range" for an anchor with one upgrade and closes the GUI
- **THEN** a purple outline of 3 by 3 chunks with corner columns shows around the anchor

#### Scenario: Others see nothing
- **WHEN** a second player stands next to that anchor
- **THEN** the second player sees no outline
