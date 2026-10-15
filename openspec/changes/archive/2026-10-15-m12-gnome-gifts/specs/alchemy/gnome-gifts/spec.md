## Purpose

Gifts a gnome likes make him dig his vein faster for a while; without gifts he keeps the base rate.

## ADDED Requirements

### Requirement: Gifts are data
The gifts gnomes like SHALL come from datapack files `data/<namespace>/servus_alchemy/gnome_gift/<name>.json`, each naming liked items and item tags, a speed factor of at least 1 and a duration in ticks. An item SHALL use the first gift file by id that names it or one of its tags; an item that no file names SHALL be disliked. The shipped data SHALL like, for example, cookie, bread, sweet berries and glow berries at x2 for 10 minutes.

#### Scenario: Shipped gift
- **WHEN** the shipped data is loaded
- **THEN** a cookie is a liked gift with factor 2 and 12,000 ticks, and raw iron is disliked

#### Scenario: Pack adds a gift
- **WHEN** a datapack adds a gift file naming an item tag
- **THEN** every item of that tag is liked with that file's factor and duration after a reload

### Requirement: A gift speeds up the gnome
A bound gnome that has taken a gift SHALL dig at its vein's yield rate times the gift's factor until the gift's duration has run out, then at the base rate again. A gift SHALL never be needed: without one the gnome SHALL work at the base rate. A new gift while a boost runs SHALL replace it, starting the new gift's full duration with its factor; boosts SHALL NOT stack. The remaining time SHALL count down on every tick the Hollow ticks (while its chunk is loaded), also while the store is full.

#### Scenario: Faster with a gift
- **WHEN** a bound iron Hollow with a yield time of 200 ticks takes a cookie (x2) and runs for 400 ticks
- **THEN** its store holds 4 raw iron, where it would hold 2 without the gift

#### Scenario: Boost runs out
- **WHEN** the gift's duration has passed
- **THEN** the gnome digs at the base rate again and the Hollow shows no boost

#### Scenario: Refresh
- **WHEN** a gnome with 100 ticks of boost left takes another cookie
- **THEN** its boost has the full 12,000 ticks left, not 12,100

### Requirement: Giving a gift by hand
A player with Elemental Sight who uses a liked item on a Hollow with a bound gnome SHALL give it: one item SHALL be used up (not in creative mode), the boost SHALL start, and the player SHALL read on the action bar that the gnome is pleased. Under the sight, a disliked item on a bound Hollow SHALL be refused, stay in the hand and show a hint that the gnome does not want it; a liked item on a Hollow without a bound gnome SHALL stay in the hand and show that no gnome is there to take it. Without the sight nothing SHALL happen and no message SHALL appear, as with stone.

#### Scenario: Liked gift
- **WHEN** a player with Elemental Sight uses 3 cookies on a bound Hollow
- **THEN** 2 cookies stay in the hand and the gnome is boosted

#### Scenario: Disliked item
- **WHEN** a player with Elemental Sight uses a stick on a bound Hollow
- **THEN** the stick stays in the hand, no boost starts and the player reads that the gnome does not want it

#### Scenario: No sight
- **WHEN** a player without Elemental Sight uses a cookie on a bound Hollow
- **THEN** the cookie stays in the hand, no boost starts and no message appears

### Requirement: Gifts from other blocks
The Hollow SHALL offer a server side way for other blocks to give one item: it SHALL answer whether the gnome took it (taken, disliked, or no gnome) and SHALL never use up the item itself, so the caller removes one item only when it was taken. It SHALL also tell how many ticks of boost are left, so a giver can wait until a boost runs low.

#### Scenario: Offered by a block
- **WHEN** a block offers a cookie to a bound Hollow
- **THEN** the Hollow answers taken, the boost starts, and the cookie stack is unchanged until the caller shrinks it

### Requirement: The boost is kept and shown
The boost's factor and remaining time SHALL be saved with the Hollow and SHALL survive a reload. With Jade, a player with Elemental Sight looking at a boosted Hollow SHALL see the factor and the remaining time; without the sight Jade SHALL still show plain Stone. Under the sight the revealed gnome SHALL move faster while boosted.

#### Scenario: Jade under the sight
- **WHEN** a player with Elemental Sight looks at a Hollow whose gnome took a cookie 2 minutes ago
- **THEN** Jade shows a boost of x2 with 8:00 left

#### Scenario: Saved
- **WHEN** a boosted Hollow is saved and loaded again
- **THEN** it has the same factor and remaining time
