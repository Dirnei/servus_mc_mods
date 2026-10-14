## MODIFIED Requirements

### Requirement: Seeding with a sample
A player with Elemental Sight who uses an item on a Gnome Hollow without a vein SHALL seed it when the item matches a vein whose chapter the owner's team has finished; one item SHALL be used up (not in creative mode) and the Hollow SHALL hold that vein at ripeness 0. A Hollow that already holds a vein, or an item that matches no open vein, SHALL seed nothing, and the player SHALL read why on the action bar.

#### Scenario: Before Villach
- **WHEN** a player of a team that has not finished Villach uses raw iron on its Hollow under Elemental Sight
- **THEN** the Hollow stays empty, the raw iron stays in the hand and the player reads that this ore does not take root yet

#### Scenario: After Villach
- **WHEN** a player of a team that finished Villach uses raw iron on its empty Hollow under Elemental Sight
- **THEN** the Hollow holds an iron vein at ripeness 0 and one raw iron is used up

### Requirement: A bound gnome works forever
A Hollow with a ripe vein and a bound gnome SHALL put one yield item into its internal store every vein yield time while the store has room, without using anything. The store SHALL hold up to the Hollow's item capacity from datapack data (example 64 items); a full store SHALL stop the gnome until it is emptied. The Hollow SHALL never push its store anywhere.

#### Scenario: Raw iron
- **WHEN** a bound iron Hollow with a yield time of 200 ticks runs for 400 ticks
- **THEN** its store holds 2 raw iron and no neighbouring chest gets any

#### Scenario: Full store
- **WHEN** the store of a bound iron Hollow holds 64 raw iron
- **THEN** the gnome digs nothing more until a player takes the store

### Requirement: Binding a gnome
A ripe vein without a gnome SHALL be bound when a player with Elemental Sight uses a Salt Crystal on the Hollow; the crystal is used up. Under the sight, before the vein is ripe or when a gnome already works it, nothing SHALL happen and the player SHALL read why. Without the sight nothing SHALL happen and the player SHALL read nothing, as with stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight uses a Salt Crystal on a ripe Hollow
- **THEN** no gnome is bound, the crystal stays in the hand and no message appears

#### Scenario: Not ripe
- **WHEN** a player with Elemental Sight uses a Salt Crystal on a Hollow whose vein is not ripe
- **THEN** no gnome is bound and the player reads that the vein is not ripe yet

### Requirement: Seen only under Elemental Sight
A Gnome Hollow SHALL look like a plain stone block. Only while the local player has Elemental Sight SHALL the client draw ore buds of the vein's yield around it, more and larger as it ripens, and a gnome at work once one is bound. The server SHALL not change any block for this. Picking the block without the sight SHALL give Stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow
- **THEN** they see a plain stone block

#### Scenario: Pick block
- **WHEN** a creative player without Elemental Sight picks a Gnome Hollow
- **THEN** they get Stone

## REMOVED Requirements

### Requirement: Principles by pipe or bucket
**Reason**: The Hollow takes no pipes; its principles come only by hand under Elemental Sight (and later from the Principle Dropper above it, which drips without touching it).
**Migration**: Replaced by "Principles by bucket under the sight". Pipes that fed a Hollow stop connecting; pour buckets by hand.

### Requirement: Hollow production
**Reason**: The data line reaches grown ore only through the Receptaculum and its homunculus (`m12-receptaculum`); a Hollow is no producer.
**Migration**: None until the Receptaculum exists; Crafting CPU orders no longer reach Hollows.

## ADDED Requirements

### Requirement: Principles by bucket under the sight
The Hollow SHALL hold one internal tank per principle (example 4,000 mB). A player with Elemental Sight who uses a principle bucket on it SHALL pour the bucket into that principle's tank and get the empty bucket back (the full bucket stays in creative mode). A full tank SHALL take nothing. The tanks SHALL never give fluid back.

#### Scenario: Bucket
- **WHEN** a player with Elemental Sight uses a Salt bucket on a Hollow
- **THEN** the Salt tank gains 1,000 mB and the player holds an empty bucket

### Requirement: Taking the store by hand
A player with Elemental Sight who uses an empty hand on a Hollow SHALL get the whole store into the inventory in stacks; what does not fit SHALL drop at the player. With an empty store the player SHALL read how the vein is doing instead: no vein, the ripeness, or a gnome at work.

#### Scenario: Take raw iron
- **WHEN** a player with Elemental Sight and room in the inventory uses an empty hand on a Hollow holding 10 raw iron
- **THEN** the player gets 10 raw iron and the store is empty

### Requirement: Stone without the sight
Without Elemental Sight a right click on a Gnome Hollow SHALL do nothing to the Hollow, whatever the player holds, and SHALL show no message: a held block is placed against it and a held bucket acts as on stone.

#### Scenario: Bucket without sight
- **WHEN** a player without Elemental Sight uses a Salt bucket on a Hollow
- **THEN** the Salt tank of the Hollow stays as it was

### Requirement: No screen, pipes or capabilities
A Gnome Hollow SHALL open no screen and SHALL expose no item, fluid, energy or production capability on any side, so pipes, ducts and the data line do not connect to it and nothing is pulled from it or pushed into it.

#### Scenario: Pipe beside a Hollow
- **WHEN** a Utility Duct with an Item Pipe and a Fluid Pipe is placed beside a Hollow
- **THEN** the duct shows no connection toward the Hollow

### Requirement: Breaking the Hollow
Breaking a Gnome Hollow by a player with Elemental Sight SHALL drop a fresh, empty Gnome Hollow and the items in its store; vein, ripeness, gnome and principles SHALL be lost. Breaking it in any other way SHALL drop what stone drops: Cobblestone, or Stone with Silk Touch; vein, gnome, principles and store SHALL be lost.

#### Scenario: Break with sight
- **WHEN** a player with Elemental Sight breaks a working Hollow with a pickaxe while its store holds 5 raw iron
- **THEN** one empty Gnome Hollow and 5 raw iron drop

#### Scenario: Break without sight
- **WHEN** a player without Elemental Sight breaks a working Hollow with a stone pickaxe
- **THEN** one Cobblestone drops and nothing else

#### Scenario: Silk Touch without sight
- **WHEN** a player without Elemental Sight breaks a Hollow with a Silk Touch pickaxe
- **THEN** one Stone drops

### Requirement: Disguised in Jade
With Jade installed, a player without Elemental Sight looking at a Gnome Hollow SHALL see the tooltip of plain Stone, name and icon. A player with Elemental Sight SHALL see the Gnome Hollow with its vein's yield, its ripeness or that a gnome is at work, the store with its capacity, and the principles in its tanks.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow with Jade
- **THEN** Jade shows Stone with the stone icon and nothing about a vein

#### Scenario: With sight
- **WHEN** a player with Elemental Sight looks at a bound iron Hollow holding 12 raw iron
- **THEN** Jade shows Gnome Hollow, Raw Iron, that a gnome is at work and the store 12 / 64
