# Spec Delta

## ADDED Requirements

### Requirement: Search prefixes
A search box that lists items SHALL read its text as terms with JEI's prefixes. A term with no prefix SHALL match the name. `@` SHALL match the mod id or mod name. `$` SHALL match an item tag id. `#` SHALL match the tooltip lines below the name. `%` SHALL match the name of a creative tab that holds the item. `&` SHALL match the item id. Each term SHALL match anywhere in its target, ignoring case.

#### Scenario: By mod
- **WHEN** the list holds Iron Ingot, Stone and a Fluid Tank and the player types `@servus`
- **THEN** only the Fluid Tank is shown

#### Scenario: By mod name
- **WHEN** the player types `@foundation` and the mod Servus: Foundation has the id `servus_foundation`
- **THEN** the Fluid Tank is shown, matched by its mod name and its id alike

#### Scenario: By tag
- **WHEN** the list holds Iron Ingot, Iron Ore and Stone and the player types `$ingots`
- **THEN** only Iron Ingot is shown

#### Scenario: By tooltip
- **WHEN** the list holds a Fluid Tank whose tooltip names its floor "Stone Bricks" and the player types `#"stone bricks"`
- **THEN** that Fluid Tank is shown

#### Scenario: By creative tab
- **WHEN** the player types `%redstone`
- **THEN** the items of the Redstone Blocks tab are shown

#### Scenario: By id
- **WHEN** the player types `&minecraft:oak_`
- **THEN** items whose id starts with `minecraft:oak_` are shown, also those whose name does not say oak

#### Scenario: Plain word
- **WHEN** the player types `IRON`
- **THEN** every item whose name contains "iron" is shown, as before

### Requirement: Combining search terms
Terms separated by spaces SHALL all have to match. `|` SHALL split the text into alternatives, and an item SHALL be shown when any alternative matches. A term starting with `-` SHALL match only items it would not match without the `-`. Text in double quotes SHALL be one term, spaces included; an unclosed quote SHALL run to the end. A term that is only a prefix or a `-` SHALL be ignored, and an empty search SHALL show everything.

#### Scenario: All terms
- **WHEN** the player types `@minecraft $ingots`
- **THEN** only vanilla ingots are shown

#### Scenario: Either side
- **WHEN** the player types `$ingots | $nuggets`
- **THEN** every ingot and every nugget is shown

#### Scenario: Not
- **WHEN** the player types `$ingots -@minecraft`
- **THEN** only ingots from other mods are shown

#### Scenario: Prefix alone
- **WHEN** the player types `iron @`
- **THEN** the list is the same as for `iron`

### Requirement: Search syntax is pure
Reading a search text into terms and alternatives, and deciding whether an item's facts match it, SHALL be pure Java without Minecraft classes, covered by JUnit tests. The facts of an item SHALL be asked for only when a term needs them, so that a search without `#` never builds a tooltip.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover each prefix, spaces, `|`, `-`, quotes, an unclosed quote, a lone prefix, case and an empty search without starting Minecraft

#### Scenario: Tooltip only when asked
- **WHEN** the search is `@minecraft iron`
- **THEN** no item's tooltip is asked for

### Requirement: Search syntax hint
Hovering a search box that uses the search syntax SHALL show a tooltip that lists the prefixes, `-` and `|`, one short line each.

#### Scenario: Hover the search
- **WHEN** the player hovers the search box of the Storage Terminal or of an item picker
- **THEN** a tooltip lists `@` mod, `$` tag, `#` tooltip, `%` tab, `&` id, `-` not and `|` or

### Requirement: Search in the item picker
Foundation's item picker SHALL filter its entries with the search syntax. An item entry SHALL match by everything the syntax reads, and a plain word SHALL also match the entry id as before. A fluid entry SHALL match by name, by mod, by fluid tag and by id. A `#` or `%` term SHALL NOT match it. Entries that are neither SHALL match by name and id only. The none entry SHALL stay first whatever the search.

#### Scenario: Duct filter by mod
- **WHEN** the player opens the item filter popup of a duct and types `@servus_shuttles`
- **THEN** only Shuttles items are listed, after the none entry

#### Scenario: Fluid by mod
- **WHEN** the player opens the fluid filter popup and types `@minecraft`
- **THEN** water and lava are listed

## MODIFIED Requirements

### Requirement: Tag search in the item picker
A screen MAY turn on tag search for Foundation's item picker. With it on, a search starting with `$` SHALL list item tags instead of items: every item tag the client knows, matched anywhere in its id, each with the first item of the tag as icon and the number of items it holds. Pressing Tab SHALL complete the search to `$` and the top hit's id. Hovering a tag SHALL show some of its items. Picking a tag SHALL hand the screen the tag id with `#` in front, which is also how the entry is labelled. Without tag search, `$` SHALL filter items by tag as the search syntax says. In both cases `#` SHALL search tooltips.

#### Scenario: Complete a tag
- **WHEN** the player types `$ingot` in a picker with tag search and presses Tab
- **THEN** the search reads `$c:ingots` and `#c:ingots` is the top entry with its item count

#### Scenario: Pack tags
- **WHEN** a datapack adds the item tag `mypack:fuel`
- **THEN** typing `$fuel` lists `#mypack:fuel`

#### Scenario: Hover a tag
- **WHEN** the player hovers `#c:ingots`
- **THEN** a tooltip lists some items of the tag, for example Iron Ingot, Copper Ingot and Gold Ingot

#### Scenario: Hash searches tooltips
- **WHEN** the player types `#ingot` in a picker with tag search
- **THEN** items are listed whose tooltip contains "ingot", not tags

### Requirement: Tag search ranking is pure
Matching and ordering tags for a search SHALL be pure Java covered by JUnit: tags whose path starts with the search come first, then tags whose id contains it, each group in id order, and the top hit is what Tab completes to.

#### Scenario: Prefix first
- **WHEN** the tags are `c:ingots`, `c:ingots/iron` and `minecraft:beacon_payment_items` and the search is `$ing`
- **THEN** the order is `c:ingots`, `c:ingots/iron` and the beacon tag is not listed
