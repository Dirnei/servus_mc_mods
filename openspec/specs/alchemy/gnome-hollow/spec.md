# alchemy/gnome-hollow Specification

## Purpose
The Gnome Hollow grows ore without world generation: seeded with a sample, ripened with principles and worked by a bound gnome, it yields its ore forever. Its vein and gnome are seen only under Elemental Sight.

## Requirements

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with a yield item, the number of ripening steps, the ticks per step, the principles per step, the ticks per yielded item and an optional colour. Alchemy SHALL ship coal, iron, copper, gold, redstone, quartz, diamond and emerald veins; the noble veins SHALL need more principles and time than iron, and coal SHALL be the cheapest (example numbers in the data). The server SHALL send every player the veins' ids, colours and yields at login and after a reload.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with yield Raw Iron
- **THEN** a Hollow planted with a Seed of Iron yields Raw Iron once ripe

#### Scenario: Gold before Schwaz
- **WHEN** a team that finished Villach but not Schwaz has a Seed of Gold and a Hollow with a gnome
- **THEN** the gnome plants the gold vein: the chapter no longer gates it, making the Seed of Gold on a Citrinitas Athanor does

#### Scenario: Gold after Schwaz
- **WHEN** a player under Elemental Sight gives a Seed of Gold to a Hollow with a gnome
- **THEN** the Hollow holds a gold vein at ripeness 0

#### Scenario: Ore of the tag
- **WHEN** a player under Elemental Sight uses a gold ore block on a Hollow with a gnome
- **THEN** nothing is planted: ores of a tag no longer plant veins, only Seeds do

#### Scenario: Diamond
- **WHEN** a ripe diamond vein is worked by its gnome
- **THEN** its store fills with Diamonds, and its Seed came from a Rubedo Athanor

#### Scenario: Quartz
- **WHEN** a ripe quartz vein is worked by its gnome
- **THEN** its store fills with Nether Quartz

#### Scenario: Coal and emerald
- **WHEN** a Hollow is planted with a Seed of Coal, and another with a Seed of Emerald
- **THEN** once ripe the first yields Coal and the second Emerald, and the coal vein ripens with fewer principles than the emerald vein

#### Scenario: A vein from a datapack
- **WHEN** a datapack adds a vein `mypack:tin` and an Athanor recipe for its Seed
- **THEN** the Seed of Tin can be made, shows the tin vein's colour and plants a tin vein

### Requirement: Seeding with a sample
A player with Elemental Sight who uses a Seed on a Gnome Hollow that has a gnome, or on that gnome, SHALL have the gnome plant the Seed's vein; one Seed SHALL be used up (not in creative mode) and the Hollow SHALL hold that vein at ripeness 0. If the Hollow already holds a vein of another kind, the gnome SHALL dig it out (it is lost) and plant the new one; a Seed of the vein it already holds SHALL be refused. A Hollow without a gnome SHALL take no Seed. Any other item, including raw ores and ore blocks, SHALL plant nothing. The player SHALL read on the action bar why nothing was planted.

#### Scenario: Seed of Iron
- **WHEN** a player under Elemental Sight uses a Seed of Iron on a Hollow with a gnome and no vein
- **THEN** the Hollow holds an iron vein at ripeness 0 and one Seed is used up

#### Scenario: Before Villach
- **WHEN** a player of a team that has not finished Villach uses a Seed of Iron on its Hollow with a gnome under Elemental Sight
- **THEN** the Hollow holds an iron vein: chapters no longer gate planting, the Athanor tier of the Seed's recipe does

#### Scenario: Raw ore
- **WHEN** a player under Elemental Sight uses Raw Gold on a Hollow with a gnome
- **THEN** nothing is planted, the Raw Gold stays in the hand and the player reads that the gnome wants a seed

#### Scenario: No gnome yet
- **WHEN** a player under Elemental Sight uses a Seed of Iron on a Hollow without a gnome
- **THEN** nothing is planted and the player reads that a gnome must live here first

#### Scenario: Changing the vein
- **WHEN** a player under Elemental Sight gives a Seed of Gold to the gnome of a ripe iron Hollow
- **THEN** the iron vein is gone and the Hollow holds a gold vein at ripeness 0; the store keeps what was dug

### Requirement: Ripening with principles
A planted Hollow that is not ripe SHALL, once per step time, take the step's principles from its tanks and grow one step while its gnome tends it. Without the principles it SHALL wait. After the vein's number of steps it is ripe and the gnome starts digging.

#### Scenario: One step
- **WHEN** an iron vein needing 100 mB Salt per step of 100 ticks has 150 mB Salt
- **THEN** after 100 ticks it is one step riper and 50 mB Salt remain

### Requirement: Binding a gnome
A Hollow without a gnome SHALL be bound when a player with Elemental Sight uses a Salt Crystal on it; the crystal is used up and a gnome moves in. No vein is needed for this. Under the sight, when a gnome already lives in the Hollow, nothing SHALL happen and the player SHALL read why. Without the sight nothing SHALL happen and the player SHALL read nothing, as with stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight uses a Salt Crystal on an empty Hollow
- **THEN** no gnome is bound, the crystal stays in the hand and no message appears

#### Scenario: Not ripe
- **WHEN** a player with Elemental Sight uses a Salt Crystal on an empty Hollow with no vein at all
- **THEN** a gnome is bound and the crystal is used up: binding needs no ripe vein any more

#### Scenario: Gnome already there
- **WHEN** a player with Elemental Sight uses a Salt Crystal on a Hollow that has a gnome
- **THEN** the crystal stays in the hand and the player reads that a gnome already lives here

### Requirement: A bound gnome works forever
A Hollow with a ripe vein and a bound gnome SHALL put one yield item into its internal store every vein yield time while the store has room, without using anything. The store SHALL hold up to the Hollow's item capacity from datapack data (example 64 items); a full store SHALL stop the gnome until it is emptied. The Hollow SHALL never push its store anywhere.

#### Scenario: Raw iron
- **WHEN** a bound iron Hollow with a yield time of 200 ticks runs for 400 ticks
- **THEN** its store holds 2 raw iron and no neighbouring chest gets any

#### Scenario: Full store
- **WHEN** the store of a bound iron Hollow holds 64 raw iron
- **THEN** the gnome digs nothing more until a player takes the store

### Requirement: Seen only under Elemental Sight
A Gnome Hollow SHALL look like a plain stone block. Only while the local player has Elemental Sight SHALL the client draw the Hollow's true face over it: a darkened, cracked block. Once a gnome is bound, a cave entry SHALL open on the side the Hollow faces; before that, that side is cracked rock like the others. An empty Hollow SHALL show dry, dark cracks. A planted Hollow SHALL fill its cracks with the vein's material from the start of each crack, as far as its ripeness, glowing and slowly flowing in the vein's colour; near ripe, nodules SHALL break out of the cracks, and crystals SHALL line the cave mouth of a bound Hollow. A gifted gnome SHALL make the material flow faster. The client SHALL draw no item icons for the vein. The server SHALL not change any block for this. Picking the block without the sight SHALL give Stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow
- **THEN** they see a plain stone block

#### Scenario: Empty under the sight
- **WHEN** a player with Elemental Sight looks at a Hollow without a gnome
- **THEN** they see dark dry cracks on every side, with no glow and no cave entry

#### Scenario: Ripening under the sight
- **WHEN** a planted iron Hollow goes from half ripe to ripe while a player with Elemental Sight watches
- **THEN** the iron coloured material creeps further along the cracks until they are full

#### Scenario: The Hollow opens
- **WHEN** a player with Elemental Sight binds a gnome to an empty Hollow
- **THEN** the rock on the Hollow's facing side crumbles away into a cave entry, with dry cracks until he is given a seed

#### Scenario: Pick block
- **WHEN** a creative player without Elemental Sight picks a Gnome Hollow
- **THEN** they get Stone

### Requirement: Gnome Hollow crafting
The Gnome Hollow SHALL be made on an Athanor of at least Albedo from cracked stone bricks with Salt and Mercury.

#### Scenario: Athanor craft
- **WHEN** cracked stone bricks lie on an Albedo Athanor with the principles
- **THEN** a Gnome Hollow is made

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

### Requirement: The Hollow faces its maker
A Gnome Hollow SHALL face the player who placed it, and the cave entry of a bound Hollow SHALL be on that side. Its look without the sight SHALL be the same stone for every facing.

#### Scenario: Placed from the south
- **WHEN** a player standing south of the spot places a Gnome Hollow and later binds a gnome to it
- **THEN** its cave entry, seen under Elemental Sight, opens to the south

### Requirement: Vein colours are data
Each vein SHALL name the colour its material shows under the sight in its JSON, with a default for veins that name none.

#### Scenario: Gold
- **WHEN** the shipped gold vein is planted
- **THEN** its cracks fill with a golden yellow material

### Requirement: Seeds
A Seed SHALL be one item that names its vein. It SHALL be named "Seed of" followed by the vein's name and tinted in the vein's colour. A Seed whose vein is not loaded SHALL show as an unknown seed and plant nothing.

#### Scenario: Name and colour
- **WHEN** a player hovers a Seed of Gold
- **THEN** it reads "Seed of Gold" and is drawn golden yellow

#### Scenario: Seeds can be browsed
- **WHEN** a player opens the Servus creative tab or searches JEI for "seed"
- **THEN** there is one Seed for every loaded vein, each as its own entry

### Requirement: Seeds come from the Athanor
Alchemy SHALL ship an Athanor recipe per vein that turns its raw material and principles into its Seed, at the tier of its stage: Seed of Coal from Coal, Seed of Iron from Raw Iron and Seed of Copper from Raw Copper on an Albedo Athanor; Seed of Gold from Raw Gold, Seed of Redstone from Redstone Dust and Seed of Quartz from Nether Quartz on a Citrinitas Athanor; Seed of Diamond from a Diamond and Seed of Emerald from an Emerald on a Rubedo Athanor (principles as example numbers in the data).

#### Scenario: Gold needs Citrinitas
- **WHEN** an Albedo Athanor holds Raw Gold and the principles of the Seed of Gold recipe
- **THEN** it makes no Seed of Gold, and a Citrinitas Athanor with the same contents does

#### Scenario: Coal on the white Athanor
- **WHEN** an Albedo Athanor holds Coal and the principles of the Seed of Coal recipe
- **THEN** it makes a Seed of Coal

#### Scenario: Quartz needs Citrinitas
- **WHEN** an Albedo Athanor holds Nether Quartz and the principles of the Seed of Quartz recipe
- **THEN** it makes no Seed of Quartz, and a Citrinitas Athanor with the same contents does

#### Scenario: Emerald needs Rubedo
- **WHEN** a Citrinitas Athanor holds an Emerald and the principles of the Seed of Emerald recipe
- **THEN** it makes no Seed of Emerald, and a Rubedo Athanor with the same contents does
