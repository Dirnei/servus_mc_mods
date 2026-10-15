# Proposal

## Why

`alchemy_plan.md` asks under "Open, Play" whether Ignis Perpetuus is also an endless fuel, for example in the Calciner, or only a building part. The answer is yes, in the Calciner: the eternal flame should spare the player the coal and reward the Citrinitas stage with faster calcining. This change implements that open point of M12 (Alchemy, Citrinitas and Rubedo).

## What Changes

- Ignis Perpetuus can be set into the Firebox of a formed Calciner by hand. It goes into its own hearth spot, not into the fuel slot, and is not used up.
- With the eternal flame set, the Calciner burns without fuel: it takes no fuel by hand or through the Firebox port, reports no upkeep, and any fuel still in the fuel slot goes back to the player who set the Ignis.
- With the eternal flame set, every calcination takes its recipe time divided by a speed factor from datapack JSON (2 in the shipped data, so twice as fast).
- The Ignis Perpetuus comes back to the player with a sneak and empty hand on the Firebox, and drops when the Firebox or the Calciner block is broken. It stays in the Calciner while the structure is unformed for another reason, and the Calciner does not work while unformed.
- The Ignis Perpetuus is drawn glowing in the Firebox grate of a formed Calciner, and Jade shows it in place of the fuel.
- The Journal's Ignis Perpetuus page and Calciner page say that it fires a Calciner forever and twice as fast.
- Ignis Perpetuus still does not burn in a furnace or any other fuel slot.
- `alchemy_plan.md`: the open question is answered and removed.

## Non-goals

- Eternal fire for other machines (Athanor, Coagulator, Purifier, Coal Generator, the burner pumps). Each can be its own change later if wanted.
- Making Ignis Perpetuus a furnace fuel. The Salamander Forge stays the infinite furnace.
- A new Firebox model. The Ignis is drawn as an item in the existing grate.

## Capabilities

### New Capabilities
- `alchemy/calciner-eternal-fire`: setting Ignis Perpetuus into the Calciner's Firebox, how it replaces fuel and upkeep, the speed factor, getting it back, and how it shows.

### Modified Capabilities
- `alchemy/calciner`: "Calciner burns fuel" gains the eternal flame as a second way to burn; the fuel slot and upkeep are idle while it is set.
- `alchemy/calciner-structure`: "Breaking unforms" drops the Ignis Perpetuus with the Firebox and the Calciner block; "Matter and fuel go in by hand" adds setting and taking the Ignis at the Firebox; "Item ports" refuses fuel at the Firebox while the Ignis is set; "Calciner in Jade" shows the Ignis.

## Impact

- Alchemy: `CalcinerBlockEntity`, `calciner/CalcinerPartBlock`, `calciner/CalcinerPorts`, `client/CalcinerRenderer`, the Jade plugin, a new settings loader and pure speed helper, datagen for lang and the Journal pages, a new datapack file `data/servus_alchemy/servus_alchemy/eternal_fire/calciner.json`, `CalcinerGameTests`.
- Foundation: `BurnerMachineBlockEntity` gets a hook for a fire that never goes out, off by default; no other machine changes behavior.
- The unarchived `m12-salamander` change says Ignis Perpetuus is "a building part only". Its scenario (no furnace burns it) still holds, but its wording should become "not a furnace fuel" before or when it archives.
- `alchemy_plan.md`: remove the answered open question.
