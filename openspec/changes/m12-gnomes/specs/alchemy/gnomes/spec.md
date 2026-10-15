## ADDED Requirements

### Requirement: Gnomes are seen only under the sight
A gnome SHALL be an entity of one of three kinds: Old Miner, Lantern Bearer or House Gnome, each with its own model after the elementals sheet (red pointed hat, beard, coat, belt and boots; the Old Miner with a pickaxe, the Lantern Bearer with a lit lantern, the House Gnome with a patched hat and a Salt Crystal). A gnome SHALL be drawn only for a player with Elemental Sight, SHALL not be hurt, pushed or leashed and SHALL not despawn.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight stands next to a bound Hollow
- **THEN** they see no gnome

#### Scenario: Unhurt
- **WHEN** a player hits a gnome
- **THEN** the gnome takes no damage

### Requirement: The Old Miner works his Hollow
Binding a gnome to a Hollow SHALL bring an Old Miner out of it. He SHALL wander within a few blocks of his Hollow on its level; while his vein ripens he SHALL tend it, tapping the cracks of the Hollow, and once it is ripe he SHALL walk into the cave entry and swing his pickaxe there, coming out again now and then. Breaking his Hollow SHALL remove him. A gnome gift or a Seed used on him SHALL count as used on his Hollow.

#### Scenario: Binding
- **WHEN** a player under the sight offers a Salt Crystal to an empty Hollow
- **THEN** an Old Miner steps out of the Hollow's new cave entry

#### Scenario: Hollow broken
- **WHEN** the Hollow of an Old Miner is broken
- **THEN** the Old Miner is gone

#### Scenario: Seed to the gnome
- **WHEN** a player under the sight gives a Seed of Iron to an Old Miner whose Hollow holds no vein
- **THEN** his Hollow holds an iron vein and he starts tending it

#### Scenario: Gift to the gnome
- **WHEN** a player under the sight uses a cookie on an Old Miner
- **THEN** his Hollow is gifted as if the cookie had been used on it

### Requirement: The Lantern Bearer leads the way
When the knocking for the old gnome wakes for a player, a Lantern Bearer SHALL appear near that player, walk towards the Ancient Hollow with his lantern lit and wait at it. He SHALL leave once the old gnome is found.

#### Scenario: Following the light
- **WHEN** the knocking wakes for a player and they drink the Elixir of Sight
- **THEN** they see a Lantern Bearer walking towards the Ancient Hollow

### Requirement: The House Gnome waits at the Ancient Hollow
The old gnome at the Ancient Hollow SHALL be a House Gnome standing in front of its open face, seen only under the sight. Finding him with the journal SHALL work on the gnome as it does on the Ancient Hollow.

#### Scenario: Found on the gnome
- **WHEN** a player under the sight uses Paracelsus' Journal on the House Gnome at the Ancient Hollow
- **THEN** the old gnome is found as if the journal had been used on the Ancient Hollow
