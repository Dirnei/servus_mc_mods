# Spec Delta

## ADDED Requirements

### Requirement: Tag search in the item picker
A screen MAY turn on tag search for Foundation's item picker. With it on, a search starting with `#` SHALL list item tags instead of items: every item tag the client knows, matched anywhere in its id, each with the first item of the tag as icon and the number of items it holds. Pressing Tab SHALL complete the search to the top hit. Hovering a tag SHALL show some of its items. Picking a tag SHALL hand the screen the tag id with its `#`. Without tag search, `#` SHALL search like any other text.

#### Scenario: Complete a tag
- **WHEN** the player types `#ingot` in a picker with tag search and presses Tab
- **THEN** the search reads `#c:ingots` and `#c:ingots` is the top entry with its item count

#### Scenario: Pack tags
- **WHEN** a datapack adds the item tag `mypack:fuel`
- **THEN** typing `#fuel` lists `#mypack:fuel`

#### Scenario: Hover a tag
- **WHEN** the player hovers `#c:ingots`
- **THEN** a tooltip lists some items of the tag, for example Iron Ingot, Copper Ingot and Gold Ingot

### Requirement: Tag search ranking is pure
Matching and ordering tags for a search SHALL be pure Java covered by JUnit: tags whose path starts with the search come first, then tags whose id contains it, each group in id order, and the top hit is what Tab completes to.

#### Scenario: Prefix first
- **WHEN** the tags are `c:ingots`, `c:ingots/iron` and `minecraft:beacon_payment_items` and the search is `#ing`
- **THEN** the order is `c:ingots`, `c:ingots/iron` and the beacon tag is not listed
