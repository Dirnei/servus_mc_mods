## ADDED Requirements

### Requirement: Amounts on filter entries
Every item filter slot and every fluid filter slot of a duct side SHALL carry an amount, none by default, in items for item entries and in mB for fluid entries. The amount SHALL be kept with the block and on dismantling, and SHALL be cleared with its slot. An entry without an amount SHALL behave as before. When two entries of one side name the same item or fluid, the first one's amount SHALL count.

#### Scenario: Default
- **WHEN** a player sets cobblestone in a filter slot
- **THEN** the slot has no amount and cobblestone passes as before

#### Scenario: Cleared with the slot
- **WHEN** a slot shows sapling with amount 64 and the player shift right clicks it
- **THEN** the slot is empty, and setting sapling again gives it no amount

### Requirement: Keep amount on giving sides
On a side that gives (an item side with Pull, a fluid side with only In, or a fluid side with In and Out acting as a source), an entry's amount SHALL be a keep amount: the duct SHALL take that item or fluid from the block only while the block holds more than the amount, and SHALL never take it below the amount. The held amount SHALL count everything of that item or fluid the block shows to that side, over all its slots or tanks.

#### Scenario: Keep 64 saplings
- **WHEN** a chest holds 300 oak saplings, its side has In and Pull with the entry "oak sapling, 64", and a composter's side on the same channel has Out
- **THEN** the duct moves 236 saplings and the chest keeps 64

#### Scenario: Overflow to the Sink
- **WHEN** a tank holds 12,000 mB of water, its side has only In with the entry "water, 8,000 mB", and a Sink on the same channel accepts fluid
- **THEN** 4,000 mB move to the Sink and the tank stays at 8,000 mB, and when a pump fills the tank again only the water above 8,000 mB moves

#### Scenario: Below the amount
- **WHEN** the chest holds 40 saplings with the entry "oak sapling, 64"
- **THEN** no sapling leaves the chest

#### Scenario: Counted over all slots
- **WHEN** a chest holds 3 stacks of 64 cobblestone in different slots and the entry of its side with Pull is "cobblestone, 100"
- **THEN** 92 cobblestone leave the chest

### Requirement: Fill up to amount on receiving sides
On a side that receives (an item side with Out receiving routed items, a fluid side with only Out, or a fluid side with In and Out acting as a sink), an entry's amount SHALL be a fill up to amount: the block SHALL receive that item or fluid only until it holds the amount. Further items or fluid SHALL go to other outputs on the channel as if this one were full.

#### Scenario: Regulate a furnace
- **WHEN** a furnace's side has only Out with the entry "coal, 16" and 64 coal arrive on the channel while a chest on a lower priority also receives
- **THEN** the furnace holds 16 coal and the other 48 go to the chest

#### Scenario: Refill as it burns
- **WHEN** the furnace burns 3 of its 16 coal
- **THEN** it receives 3 coal again

#### Scenario: Fill a boiler to a level
- **WHEN** a boiler's side has only Out with the entry "water, 4,000 mB" and a pump supplies water on the channel
- **THEN** the boiler holds at most 4,000 mB of water from the duct

### Requirement: Editing amounts
A filter slot with an amount SHALL show it in its corner, fluids in mB shortened above 1,000. Right clicking a set filter slot SHALL open an amount popup with a number field, step buttons and Clear, labelled "Keep" on a giving side, "Up to" on a receiving side and "Keep / up to" on a side that does both (an item side with Pull and Out, a fluid side with In and Out). Saving an empty field or 0 SHALL remove the amount. The slot's tooltip SHALL name the amount and its meaning.

#### Scenario: Set a keep amount
- **WHEN** the item side has In and Pull, the player right clicks the sapling slot, types 64 and saves
- **THEN** the slot shows 64 and its tooltip reads "Keep 64"

#### Scenario: Fluid amount
- **WHEN** the player sets 8000 on a water slot of a fluid side with only In
- **THEN** the slot shows "8k" and its tooltip reads "Keep 8,000 mB"

#### Scenario: Remove an amount
- **WHEN** the player opens the popup on a slot with amount 64 and presses Clear
- **THEN** the slot shows no amount and still lists its item

### Requirement: Keep logic is pure
How much may be given above a keep amount and how much may be received below a fill up to amount, given the held amount and the offered amount, SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Allowance tests
- **WHEN** the unit tests compute allowances
- **THEN** holding 300 with keep 64 allows 236 of an offer of 1,000; holding 40 with keep 64 allows 0; holding 13 with up to 16 accepts 3 of an offer of 64; no amount allows the whole offer
