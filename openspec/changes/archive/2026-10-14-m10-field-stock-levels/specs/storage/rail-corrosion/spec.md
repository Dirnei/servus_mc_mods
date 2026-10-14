# Spec Delta

## MODIFIED Requirements

### Requirement: Scraping with an axe
Using an axe (any item that can scrape, like vanilla axes on copper) on a farm rail of a field SHALL scrape the whole field back one stage and SHALL cost axe durability per rail of the field (`axe_damage_per_rail` in `field.json`, example 2; Unbreaking applies as usual). A field at fresh copper SHALL NOT be scraped and costs nothing. When the axe has less durability left than the scrape costs, nothing SHALL happen and the player SHALL be told how many rails the field has and how much durability the axe needs. A successful scrape SHALL play the vanilla scrape sound and particles. A scraped corroded field SHALL be watered again.

#### Scenario: Scrape a field
- **WHEN** a player uses an iron axe on a rail of an oxidized field of five rails
- **THEN** all five rails show weathered heads and the axe lost ten durability

#### Scenario: Axe too worn
- **WHEN** an axe with nine durability left is used on a rail of a weathered field of five rails
- **THEN** nothing changes and the player is told the field has five rails and the axe needs ten durability

#### Scenario: Fresh copper
- **WHEN** an axe is used on a field of fresh copper
- **THEN** nothing changes and the axe keeps its durability
