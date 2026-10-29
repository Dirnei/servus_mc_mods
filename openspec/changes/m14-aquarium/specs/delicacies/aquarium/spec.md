# Spec Delta

## Purpose

The aquarium turns a Foundation Fluid Tank of water into a fish farm: a floor and a lid around the tank form it, a stock of one fish species breeds while fed, powered and kept clean, and the catch leaves by hand or by any pipe.

## ADDED Requirements

### Requirement: Aquarium parts
Delicacies SHALL add the Aquarium Floor, a full block with copper cabinet sides and a gravel top, and the Aquarium Lid, a copper slab 4 px high with a light panel on its underside. Both SHALL be crafted from copper and vanilla materials, shown in the Delicacies creative tab, and drop themselves when broken.

#### Scenario: Crafting the parts
- **WHEN** a player crafts the Aquarium Floor and the Aquarium Lid recipes (for example copper ingots with gravel and kelp, and copper ingots with glowstone dust)
- **THEN** they get Aquarium Floor blocks and Aquarium Lid slabs

#### Scenario: Breaking a loose part
- **WHEN** a player breaks an Aquarium Lid that belongs to no aquarium
- **THEN** it drops one Aquarium Lid

### Requirement: Forming the aquarium
Using the Configuration Carrot on any Floor or Lid part SHALL form an aquarium when a Fluid Tank holding water or nothing stands directly on Floor blocks covering every column of its lowest layer, and Lid slabs cover every column above its top layer. The player SHALL be told that it formed and its size, or what is missing.

#### Scenario: Forming a 3 by 2 aquarium
- **WHEN** a 3 by 2 by 2 tank of water stands on six Floor blocks, six Lid slabs lie on its top and the player uses the carrot on a Lid slab
- **THEN** the aquarium forms and the player is told it formed 3 by 2 by 2

#### Scenario: A missing lid
- **WHEN** one column above the tank has no Lid slab and the player uses the carrot on a Floor block
- **THEN** nothing forms and the player is told the lid does not cover the tank

#### Scenario: Wrong fluid
- **WHEN** the tank holds lava and every part is in place
- **THEN** nothing forms and the player is told the tank must hold water

### Requirement: Unforming the aquarium
Breaking any part or tank block of a formed aquarium, dismantling a part with a sneaking carrot click, or a change of the tank's box SHALL unform the aquarium. Its slot contents SHALL drop as items, and its fish SHALL drop as raw fish items, one per fish.

#### Scenario: Breaking a Floor block
- **WHEN** a player breaks one Floor block of an aquarium holding 12 cod and 20 seeds
- **THEN** the aquarium unforms and 12 raw cod and 20 seeds drop

#### Scenario: Tank grows
- **WHEN** a player adds a layer of tank blocks on top of a formed aquarium's tank, under the lid
- **THEN** the aquarium unforms because its lid no longer sits on the tank

### Requirement: Whole lid layer
Using a stack of Aquarium Lid items on the top face of a Fluid Tank's top layer, while not sneaking, SHALL place a Lid slab over every column of that tank when every spot is free and the stack holds one item per column (always in creative). Otherwise one slab SHALL be placed as usual.

#### Scenario: Lid over a 3 by 2 tank
- **WHEN** a player holding 16 Lid items uses one on the top of a 3 by 2 tank
- **THEN** six Lid slabs are placed and the player holds 10

#### Scenario: Too few items
- **WHEN** the player holds 4 Lid items over a 3 by 2 tank
- **THEN** one Lid slab is placed on the clicked block

### Requirement: One species per aquarium
An aquarium SHALL keep one species, cod or salmon, set by the first fish poured into an empty aquarium. Pouring a fish of the other species SHALL be refused while the aquarium holds any fish. An aquarium with no fish SHALL accept either species.

#### Scenario: Other species refused
- **WHEN** an aquarium holds cod and a player uses a salmon bucket on a part
- **THEN** nothing changes, the player keeps the salmon bucket and is told the aquarium keeps cod

#### Scenario: Empty aquarium
- **WHEN** the last cod was caught and a player uses a salmon bucket on a part
- **THEN** the aquarium now keeps salmon and holds one fish

### Requirement: Fish buckets on the parts
Using a cod or salmon bucket on a part of a formed aquarium SHALL add one fish and give back an empty bucket. Using a water bucket on a part SHALL remove one fish and give back the matching fish bucket. A full aquarium SHALL refuse fish, an empty one SHALL refuse the water bucket. A cod or salmon bucket used on the glass of the aquarium's tank SHALL pour the fish in as on a part. Other buckets used on the tank glass SHALL fill and drain the tank as before.

#### Scenario: Pouring a cod
- **WHEN** a player uses a cod bucket on a Lid slab of an aquarium with room
- **THEN** the aquarium holds one more cod and the player holds an empty bucket

#### Scenario: Catching a cod
- **WHEN** a player uses a water bucket on a Floor block of an aquarium holding 5 cod
- **THEN** it holds 4 cod and the player holds a cod bucket

#### Scenario: Fish bucket on the glass
- **WHEN** a player uses a cod bucket on the glass of an aquarium's tank with room
- **THEN** the aquarium holds one more cod, the tank's water is unchanged and the player holds an empty bucket

#### Scenario: Water bucket on the glass
- **WHEN** a player uses a water bucket on the glass of an aquarium's tank that has room
- **THEN** the tank takes the water and no fish is caught

### Requirement: Capacity follows the water
An aquarium's capacity SHALL be the water in its tank divided by the water per fish from data (example 4,000 mB). When the stock is larger than the capacity at the end of a cycle, the fish above the capacity SHALL die. An aquarium whose tank holds no water SHALL lose all its fish at the end of a cycle.

#### Scenario: Capacity of a full tank
- **WHEN** a 4 by 2 by 2 tank holds 256,000 mB of water and the water per fish is 4,000 mB
- **THEN** the aquarium's capacity is 64 fish

#### Scenario: Draining kills the surplus
- **WHEN** an aquarium holding 60 fish is drained to 120,000 mB
- **THEN** after the next cycle it holds 30 fish

### Requirement: Breeding cycle
Once per cycle (example 1,200 ticks) a powered, fed aquarium with at least two fish SHALL grow its stock by rate times quality times stock times one minus stock over capacity, rate from data (example 0.12). Fractions SHALL carry over to later cycles. One fish alone SHALL not breed.

#### Scenario: Growth in clean water
- **WHEN** a powered, fed aquarium with capacity 100 holds 20 fish at 100% quality and the rate is 0.12
- **THEN** after one cycle it holds about 21.9 fish, shown as 21

#### Scenario: Single fish
- **WHEN** a powered, fed aquarium holds one fish
- **THEN** after one cycle it still holds one fish

### Requirement: Feeding
Each cycle every fish SHALL eat feed from data (example 0.1 items per fish), taken from the feed slot, which accepts only items in the tag `c:fish_feed`. Delicacies SHALL put `c:seeds` into that tag. An aquarium that cannot feed every fish SHALL not breed that cycle, and after several unfed cycles in a row (example 3) SHALL lose a share of its fish each cycle (example 5%).

#### Scenario: Seeds as feed
- **WHEN** a player puts wheat seeds into the feed slot
- **THEN** the slot accepts them, while it refuses bread

#### Scenario: Starving
- **WHEN** an aquarium with 40 fish has had no feed for 4 cycles
- **THEN** it loses 2 fish that cycle

### Requirement: Water quality
Each cycle the fish SHALL add dirt (example 0.5 per fish) and eaten feed SHALL add dirt (example 1 per item). Quality SHALL be one minus the dirt over the water in buckets times the dirt a bucket holds at zero quality (example 10). Quality SHALL slow breeding in proportion, and below a threshold (example 20%) the aquarium SHALL lose a share of its fish each cycle (example 10%).

#### Scenario: Crowded tank gets dirty
- **WHEN** a 128 bucket aquarium with 32 fish has no filter and no water exchange
- **THEN** its quality drops by about 1.5% per cycle

#### Scenario: Dirty water kills
- **WHEN** an aquarium with 50 fish has 15% quality at the end of a cycle
- **THEN** it holds 45 fish afterwards

### Requirement: Carbon Filter
The filter slot SHALL accept only items the Foundation filter API can clean with. Each powered cycle the filter SHALL remove dirt up to its rate from data (example 40) and wear by the dirt it removed. A worn out filter SHALL be replaced by its spent item, which moves to the catch slots.

#### Scenario: Filter keeps a light stock clean
- **WHEN** an aquarium makes 14 dirt per cycle and holds a fresh Carbon Filter
- **THEN** its quality stays at 100% and the filter loses 14 durability per cycle

#### Scenario: Filter wears out
- **WHEN** the filter's last durability is used
- **THEN** the filter slot is empty and a spent filter lies in a catch slot

### Requirement: Murky Water
Delicacies SHALL add the fluid Murky Water with a bucket. Draining an aquarium through a part SHALL take water from its tank and hand it out as Murky Water, removing dirt in the same share as the water drained. Draining the tank through its glass SHALL remove water but no dirt. Water filled in through a part or the glass SHALL dilute the dirt.

#### Scenario: Exchange through a part
- **WHEN** a pipe drains 10 buckets through a Floor block of a 100 bucket aquarium with 200 dirt
- **THEN** the pipe receives 10,000 mB of Murky Water and the aquarium keeps 180 dirt

#### Scenario: Draining the glass does not clean
- **WHEN** 10 buckets are drained through the tank glass of the same aquarium
- **THEN** it keeps 200 dirt in 90 buckets and its quality drops

#### Scenario: Parts refuse other fluids
- **WHEN** a pipe offers lava to a part
- **THEN** the part takes none

### Requirement: Catch
An aquarium SHALL have three catch slots that only hand items out, an auto catch switch (default on) and a keep level (example default 8). Each cycle with auto catch on, every whole fish above the keep level SHALL leave the stock as one raw fish of its species into the catch slots, as far as they have room.

#### Scenario: Catch above the keep level
- **WHEN** an aquarium with auto catch on and keep level 8 holds 20 cod at the end of a cycle
- **THEN** it holds 8 cod and the catch slots hold 12 raw cod

#### Scenario: Auto catch off
- **WHEN** auto catch is off
- **THEN** no fish leave the stock and the stock grows up to its capacity

#### Scenario: Catch slots full
- **WHEN** the catch slots are full
- **THEN** the fish stay in the aquarium

### Requirement: Power
An aquarium SHALL take FE through every Floor block into one buffer and spend a cost per tick from data: a share per tank block and a share per lid slab (example 2 and 1 FE/t). A tick with less FE than the cost SHALL spend nothing and count as unpowered. Breeding and cleaning of a cycle SHALL scale with the share of powered ticks.

#### Scenario: Cost of a 4 by 2 by 2 aquarium
- **WHEN** the aquarium has 16 tank blocks and 8 lid slabs and the costs are 2 and 1 FE/t
- **THEN** it spends 40 FE every powered tick

#### Scenario: Power cut
- **WHEN** an aquarium gets no FE for a whole cycle
- **THEN** it does not breed and its filter cleans nothing, but its fish eat and dirt grows

#### Scenario: Lid takes no power
- **WHEN** a cable offers FE to a Lid slab
- **THEN** the slab takes none

### Requirement: Light
While an aquarium is formed and powered, its lid panels SHALL glow into the water and the Lid slabs SHALL give off light. Without power or unformed they SHALL be dark. The light SHALL change nothing else.

#### Scenario: Power returns
- **WHEN** power comes back to an aquarium
- **THEN** its lid panels glow again

### Requirement: Aquarium GUI
Using any part of a formed aquarium with an empty hand or any item other than a bucket SHALL open its GUI. The GUI SHALL show the species, fullness in percent, stock and capacity, water quality, whether it is powered, the feed, filter and catch slots, the auto catch switch and the keep level.

#### Scenario: Opening from the lid
- **WHEN** a player right clicks a Lid slab of a formed aquarium with an empty hand
- **THEN** the aquarium GUI opens and shows, for example, "Cod", 30% full, 24 of 80, water 92%, powered

#### Scenario: Keep level in the GUI
- **WHEN** the player raises the keep level from 8 to 18
- **THEN** the next cycle leaves 18 fish in the stock

#### Scenario: Unformed part
- **WHEN** a player right clicks a Floor block that belongs to no aquarium
- **THEN** no GUI opens and the player is told to form it with the Configuration Carrot

### Requirement: Automation through capabilities
Every part of a formed aquarium SHALL offer through the standard item capability the feed slot (insert), the filter slot (insert) and the catch slots (extract); through the fluid capability water in and Murky Water out; and Floor blocks the energy capability. An unformed part SHALL offer nothing.

#### Scenario: Pipe brings seeds
- **WHEN** an item pipe pushes wheat seeds into a Floor block of a formed aquarium
- **THEN** the seeds land in the feed slot

#### Scenario: Pipe takes the catch
- **WHEN** an item pipe pulls from a Lid slab of an aquarium with 12 raw cod in its catch slots
- **THEN** the pipe receives the raw cod and never the feed or filter

### Requirement: Swimming fish
A formed aquarium SHALL draw fish of its species with the vanilla fish models, swimming on smooth paths inside the water and turning in their direction. The number drawn SHALL be the cap from data (example 16) times the fullness, at least one per fish up to two. Every player SHALL see the same paths.

#### Scenario: Half full
- **WHEN** an aquarium is 50% full and the cap is 16
- **THEN** 8 fish swim in its tank

#### Scenario: Fish stay in the water
- **WHEN** the tank is filled to one third
- **THEN** every drawn fish swims below the water surface

### Requirement: Fish show the aquarium's state
Drawn fish SHALL swim slower and move their tails less the dirtier the water is. When fish die, a few SHALL float belly up at the surface for some seconds. When the tank holds too little water to swim in, the fish SHALL flop on the tank floor.

#### Scenario: Dirty water
- **WHEN** water quality drops from 100% to 30%
- **THEN** the drawn fish swim visibly slower

#### Scenario: Deaths
- **WHEN** an aquarium loses fish at the end of a cycle
- **THEN** up to three fish float belly up at the surface and then disappear

### Requirement: Aquarium floor bed
A formed aquarium SHALL show a gravel bed with swaying seagrass on its tank's floor, above the tank's floor plate.

#### Scenario: Looking into the tank
- **WHEN** a player looks down into a formed aquarium
- **THEN** they see gravel and seagrass instead of the tank's floor plate

### Requirement: Aquarium logic is pure
The cycle (breeding, feeding, starving, dirt, filter, exchange share, catch, deaths), the power cost and the number of drawn fish SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover growth toward capacity, the single fish, starving, dirt and quality, the filter and its wear, the exchange share, the catch above the keep level, power share and the drawn count
