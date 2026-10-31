# shuttles/rail-corrosion Specification

## Purpose
The copper heads of Farm Rails corrode rail by rail as the Field Head waters them. Each rail keeps its stage, also on its item; anti-corrosion items in the head hold the aging, a corroded rail stops the water to the rails behind it, and an axe scrapes the field back one stage per rail for some durability per rail, keeping a worn axe's progress on the next rail.

## Requirements

### Requirement: Aging by water used
A Field Head serving a field SHALL count the water it uses to hydrate the field. Every `corrosion_water` mB of that water (from `data/<ns>/shuttles/field.json`, example 9000) one aging step SHALL be due. When an aging step is due and the head holds an anti-corrosion item, the head SHALL use one of them and every rail SHALL keep its stage; otherwise every wet rail SHALL go one stage further. Dry rails SHALL NOT age. The counted water SHALL be kept when the world is saved. Aging SHALL happen only when water is used, never by a timer or a scan.

#### Scenario: Step without upkeep
- **WHEN** a field of copper rails has used `corrosion_water` mB and its head holds no anti-corrosion item
- **THEN** every rail becomes exposed

#### Scenario: Uneven field ages evenly
- **WHEN** a field of two weathered rails and one fresh rail, all wet, reaches an aging step without upkeep
- **THEN** the two rails become oxidized and the fresh rail becomes exposed

#### Scenario: Dry rails stay
- **WHEN** a field whose second rail is corroded reaches an aging step without upkeep
- **THEN** the first rail ages one stage and the rails behind the corroded one keep their stage

#### Scenario: Honeycomb holds the stage
- **WHEN** a field's head holding three honeycomb reaches an aging step
- **THEN** every rail keeps its stage and the head holds two honeycomb

#### Scenario: No water, no aging
- **WHEN** a head has no water and its field is not hydrated
- **THEN** none of its rails age

### Requirement: Scraping with an axe
Using an axe (any item that can scrape, like vanilla axes on copper) on a farm rail SHALL scrape the rails of its field, starting at the clicked rail and going outward through the field. Each rail past fresh copper SHALL go back one stage for `axe_damage_per_rail` durability (in `field.json`, example 4), less any scrape progress it holds. When the axe has less durability left than the whole field costs, it SHALL spend all it has left: the rails it fully pays for go back one stage and the remaining points SHALL stay on the next rail as scrape progress. Fresh copper rails SHALL cost nothing. Unbreaking SHALL apply as usual to the durability the axe loses; the progress SHALL count the durability the scrape asked for. A rail SHALL go back at most one stage per use. The player SHALL be told how many rails went back a stage and, when not every rail past fresh copper did, how many are left. A scrape SHALL play the vanilla scrape sound and particles on the scraped rails. A scraped rail that is no longer corroded SHALL let water through again.

#### Scenario: Scrape a field
- **WHEN** a player uses an iron axe on a rail of a field of five oxidized rails with `axe_damage_per_rail` 4
- **THEN** all five rails show weathered heads and the axe lost twenty durability

#### Scenario: Stone axe rule of thumb
- **WHEN** a new stone axe (131 durability) is used on a field of 32 exposed rails with `axe_damage_per_rail` 4
- **THEN** all 32 rails become fresh copper and the axe has three durability left

#### Scenario: Axe too worn
- **WHEN** an axe with ten durability left is used on the first rail of a straight field of five weathered rails with `axe_damage_per_rail` 4
- **THEN** the first two rails become exposed, the third rail stays weathered holding two points of scrape progress, the axe breaks and the player is told two rails were scraped and three are left

#### Scenario: Progress finishes a rail
- **WHEN** a rail holding two points of scrape progress is the only weathered rail of its field and an axe is used on it with `axe_damage_per_rail` 4
- **THEN** the rail becomes exposed and the axe loses two durability

#### Scenario: Fresh copper
- **WHEN** an axe is used on a field of fresh copper rails
- **THEN** nothing changes and the axe keeps its durability

#### Scenario: Scraping opens the water
- **WHEN** the corroded first rail of a field is scraped to oxidized while the head holds water
- **THEN** the field is wet again

### Requirement: Stage looks
Each stage SHALL have its own rail head texture: fresh copper with a touch of verdigris, exposed copper with patches of verdigris, weathered green copper, oxidized teal, and corroded dark green black with rust flecks. The sleepers SHALL keep the wood of the rail at every stage. The farm rail item SHALL show the head texture of its stage.

#### Scenario: Corroded rail
- **WHEN** a rail becomes corroded
- **THEN** it shows dark green black heads with rust flecks on its wooden sleepers

#### Scenario: Oxidized item
- **WHEN** a player holds a farm rail item broken from an oxidized rail
- **THEN** the item shows teal heads

### Requirement: Anti-corrosion slot
Anti-corrosion items SHALL be the item tag `servus_shuttles:anti_corrosion`, holding honeycomb by default, so packs can add their own. The anti-corrosion part of the Field Head's supply buffer SHALL take only those items. Supply runs SHALL keep it at `anti_corrosion_level` (from `field.json`, example 8) of the item it holds, or of the first item of the tag while it is empty. Players, hoppers, item pipes and the data line SHALL neither put items in nor take them out. Its items SHALL drop when the head is broken.

#### Scenario: Filled by hand
- **WHEN** a player tries to put honeycomb into the head's GUI
- **THEN** the honeycomb stays with the player

#### Scenario: Hopper cannot fill it
- **WHEN** a hopper holding honeycomb points into a Field Head
- **THEN** the honeycomb stays in the hopper

#### Scenario: Other items refused
- **WHEN** a supply run brings honeycomb and the head's anti-corrosion part already holds a different anti-corrosion item
- **THEN** the head takes only the item it holds and the rest goes back to storage

#### Scenario: Network keeps it stocked
- **WHEN** a head holds two honeycomb, `anti_corrosion_level` is 8 and a rack holds honeycomb
- **THEN** a supply run brings six honeycomb and the head ends up with eight

### Requirement: Corrosion stages per rail
The copper heads of farm rails SHALL have five corrosion stages, in order: copper, exposed, weathered, oxidized and corroded. Each farm rail SHALL have its own stage, and rails of one field MAY have different stages. Each farm rail SHALL keep its stage when the world is saved. The Warehouse Rail SHALL never corrode.

#### Scenario: Mixed field
- **WHEN** a fresh farm rail is placed at the end of a weathered field with a head
- **THEN** the new rail shows fresh copper heads and the other rails stay weathered

#### Scenario: Steel stays steel
- **WHEN** a run mixes Warehouse Rails and oxidized farm rails
- **THEN** the Warehouse Rails keep their steel heads

### Requirement: Stage kept on the rail item
A farm rail that is broken SHALL drop an item that keeps its stage and its scrape progress, and placing that item SHALL give the placed rail the same stage and progress. A crafted farm rail SHALL be fresh copper. Farm rail items of different stages or progress SHALL NOT stack together. The tooltip of a farm rail item past fresh copper SHALL name its stage.

#### Scenario: Breaking does not clean
- **WHEN** an oxidized farm rail is broken and its drop is placed on its own
- **THEN** the placed rail shows oxidized heads

#### Scenario: Crafted is fresh
- **WHEN** a player crafts farm rails
- **THEN** they are fresh copper and stack with other fresh farm rails

#### Scenario: Stages do not stack
- **WHEN** a player picks up an oxidized and a weathered farm rail
- **THEN** they sit in two separate stacks

### Requirement: Water flows along the rails
A Field Head SHALL water its field from the farm rail beside it along the rails of the field. A rail SHALL be wet while the head waters the field and the rail can be reached from that first rail over rails that are not corroded. A corroded rail SHALL be dry and SHALL block the water for every rail that can only be reached through it. Only wet rails SHALL use water and only plots of wet rails SHALL be hydrated.

#### Scenario: Corroded rail in the middle
- **WHEN** the third rail of a straight field of six rails, counted from the head, is corroded
- **THEN** the first two rails are wet and the third to sixth rails are dry

#### Scenario: Corroded first rail
- **WHEN** the farm rail beside the head is corroded
- **THEN** every rail of the field is dry and the head uses no water

#### Scenario: Water goes around
- **WHEN** a field forms a loop and one rail of the loop is corroded
- **THEN** every other rail is still wet, reached from the other side of the loop

#### Scenario: Corroded rail at the far end
- **WHEN** only the last rail of a straight field is corroded
- **THEN** every other rail is wet and only the plots of the last rail dry out, unless a wet rail reaches them

### Requirement: Corroded rails
A field with a corroded rail SHALL keep working apart from the water: harvest runs SHALL continue, shuttles SHALL keep driving over every rail, and no block SHALL break. While any rail of the served field is corroded, the head's top plate SHALL show a rust red rim.

#### Scenario: Harvest goes on
- **WHEN** a field whose first rail is corroded has ripe crops past its threshold
- **THEN** a harvester still comes and harvests

#### Scenario: Rust rim
- **WHEN** one rail of a served field becomes corroded
- **THEN** the head's top plate shows a rust red rim

### Requirement: Corrosion rules per rail are pure logic
The next and previous stage, the counting of water into aging steps, which rails of a field the water reaches, and how a scrape with a given amount of durability spreads over the rails in order SHALL be pure Java without Minecraft imports and covered by JUnit tests.

#### Scenario: Water stops at a corroded rail
- **WHEN** the water reach is computed for a straight field of four rails whose second rail is corroded
- **THEN** only the first rail is wet

#### Scenario: Scrape plan with progress
- **WHEN** a scrape of ten points with 4 per rail is planned over three weathered rails in order
- **THEN** the first two rails go back a stage, the third holds two points of progress and all ten points are spent
