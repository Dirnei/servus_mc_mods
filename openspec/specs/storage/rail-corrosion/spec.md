# storage/rail-corrosion Specification

## Purpose
The copper heads of Farm Rails corrode per field as the Field Head waters it. Anti-corrosion items in the head hold the stage, a corroded field is no longer watered, and an axe scrapes the whole field back one stage at one durability per rail.

## Requirements

### Requirement: Corrosion stages per field
The copper heads of farm rails SHALL have five corrosion stages, in order: copper, exposed, weathered, oxidized and corroded. A field SHALL have one stage for all its rails; when the stage of a served field changes, every rail of the field SHALL show the new stage at once. Each farm rail SHALL keep its stage when the world is saved. The Warehouse Rail SHALL never corrode.

#### Scenario: Whole field changes
- **WHEN** a served field of six farm rails goes from copper to exposed
- **THEN** all six rails show exposed copper heads

#### Scenario: Steel stays steel
- **WHEN** a run mixes Warehouse Rails and farm rails and the field is oxidized
- **THEN** the Warehouse Rails keep their steel heads

### Requirement: Stage of placed and joined rails
A crafted farm rail SHALL be fresh copper and the stage SHALL NOT be stored on the item: a farm rail that is broken and placed again SHALL be fresh until it joins a field. A placed rail that joins a field SHALL take the stage of that field. When a placed rail joins two or more fields, the joined field SHALL take the stage of the field whose Field Head serves it; when no head serves it, the younger (less corroded) stage of the joined fields. When a removed rail splits a field, each part SHALL keep the stage it had; a part without a serving head SHALL stop aging.

#### Scenario: Placed rail takes the stage
- **WHEN** a fresh farm rail is placed at the end of a weathered field with a head
- **THEN** the new rail shows weathered heads

#### Scenario: Dropped rail is fresh
- **WHEN** a rail of an oxidized field is broken and the drop is placed on its own
- **THEN** the placed rail shows fresh copper

#### Scenario: Head decides a join
- **WHEN** a rail joins an exposed field served by a head and an oxidized field without a head
- **THEN** all rails of the joined field show exposed

#### Scenario: Younger stage without a head
- **WHEN** a rail joins a weathered field and an exposed field and no head serves either
- **THEN** all rails of the joined field show exposed

### Requirement: Aging by water used
A Field Head serving a field SHALL count the water it uses to hydrate the field. Every `corrosion_water` mB of that water (from `data/<ns>/storage/field.json`, example 64000) one aging step SHALL be due. When an aging step is due and the head holds an anti-corrosion item, the head SHALL use one of them and the stage SHALL stay; otherwise the field SHALL go one stage further. The counted water SHALL be kept when the world is saved. Aging SHALL happen only when water is used, never by a timer or a scan.

#### Scenario: Step without upkeep
- **WHEN** a copper field's head has used `corrosion_water` mB and holds no anti-corrosion item
- **THEN** the field becomes exposed

#### Scenario: Honeycomb holds the stage
- **WHEN** a weathered field's head holding three honeycomb reaches an aging step
- **THEN** the field stays weathered and the head holds two honeycomb

#### Scenario: No water, no aging
- **WHEN** a head has no water and its field is not hydrated
- **THEN** its field does not age

### Requirement: Corroded field
A corroded field SHALL no longer be watered: its head SHALL stop hydrating it, so its farmland may dry, and SHALL use no water and therefore no longer age. The head SHALL show that it is broken: its GUI SHALL say that the rails are corroded and the field is not watered, and its top plate SHALL show a rust red rim. Harvest runs SHALL continue, shuttles SHALL keep driving over the rails, and no block SHALL break.

#### Scenario: Corroded stops hydration
- **WHEN** a field with water in its head becomes corroded
- **THEN** its farmland no longer counts as near water and the head's water stays where it is

#### Scenario: Harvest goes on
- **WHEN** a corroded field has ripe crops past its threshold
- **THEN** a harvester still comes and harvests

### Requirement: Scraping with an axe
Using an axe (any item that can scrape, like vanilla axes on copper) on a farm rail of a field SHALL scrape the whole field back one stage and SHALL cost one axe durability per rail of the field (Unbreaking applies as usual). A field at fresh copper SHALL NOT be scraped and costs nothing. When the axe has less durability left than the field has rails, nothing SHALL happen and the player SHALL be told how many rails the field has and how much durability the axe needs. A successful scrape SHALL play the vanilla scrape sound and particles. A scraped corroded field SHALL be watered again.

#### Scenario: Scrape a field
- **WHEN** a player uses an iron axe on a rail of an oxidized field of five rails
- **THEN** all five rails show weathered heads and the axe lost five durability

#### Scenario: Axe too worn
- **WHEN** an axe with three durability left is used on a rail of a weathered field of five rails
- **THEN** nothing changes and the player is told the field has five rails and the axe needs five durability

#### Scenario: Fresh copper
- **WHEN** an axe is used on a field of fresh copper
- **THEN** nothing changes and the axe keeps its durability

### Requirement: Stage looks
Each stage SHALL have its own rail head texture: fresh copper with a touch of verdigris, exposed copper with patches of verdigris, weathered green copper, oxidized teal, and corroded dark green black with rust flecks. The sleepers SHALL keep the wood of the rail at every stage. The farm rail item SHALL always show fresh copper.

#### Scenario: Corroded rail
- **WHEN** a field becomes corroded
- **THEN** its rails show dark green black heads with rust flecks on their wooden sleepers

### Requirement: Corrosion rules are pure logic
The next and previous stage, the stage of a join, the counting of water into aging steps and the check whether an axe has durability enough for a field SHALL be pure Java without Minecraft imports and covered by JUnit tests.

#### Scenario: Join without a head
- **WHEN** the joined stages are weathered and exposed and no head stage is known
- **THEN** the join gives exposed

### Requirement: Anti-corrosion slot
Anti-corrosion items SHALL be the item tag `servus_storage:anti_corrosion`, holding honeycomb by default, so packs can add their own. The anti-corrosion part of the Field Head's supply buffer SHALL take only those items. Supply runs SHALL keep it at `anti_corrosion_level` (from `field.json`, example 8) of the item it holds, or of the first item of the tag while it is empty. Players, hoppers, item pipes and the data line SHALL neither put items in nor take them out. Its items SHALL drop when the head is broken.

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
