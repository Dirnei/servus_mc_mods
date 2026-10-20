## Purpose

Defines the Fermenting Crock of Servus: Delicacies: a stoneware crock that ferments data driven recipes over in-game days, by hand and by automation.

## ADDED Requirements

### Requirement: Fermenting Crock
Servus: Delicacies SHALL add a Fermenting Crock, a placed block that needs no firebox. It SHALL hold four ingredient stacks, one result stack and one fluid tank (example: 4000 mB). Breaking it SHALL drop the crock and every item in it.

#### Scenario: Breaking a full crock
- **WHEN** a player breaks a crock holding Shredded Cabbage and Salt
- **THEN** the crock, the Shredded Cabbage and the Salt drop as items

### Requirement: Fermenting recipes
Fermenting recipes (`servus_delicacies:fermenting`) SHALL name counted item ingredients, an optional counted fluid, a result and a time in game ticks. They SHALL come from datapacks and be shown to clients. A recipe SHALL match when every ingredient is met by a different ingredient stack holding at least its count, every non empty ingredient stack is used by the recipe, and the tank holds the recipe's fluid amount.

#### Scenario: Recipe from a datapack
- **WHEN** a datapack adds a fermenting recipe that turns Carrots into Golden Carrots in 100 ticks
- **THEN** a crock holding Carrots seals and after 100 ticks of game time holds Golden Carrots

### Requirement: Sealing and fermenting over game time
When the crock's contents match a fermenting recipe and at least one result fits into the result stack, the crock SHALL seal (lid on) and start counting the recipe's time on the level's game time. Time SHALL keep counting while the crock's chunk is unloaded. The crock SHALL not check its contents every tick. When the time is up it SHALL take as many full batches as its ingredients, its fluid and the room in the result stack allow, put their results into the result stack, and unseal. When the contents still match after that, it SHALL seal again.

#### Scenario: Whole batch
- **WHEN** a crock holds eight Shredded Cabbage and two Salt and the shipped recipe takes four Shredded Cabbage and one Salt for four Sauerkraut
- **THEN** once the time is up it holds eight Sauerkraut and no Shredded Cabbage or Salt

#### Scenario: Leftovers stay
- **WHEN** a crock holds six Shredded Cabbage and one Salt
- **THEN** once the time is up it holds four Sauerkraut and two Shredded Cabbage, and it stays open

#### Scenario: Not yet done
- **WHEN** a sealed crock has fermented for half of the recipe's time
- **THEN** it still holds its ingredients and no result

#### Scenario: Time passes while unloaded
- **WHEN** a crock seals and its chunk is unloaded for longer than the recipe's time
- **THEN** the crock is done shortly after its chunk is loaded again

### Requirement: A sealed crock stays shut
While sealed, the crock SHALL take no items and no fluid, by hand or by automation, and SHALL give out no ingredients and no fluid. Its result stack SHALL still be taken by automation. Right clicking a sealed crock with an empty hand SHALL tell the player what it makes and about how many in-game hours are left.

#### Scenario: Refused while sealed
- **WHEN** a hopper holding Salt points into a sealed crock
- **THEN** the Salt stays in the hopper

#### Scenario: Status
- **WHEN** a player right clicks a sealed crock that makes Sauerkraut with an empty hand
- **THEN** the player is told it makes Sauerkraut and how many in-game hours are left, and the crock keeps its contents

### Requirement: Crock by hand
While open, right clicking the crock with an item some fermenting recipe takes SHALL put as much of the held stack into its ingredient stacks as fits; a filled fluid container whose fluid some fermenting recipe uses SHALL be emptied into the tank, and an empty bucket SHALL be filled from it. An empty hand SHALL take the whole result stack, or the last filled ingredient stack when there is no result.

#### Scenario: Milk from a bucket
- **WHEN** a player right clicks an open, empty crock with a Milk Bucket
- **THEN** the crock holds 1000 mB of milk and the player holds an empty Bucket

### Requirement: Crock by automation
The crock SHALL expose the standard item capability on every side, inserting only ingredients some fermenting recipe takes and extracting only the result, and the standard fluid capability for its tank.

#### Scenario: Pipes take only results
- **WHEN** a hopper below a crock pulls items while the crock holds Sauerkraut and leftover Shredded Cabbage
- **THEN** only the Sauerkraut comes out

### Requirement: Crock look
The crock SHALL look like brown stoneware with a water seal rim around its neck. Sealed it SHALL wear its lid and bubbles SHALL now and then rise from the water seal; open it SHALL show its ingredients, its fluid surface rising with the fill and its result inside the neck.

#### Scenario: Lid while fermenting
- **WHEN** a crock seals
- **THEN** its lid is on until the fermentation is done, then it is off and the result shows inside
