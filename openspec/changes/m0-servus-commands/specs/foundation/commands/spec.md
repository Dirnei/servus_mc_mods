## ADDED Requirements

### Requirement: One /servus root with a branch per mod
Every Servus mod SHALL register its commands as one branch of a shared `/servus` root named after the mod: `/servus foundation`, `/servus shuttles`, `/servus alchemy`, and later `/servus delicacies` and `/servus brewery`. Each branch SHALL work when the other mods are not installed. Permissions SHALL be set per branch or command, never on the shared root, so commands open to every player stay usable. The old roots `/servus_foundation`, `/servus_warehouse` and `/servus_alchemy` SHALL no longer exist.

#### Scenario: Only Foundation installed
- **WHEN** a server runs Foundation without the other mods
- **THEN** `/servus foundation` works and `/servus shuttles` is unknown

#### Scenario: Mixed permissions
- **WHEN** a player without operator permission types `/servus `
- **THEN** only commands open to every player, such as `/servus alchemy journal`, are suggested

### Requirement: Data move test command
An operator SHALL be able to start a data line move with `/servus foundation data move <from> <to> <item> <count> [dimension]`, where `<from>` and `<to>` are block positions in the given dimension (the source's own dimension, or the Overworld for RCON and the console, when left out); the command SHALL report the delivery id, or why it failed.

#### Scenario: Command
- **WHEN** an op runs the command from a Crafting Station holding 8 sticks in its outputs to a Storage Terminal on one data line with 8 sticks
- **THEN** chat shows the delivery id and the sticks arrive at the terminal

#### Scenario: Nether over RCON
- **WHEN** the command arrives over RCON with `minecraft:the_nether` and two loaded members of a Nether data line
- **THEN** the move starts in the Nether

### Requirement: Every mod ships an Edict catalog of its commands
Each Servus mod with commands SHALL ship a catalog of them in the Edict format (version 1.0.0 of the specification at edict.leberkas.org/spec) inside its jar, valid against the Edict catalog schema. Every command SHALL be listed with its fixed words, description, category, risk level, whether it works over RCON, its parameters in order with type and description, at least one example and the kind of response. A test SHALL fail when a command registered by the mod is missing from its catalog, when the catalog lists a command the mod does not register, or when the parameters of a listed command differ from the registered ones.

#### Scenario: Forgotten command
- **WHEN** a developer adds `/servus shuttles wipe` and does not add it to the Shuttles catalog
- **THEN** the catalog test fails naming `shuttles wipe`

#### Scenario: Stale catalog
- **WHEN** the catalog lists `shuttles sites` but the mod no longer registers it
- **THEN** the catalog test fails naming `shuttles sites`

### Requirement: Docs Commands page from the released catalogs
The docs site SHALL have a Commands page generated from the mods' catalogs, never written by hand. For each released mod it SHALL read the catalog as it is at that mod's release tag (the version of the mod's latest released CHANGELOG section), and SHALL show per command its signature built by the Edict signature rules, description, parameters, risk level, whether it works over RCON and its examples. A mod whose release has no catalog SHALL get no section. The docs build SHALL fail when a catalog it reads is not valid against the Edict schema, and the docs SHALL be rebuilt when a release tag is pushed.

#### Scenario: Unreleased command hidden
- **WHEN** main's Shuttles catalog lists a command that the latest Shuttles release does not have
- **THEN** the Commands page does not show it

#### Scenario: Release updates the page
- **WHEN** the tag `shuttles-v0.8.0` is pushed with a new command in its catalog
- **THEN** the docs are rebuilt and the Commands page shows the command
