# Spec Delta

## MODIFIED Requirements

### Requirement: Stock view with search
The Storage Terminal GUI SHALL list every indexed item of the warehouses it reaches over its data line with its requestable count in a scrollable grid, sorted by the player's sort setting (name, amount or mod, ascending or descending; amount descending by default). The requestable count SHALL be the stock in the racks less the items shuttle picks have locked; items on their way into storage SHALL count once they are in a rack. An item with nothing left to request SHALL NOT be listed as stock. A search box SHALL filter the list with Foundation's search syntax as the player types: a plain word by item name, case insensitive, and the prefixes by mod, tag, tooltip, creative tab and id. On the Rules tab the same search SHALL filter the rule rows by their item.

#### Scenario: Search
- **WHEN** the warehouse holds cobblestone, iron ingots and bread and the player types "iron"
- **THEN** only iron ingots are shown

#### Scenario: Search by mod
- **WHEN** the warehouse holds cobblestone, a Fluid Tank and Rails and the player types `@servus`
- **THEN** only the Fluid Tank and the Rails are shown

#### Scenario: Search by tag
- **WHEN** the warehouse holds iron ingots, copper ingots and iron ore and the player types `$ingots`
- **THEN** only the iron and copper ingots are shown

#### Scenario: Search with components
- **WHEN** the warehouse holds a Fluid Tank with a stone brick floor and one with a cobblestone floor and the player types `#cobblestone`
- **THEN** only the tank with the cobblestone floor is shown

#### Scenario: Craftable entries searched too
- **WHEN** a sticks pattern is on the line, no sticks are in stock and the player types `$c:rods`
- **THEN** sticks show as a craftable entry

#### Scenario: Rules by tag
- **WHEN** the Rules tab shows rules for torches, sticks and iron ingots and the player types `$ingots`
- **THEN** only the iron ingots row shows

#### Scenario: Sort by name
- **WHEN** the warehouse holds 50 cobblestone, 10 apples and 20 bread and the sort is name ascending
- **THEN** the grid shows apple, bread, cobblestone in that order

#### Scenario: Sort by amount
- **WHEN** the same stock is sorted by amount descending
- **THEN** the grid shows cobblestone, bread, apple

#### Scenario: Request lowers the count
- **WHEN** the warehouse holds 50 cobblestone and the player requests 20
- **THEN** the grid shows 30 cobblestone within half a second, before the shuttle has picked them

#### Scenario: Last item requested
- **WHEN** the warehouse holds one configuration item and the player requests it
- **THEN** the configuration item leaves the stock entries while the shuttle fetches it

#### Scenario: Fully reserved but craftable
- **WHEN** every stick in stock is locked by picks and a sticks pattern, a Crafting Station and a CPU are on the line
- **THEN** sticks show as a craftable entry with "Craft" instead of a count

### Requirement: JEI search sync
With JEI installed, the search mode JEI synced SHALL copy the terminal's search text into JEI's search as the player types, and JEI two way SHALL also copy JEI's search text into the terminal's search box when it changes there. Because the terminal reads JEI's syntax, a copied text SHALL filter both lists the same way. Without JEI the search mode button SHALL only offer normal.

#### Scenario: Synced
- **WHEN** the search mode is JEI synced and the player types "iron" in the terminal
- **THEN** JEI's search shows "iron"

#### Scenario: Same meaning on both sides
- **WHEN** the search mode is JEI two way and the player types `@minecraft $ingots` in JEI's search
- **THEN** the terminal's search shows the same text and its grid shows only vanilla ingots, as JEI's list does

#### Scenario: No JEI
- **WHEN** JEI is not installed
- **THEN** the search mode stays normal and the terminal works unchanged
