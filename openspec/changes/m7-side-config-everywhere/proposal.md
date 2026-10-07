# Proposal

## Why

Side configuration exists three times: Manufacturing machines use the Lib side config with a 3 by 3 panel beside the GUI, the Storage stations got their own world side modes with a row of six buttons, and the Logistics duct has a third variant. Play test feedback (2026-10-07): it must look and work the same everywhere, the Inbound Station must be able to pull from a neighbour instead of only accepting pushes, and machines such as the Assembler need both directions on one side. This change makes the Lib side config the one model, adds the active modes pull and push to it, ships one reusable side panel widget in Lib, and moves machines, both stations and the Crafting Station onto it.

## What Changes

- **One vocabulary.** The Lib side mode gains Pull and Push. The seven modes are Auto, Input, Output, Both, Pull, Push and Off. Input and Output are passive (neighbours insert or extract), Pull and Push are active (the block takes from or gives to the neighbour every few ticks, and the matching passive right comes along), Both is passive in and out, Auto is the block's unconfigured behaviour. Every block declares which modes it supports and its default; cycling moves through the supported modes only.
- **One widget.** Lib ships a side panel: the 3 by 3 grid of relative sides (front, back, left, right, top, bottom) with a colour per mode, a letter per side, a tooltip and a click that cycles the side through a menu button. It sits left of the GUI on every block that has sides.
- **One carrot gesture.** The Configuration Carrot on a face cycles that face's mode on every configurable block, with the result in the action bar. Sneak plus carrot dismantles and keeps the configuration in the dropped item. Machines are no longer rotated by the carrot; like the stations their facing is set at placement.
- **Machines pull and push explicitly.** Output no longer pushes; Push does. Pull takes items the machine's inputs accept from the neighbour. Auto keeps today's behaviour (passive both ways and outputs pushed).
- **Stations on the shared model.** Inbound Station: Input, Pull, Off, default Input. Outbound Station: Push, Output, Off, default Off with the front on Push. Both use relative sides, the Lib panel and menu data slots instead of the six button row and the payload string.
- **Crafting Station configurable.** The same modes and defaults as a machine.

## Capabilities

### Modified Capabilities
- `lib/side-config`: the seven modes, supported sets and defaults, the side panel widget, the carrot gesture.
- `manufacturing/machines`: side panel from Lib, pull and push semantics, carrot cycles sides instead of rotating.
- `storage/inbound-station`: Input, Pull, Off through the shared model and panel.
- `storage/outbound-station`: Push, Output, Off through the shared model and panel.
- `storage/crafting-station`: side configuration like a machine.

## Non-goals

- The Utility Duct. `m5-duct-rework` is in flight and defines the duct's per line, per side modes; moving its endpoint GUI onto the Lib panel is a follow up after that change is archived.
- Per side filters or priorities on machines and stations.
- Rotating placed blocks with the carrot. Placement sets the facing.

## Impact

- `lib`: `SideMode` (two modes, flags), `SideConfig` (supported set, default, cycle within the set), `SideConfigData` (menu data slots), `client/SideConfigPanel`, lang keys for sides and modes, unit tests. The Logistics switch over the enum gets the two new cases so it keeps compiling; its behaviour is unchanged.
- `manufacturing`: `MachineScreen` draws the Lib panel, `MachineBlockEntity` pulls on Pull sides and pushes only on pushing modes, `MachineBlock.onWrench` cycles the clicked side; lang keys move to Lib; a GameTest for pulling.
- `storage`: `SideModes`, `CycleSidePayload` and `SideButtons` go; stations and the Crafting Station hold a Lib `SideConfig`; menus expose it through data slots; screens draw the Lib panel and get their compact layouts back; the inbound station pulls on Pull sides; GameTests for inbound pull and crafting station sides.
- `PLAN.md`: section 2.1 bullet on side configuration mentions the shared panel and the carrot gesture.
