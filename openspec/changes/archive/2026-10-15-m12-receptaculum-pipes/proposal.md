# Proposal

## Why

The play check of the Receptaculum found two pipe problems. An Item Pipe draws its connection to the Receptaculum's front, which is fixed to Off, so the pipe looks connected but never moves anything. And a pipe that delivers to the Receptaculum fills its three gift slots with anything that is not the Elixir, so junk blocks the real gifts. Both are play check fixes in M12 (Alchemy, Citrinitas and Rubedo); the first one is a Foundation rule for every machine with a fixed side.

## What Changes

- A machine side that its block fixes to Off offers no item capability. Pipes, hoppers and every other mod see nothing there, so an Item Pipe draws no arm to a machine's front. This holds for every Foundation machine with a fixed front: the Receptaculum, the Salamander Forge, the Prelum, the Coal Generator, the pumps and the other Alchemy machines that share the side handler.
- Sides a player sets to Off with the carrot or in the side panel keep their capability, so a pipe stays connected and works again when the side is switched back.
- Automation (pipes, hoppers, data lines) inserts into the Receptaculum's gift slots only what gnomes like, by the gnome gift data. Everything else stays in the pipe network for other targets. The Elixir slot keeps taking only the Elixir of Sight.
- By hand, in the screen, the gift slots still take any item but the Elixir, as now.

## Non-goals

- Hiding capabilities on sides the player sets to Off. That would need capability invalidation on every side change.
- Filtering gifts in the screen. The gift data is loaded on the server only, so a client of a dedicated server could not check it.
- Other machines' input filters.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/side-config`: "Fixed sides" adds that a side fixed to Off offers no item capability.
- `alchemy/receptaculum`: "Yield, gift and Elixir slots" and "Pipes reach the yield": automation inserts only liked gifts into the gift slots; a pipe at the front does not connect.

## Impact

- Foundation: `machine/MachineBlockEntity.java` gets `itemCapability(Direction)`; `machine/MachineEvents.java` uses it for the Coal Generator and the Burner Pump.
- Alchemy: `ServusAlchemyEvents.java` uses it for the Salamander Forge, the Receptaculum, the Prelum and the shared machine registration; `homunculus/ReceptaculumBlockEntity.java` filters automation inserts into the gift slots by `GnomeGiftData`.
- Tests: GameTests in `HomunculusGameTests` for both fixes, and a Foundation GameTest for a fixed front.
