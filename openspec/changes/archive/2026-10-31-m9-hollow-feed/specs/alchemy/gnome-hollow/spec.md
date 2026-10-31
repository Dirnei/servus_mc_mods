## ADDED Requirements

### Requirement: The working gnome is fed principles
A vein's JSON SHALL name an optional feed: for each principle, the amount one dug item uses and its bonus. While a bound gnome works a ripe vein, a feed principle whose tank holds at least its amount SHALL count as fed. The gnome SHALL dig at the base rate times one plus the bonuses of the fed principles, and each dug item SHALL use the amount of every principle fed when it lands. Feeding SHALL never be needed: unfed, the gnome SHALL dig at the base rate and use nothing.

#### Scenario: Unfed
- **WHEN** a bound, ripe iron Hollow with a base yield time of 1,200 ticks and empty tanks runs for 2,400 ticks
- **THEN** its store holds 2 Raw Iron and its tanks stay empty

#### Scenario: Salt only
- **WHEN** a bound, ripe iron Hollow whose feed is Salt 10 mB with bonus 1, Mercury 4 mB with bonus 0.5 and Sulfur 2 mB with bonus 0.5 holds 1,000 mB Salt and nothing else, and its base yield time is 1,200 ticks
- **THEN** it digs one Raw Iron every 600 ticks and each one uses 10 mB Salt

#### Scenario: All three fed
- **WHEN** the same Hollow holds enough of all three principles
- **THEN** it digs one Raw Iron every 400 ticks and each one uses 10 mB Salt, 4 mB Mercury and 2 mB Sulfur

#### Scenario: Running dry
- **WHEN** the same Hollow fed only Salt holds 15 mB Salt
- **THEN** the next Raw Iron uses 10 mB, the 5 mB left no longer count as fed and the gnome digs at the base rate again

#### Scenario: Not in the feed
- **WHEN** a datapack vein's feed names only Salt and its Hollow holds Mercury
- **THEN** the Mercury neither speeds the gnome up nor is used up

#### Scenario: Unripe vein
- **WHEN** an unripe vein's Hollow holds Salt
- **THEN** the Salt is taken only by the ripening steps, never by the feed

#### Scenario: Full store
- **WHEN** a fed Hollow's store is full
- **THEN** no principle is used until the store has room again

### Requirement: Each shipped vein has its own feed
Every shipped vein SHALL name all three principles in its feed with its own mix: iron and copper mostly Salt, gold mostly Sulfur, quartz and redstone mostly Mercury, diamond and emerald all three in large doses. With all three fed, a shipped vein SHALL dig three times as fast as unfed (example numbers in the data).

#### Scenario: Gold wants Sulfur
- **WHEN** a ripe gold Hollow is fed Sulfur only, and another Salt only
- **THEN** the Sulfur fed gnome digs faster than the Salt fed one

#### Scenario: Three times as fast
- **WHEN** a ripe diamond Hollow has all three principles fed and no gift
- **THEN** it digs one Diamond in a third of its unfed yield time

## MODIFIED Requirements

### Requirement: Veins are data
A vein SHALL be a JSON file at `data/<namespace>/servus_alchemy/vein/<name>.json` with a yield item, the number of ripening steps, the ticks per step, the principles per step, the ticks per yielded item while unfed, an optional feed and an optional colour. Alchemy SHALL ship coal, iron, copper, gold, redstone, quartz, diamond and emerald veins; the noble veins SHALL need more principles and time than iron, and coal SHALL be the cheapest. An unfed gnome SHALL dig slowly: the shipped base yield times SHALL be at least 600 ticks for coal, 1,200 ticks for iron and copper and 4,800 ticks for diamond and emerald (example numbers in the data). The server SHALL send every player the veins' ids, colours and yields at login and after a reload.

#### Scenario: Iron
- **WHEN** the shipped data holds an iron vein with yield Raw Iron
- **THEN** a Hollow planted with a Seed of Iron yields Raw Iron once ripe

#### Scenario: Slow unfed diamond
- **WHEN** a bound, ripe diamond Hollow with empty tanks and no gift runs for an hour of game ticks
- **THEN** its store holds about 15 Diamonds, and with all three principles fed and a gift about 90

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

#### Scenario: A vein without a feed
- **WHEN** a datapack vein names no feed
- **THEN** it loads, and its gnome digs at its base rate and uses no principles

### Requirement: A bound gnome works forever
A Hollow with a ripe vein and a bound gnome SHALL put one yield item into its internal store every vein yield time, shortened by its feed and any gift, while the store has room. It SHALL use principles only as its feed says, and SHALL never stop for lack of them. The store SHALL hold up to the Hollow's item capacity from datapack data (example 64 items); a full store SHALL stop the gnome until it is emptied. The Hollow SHALL never push its store anywhere.

#### Scenario: Raw iron
- **WHEN** a bound, unfed iron Hollow with a yield time of 1,200 ticks runs for 2,400 ticks
- **THEN** its store holds 2 raw iron and no neighbouring chest gets any

#### Scenario: Full store
- **WHEN** the store of a bound iron Hollow holds 64 raw iron
- **THEN** the gnome digs nothing more until a player takes the store

### Requirement: Seen only under Elemental Sight
A Gnome Hollow SHALL look like a plain stone block. Only while the local player has Elemental Sight SHALL the client draw the Hollow's true face over it: a darkened, cracked block. Once a gnome is bound, a cave entry SHALL open on the side the Hollow faces; before that, that side is cracked rock like the others. An empty Hollow SHALL show dry, dark cracks. A planted Hollow SHALL fill its cracks with the vein's material from the start of each crack, as far as its ripeness, glowing and slowly flowing in the vein's colour; near ripe, nodules SHALL break out of the cracks, and crystals SHALL line the cave mouth of a bound Hollow. The material of a ripe vein SHALL flow faster the more its gnome is fed, and faster again while the gnome is gifted. The client SHALL draw no item icons for the vein. The server SHALL not change any block for this. Picking the block without the sight SHALL give Stone.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow
- **THEN** they see a plain stone block

#### Scenario: Empty under the sight
- **WHEN** a player with Elemental Sight looks at a Hollow without a gnome
- **THEN** they see dark dry cracks on every side, with no glow and no cave entry

#### Scenario: Ripening under the sight
- **WHEN** a planted iron Hollow goes from half ripe to ripe while a player with Elemental Sight watches
- **THEN** the iron coloured material creeps further along the cracks until they are full

#### Scenario: Fed flows faster
- **WHEN** a player with Elemental Sight watches a ripe Hollow while Salt is poured into it for an unfed gnome
- **THEN** the material in its cracks starts to flow faster

#### Scenario: The Hollow opens
- **WHEN** a player with Elemental Sight binds a gnome to an empty Hollow
- **THEN** the rock on the Hollow's facing side crumbles away into a cave entry, with dry cracks until he is given a seed

#### Scenario: Pick block
- **WHEN** a creative player without Elemental Sight picks a Gnome Hollow
- **THEN** they get Stone

### Requirement: Disguised in Jade
With Jade installed, a player without Elemental Sight looking at a Gnome Hollow SHALL see the tooltip of plain Stone, name and icon. A player with Elemental Sight SHALL see the Gnome Hollow with its vein's yield, its ripeness or that a gnome is at work, the store with its capacity, and the principles in its tanks. For a ripe vein with a bound gnome, each principle of the feed SHALL be marked fed or hungry, and the time the gnome takes per item at its current feed and gift SHALL be shown.

#### Scenario: Without sight
- **WHEN** a player without Elemental Sight looks at a working Hollow with Jade
- **THEN** Jade shows Stone with the stone icon and nothing about a vein

#### Scenario: With sight
- **WHEN** a player with Elemental Sight looks at a bound iron Hollow holding 12 raw iron
- **THEN** Jade shows Gnome Hollow, Raw Iron, that a gnome is at work and the store 12 / 64

#### Scenario: Feed in Jade
- **WHEN** a player with Elemental Sight looks at a ripe, bound iron Hollow fed Salt only, with an unfed yield time of 60 s
- **THEN** Jade marks Salt fed and Mercury and Sulfur hungry, and shows one item per 30 s
