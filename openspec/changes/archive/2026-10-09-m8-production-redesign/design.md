# Design

## Context

`ProductionView.of` builds `ProductionPayload` with `CpuRow`s holding full `OrderRow`s and `StepRow`s, and `MachineRow`s holding up to 64 `RecipeRow`s each. `ProductionTerminalScreen` turns them into lines per tab (`Tab` list). `ProductionTerminalMenu` sends the payload on open (`sendAllDataToRemote`) and every 10 ticks when its signature changed; `ClientProduction` caches the last payload per terminal (m8-terminal-speed). `TerminalLayout.Production` holds the panel rectangles and `TerminalLayoutTest` checks them. See proposal.md for why.

## Goals / Non-Goals

**Goals:** a job monitor and a machine status list that a player understands at a glance; smaller payloads; keep instant content, the client cache and the checked layout.

**Non-Goals:** order planning, the Patterns tab, art.

## Decisions

### Rows on the wire
`ProductionRows` gets:
- `CpuRow(cpu, slots, inUse, waiting)`: summary only, for the header tooltip.
- `JobRow(cpu, id, item, amount, crafted, state, doneAt, status, args, requestId)`: one per order of every CPU. `status` is a lang key suffix and `args` its arguments as strings (item and machine names resolved on the server, like the old rows). `doneAt` is the game time the order ended (-1 while open).
- `MachineRow(pos, icon, name, state, item, left, reason, upkeep, upkeepCount, upkeepLevel, makes, problem)`: `makes` is the number of different output items, -1 for a Crafting Station (any pattern).
`StepRow`, `OrderRow` and `RecipeRow` are removed. The payload becomes `(terminal, online, reason, cpus, jobs, machines, patterns)`.

### Job wording (pure, JUnit)
`production/JobStatus` turns plain facts into a status key and arguments, no Minecraft imports: order state and reason, the first missing item and count, the first step that is not done (machine name, step state, step reason, verb, crafts done, crafts total) and whether that step's machine is out of upkeep. Precedence: done, cancelled, waiting for a CPU slot, missing material (only while the order is blocked, since the plan keeps what was missing when it was made), out of fuel, step reason (blocked), waiting for the machine, getting ingredients, working ("Crafting Station: crafting 1 of 2"). Crafts done = collected step output divided by the output per craft, capped at the batches.

The verb comes from the step recipe's type, looked up by recipe id in the server's recipe manager: crafting, smelting, blasting, smoking, campfire cooking, and the type path for other mods (crushing, assembling); unknown or not found gives "working". Patterns use vanilla crafting ids, so stations say crafting.

Progress: items made = the output collected by the steps that make the order's item, capped at the amount; a done job counts as full.

### Fading
The client hides a done or cancelled job once the client level's game time is more than 100 ticks past `doneAt`, and draws it fading over that time. Sending `doneAt` instead of an age keeps the signature stable so nothing is resent per tick. Orders without a `doneAt` stay until the CPU's book drops them.

### Machine states (pure ordering, JUnit)
Server side per member, in this order: a Crafting Station reports crafting with crafts left or idle; a Machine Adapter without a known machine reports no_machine; a member whose upkeep level is above zero and holds none reports out_of_fuel unless it is working; a member that is the producer of a step waiting with a reason reports blocked with that reason; a working producer reports crafting with the active output; no recipes reports no_recipes; a member without the producer role reports upkeep_only; otherwise idle. `production/MachineOrder` (pure) sorts problems (out_of_fuel, blocked, no_machine, no_recipes) first, then by position. Icon: the faced machine's block for an adapter, otherwise the member's block.

### Layout
`TerminalLayout.Production` gains row geometry relative to a row's top left on the list: `HEADER` (12 high); job row 22 high with `JOB_ICON`, `JOB_TITLE`, `JOB_BAR`, `JOB_STATUS`, `JOB_CANCEL`; machine row 20 high with `MACHINE_ICON`, `MACHINE_NAME`, `MACHINE_STATE`, `MACHINE_BAR`; plus `jobAreas()` and `machineAreas()`. `TerminalLayoutTest` asserts each row's parts are disjoint and inside the list width and the row height. Texts are cut to their rectangle width with `plainSubstrByWidth`.

### Screen
Tabs in order crafting, patterns, machines. The cancel button is a small X box; only it cancels, and only for open jobs. Hover on the header shows each CPU. Machine row tooltip: position, "can make N items" or "crafts any pattern", the blocked reason, and upkeep N of M.

### Signature
The menu's signature hashes CPU summaries, job id, state, crafted, status and args, machine state, item, left, reason, upkeep count, makes and problem, and the patterns as before.

### PLAN.md open decisions
None.

## Risks / Trade-offs

- [Server side names are English on a server with another language] -> same as the old rows; accepted for now.
- [Recipe type lookup per step per sync] -> one map lookup per open step every half second while a screen is open.
- [Orders the book keeps long after doneAt] -> the client hides them after the fade, so nothing piles up on screen.
