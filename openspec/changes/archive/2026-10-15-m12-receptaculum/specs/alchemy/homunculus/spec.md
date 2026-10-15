## Purpose

The homunculus is Paracelsus' "faithful little helper": grown in a sealed flask on a heated Receptaculum, it harvests the hidden Gnome Hollows around it and brings the gnomes their gifts. Only Elemental Sight shows it and the chains that bind it; a Nether Star sets it free.

## ADDED Requirements

### Requirement: The Sealed Flask
The Athanor SHALL make a Sealed Flask from Crying Obsidian with Salt and Mercury at the Rubedo stage only (example 250 Salt and 500 Mercury, datapack recipe). The Sealed Flask SHALL be the item of the Vas Hermeticum block.

#### Scenario: Red fire only
- **WHEN** Crying Obsidian lies on a Citrinitas Athanor with enough Salt and Mercury
- **THEN** no Sealed Flask is made; on a Rubedo Athanor it is

### Requirement: The Vas stands only on a Receptaculum
Placing a Sealed Flask SHALL create the Vas Hermeticum block only on top of a Receptaculum; anywhere else nothing is placed and the flask stays in the hand. When the Receptaculum below is removed, the Vas SHALL break and drop its Sealed Flask. Breaking the Vas SHALL drop the Sealed Flask; the homunculus in it is lost and a new one grows from the start.

#### Scenario: On stone
- **WHEN** a player uses a Sealed Flask on the top of a Stone block
- **THEN** no block is placed and the flask stays in the hand

#### Scenario: On a Receptaculum
- **WHEN** a player uses a Sealed Flask on the top of a Receptaculum
- **THEN** a Vas Hermeticum stands on the Receptaculum

#### Scenario: Receptaculum broken
- **WHEN** the Receptaculum under a Vas is broken
- **THEN** the Vas breaks too and drops a Sealed Flask

### Requirement: The homunculus grows with Elixir
A new Vas SHALL hold a homunculus that grows for the grow time (datapack, example 48000 ticks, two in-game days). It SHALL grow only on ticks it is fed. It SHALL be fed while a bottle of Elixir is in use: when none is in use, it SHALL take one Elixir of Sight from the Receptaculum's Elixir slot, and one bottle SHALL last the Elixir time (datapack, example 12000 ticks). Without Elixir it SHALL pause and keep its growth. Its growth and the bottle in use SHALL survive a reload.

#### Scenario: Grows with Elixir
- **WHEN** a homunculus 20 ticks short of grown sits on a Receptaculum with an Elixir of Sight
- **THEN** the Elixir is taken from the slot and the homunculus is grown within a few seconds

#### Scenario: Pauses without Elixir
- **WHEN** a homunculus 20 ticks short of grown sits on a Receptaculum without Elixir and without a bottle in use
- **THEN** it stays not grown

### Requirement: The homunculus harvests the Hollows in reach
A grown homunculus that is fed SHALL, every work interval (datapack, example 100 ticks), take the stored ore out of every Gnome Hollow in its reach into the Receptaculum's yield slots, as much as the yield slots take; what does not fit SHALL stay in the Hollow. Its reach SHALL be the 5x5 area centred on the stack on the Receptaculum's level: Hollows at the Receptaculum's height whose x and z differ by at most 2. A grown homunculus without Elixir SHALL do nothing and use up nothing.

#### Scenario: In reach and outside
- **WHEN** a grown, fed homunculus works and Hollows with 5 Raw Iron each lie 2 blocks and 3 blocks east of its Receptaculum on the same level
- **THEN** the yield slots receive the 5 Raw Iron of the nearer Hollow and the farther Hollow keeps its 5

#### Scenario: Another level
- **WHEN** a Hollow with stored ore lies one block above or below the Receptaculum's level, inside the 5x5 area
- **THEN** the homunculus does not take from it

#### Scenario: Out of Elixir
- **WHEN** the last bottle in use runs out and the Elixir slot is empty
- **THEN** the homunculus stops taking ore until Elixir is added

### Requirement: The homunculus brings gifts
Every time it works, the homunculus SHALL offer a gift to every bound gnome in reach whose boost has less than the gift threshold left (datapack, example 1200 ticks): it SHALL try the gift slots in order and give one item of the first stack the gnome takes; a gift the gnome dislikes SHALL stay in its slot.

#### Scenario: A cookie for a gnome
- **WHEN** a grown, fed homunculus works, a gift slot holds 3 cookies and a bound gnome in reach has no boost
- **THEN** the gnome's boost starts and 2 cookies stay in the slot

#### Scenario: Disliked
- **WHEN** the only gift slot in use holds Cobblestone
- **THEN** the Cobblestone stays and no boost starts

### Requirement: Hidden without the sight
Without Elemental Sight, the Vas SHALL look like a glass flask with a stepped bulb, a neck, a red wax seal and murky liquid, and nothing in it. A player with Elemental Sight SHALL see the pale homunculus inside the flask, as small as its growth, and, once it is grown and not freed, a sagging chain from the Vas' neck to each Gnome Hollow in its reach. The chains SHALL follow the Hollows in reach as they are placed and removed.

#### Scenario: With the sight
- **WHEN** a player with Elemental Sight looks at a grown homunculus with two Hollows in reach
- **THEN** they see the homunculus in the flask and two chains from the neck to the two Hollows

#### Scenario: Without the sight
- **WHEN** a player without the sight looks at the same Vas
- **THEN** they see glass, murky liquid and the seal only

### Requirement: Whispers
Now and then (every one to three minutes) a grown homunculus that is not freed SHALL whisper to each player with Elemental Sight within 8 blocks: one line, picked at random from a list of whisper lines, shown in the action bar. The lines are placeholders for the storyteller, marked "[storyteller]", about warmth and a star. No other text of the mods SHALL mention the star: not the journal, not the recipe viewer, not the tooltips.

#### Scenario: A whisper
- **WHEN** a player with the sight stands near a grown homunculus for three minutes
- **THEN** at least one whisper line appears in their action bar

#### Scenario: No sight, no whisper
- **WHEN** a player without the sight stands near it
- **THEN** no whisper appears

### Requirement: Freed by a Nether Star
Using a Nether Star on the Vas of a grown homunculus that is not yet freed SHALL use up the star, break the seal and set the Vas to freed, with or without Elemental Sight. A freed Vas SHALL show no seal, for every player. A freed homunculus SHALL need no Elixir and SHALL deliver two items into the yield slots for every item it takes from a Hollow (datapack multiplier, example 2). It SHALL send the player who freed it its secret: several chat lines, placeholders marked "[storyteller]". Under Elemental Sight it SHALL sit on the rim of the open neck, with no chains. A Nether Star used on a Vas whose homunculus is still growing, or is already freed, SHALL do nothing and stay in the hand. Nothing in the mods SHALL hint at this use.

#### Scenario: Freeing
- **WHEN** a player uses a Nether Star on the Vas of a grown homunculus
- **THEN** the star is used up, the Vas shows no seal, and the player reads the secret in chat

#### Scenario: Double yield without Elixir
- **WHEN** a freed homunculus works with no Elixir and a Hollow in reach holds 5 Raw Iron
- **THEN** the Hollow is emptied and the yield slots receive 10 Raw Iron

#### Scenario: Too young
- **WHEN** a player uses a Nether Star on the Vas of a growing homunculus
- **THEN** nothing happens and the star stays in the hand

### Requirement: Numbers are data
The grow time, the work interval, the Elixir time, the gift threshold and the freed multiplier SHALL come from `data/<namespace>/servus_alchemy/homunculus/<name>.json` (the file `servus_alchemy:homunculus` wins); without a file the shipped example values SHALL apply.

#### Scenario: Faster helper
- **WHEN** a datapack sets the work interval to 20 ticks and the server reloads
- **THEN** a grown, fed homunculus takes from the Hollows every 20 ticks

### Requirement: Explained in the Workshop
The journal's Workshop SHALL have an entry "The Vas Hermeticum" that opens with the Basel and Salzburg chapter (Rubedo): how the Sealed Flask is made, that it stands only on a Receptaculum, that the homunculus grows on Elixir, what it does once grown, and Paracelsus praising his faithful little helper. It SHALL NOT call the homunculus a slave and SHALL NOT mention the Nether Star or a star.

#### Scenario: Reading the Workshop
- **WHEN** a team finished Wandering years and opens the Workshop
- **THEN** "The Vas Hermeticum" is readable and tells how to make and feed the homunculus
