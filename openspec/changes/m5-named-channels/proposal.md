# Proposal

## Why

Duct channels are eight fixed colours (white, orange, magenta, light blue, yellow, lime, pink, cyan) known only by number. Red, green, blue, black and grey are missing, eight is a hard limit, and in a large base nobody remembers what channel 6 carries. Players want to create channels as they go, name them ("Iron ore", "Smelter feed", "Main grid") and give them a colour and an icon.

This is a follow-up to the done milestone M5 Logistics (channels), the second of three duct changes explored on 2026-10-15. It builds on `m5-duct-filter-picker` (the item picker in Foundation, used here for channel icons) and is applied after it; `m5-data-potato` follows.

## What Changes

- Channels become named entries in one channel list per team, kept in the world save. A channel has a name, one of the 16 dye colours and an optional item icon.
- A duct side stores a channel id. Every side starts on the Default channel: unnamed, white, shared by everyone, never deleted. A side whose channel no longer exists is on Default.
- The eight fixed channels go; sides saved on channels 2 to 8 load on Default.
- The endpoint GUI replaces the eight channel cells with a channel button showing the current channel (colour, icon, name). It opens a channel popup with a search field, a toggle "This cable" (channels used on this line of the connected run) or "All" (the team's list), and "+ New".
- "+ New" opens a form (name, colour palette, icon via the item picker) and assigns the new channel to the side at once. A pencil edits a channel; any team member may edit.
- A trash icon deletes a channel only when no loaded duct side uses it, or when the only loaded use is the side being configured, which then moves to Default.
- Rings at duct endpoints show the channel's colour and follow edits at once. Every client receives the channel list on login and after every change.
- Routing is unchanged: items, energy and fluid move between sides on the same channel, now compared by channel id.

## Capabilities

### New Capabilities

- `foundation/duct-channels`: the team channel list: Default channel, create, edit, delete rule, names, colours and icons, persistence and sync to clients.

### Modified Capabilities

- `foundation/utility-duct`: "Eight channels per line per side" is replaced by named channels per side; the Duct GUI shows the channel button and popup instead of eight cells.
- `foundation/duct-appearance`: the endpoint ring shows the side's channel colour and follows channel edits.

## Non-goals

- Channels per connected network with their own lists (decided against: networks merge and split, a team list is stable).
- Channels on the data line (it stays on or off).
- Free RGB colours; the palette is the 16 dye colours.
- Setting a channel by clicking a ring with a dye.
- Showing channel names in the world (that is inspection, `m5-data-potato`).
- Permissions beyond team membership.

## Impact

- `foundation/logistics/duct`: `DuctChannels` is replaced by the channel list; `UtilityDuctBlockEntity` stores an int channel id per line and side instead of a byte, resolves unknown ids to Default, `DuctModelData` carries ids.
- `foundation/logistics/network`: `LineNetwork` reports the channel ids its endpoints use (for "This cable" and for the delete rule); routing code unchanged apart from the id type.
- New: world saved data for the channel list, pure channel list logic with JUnit tests, payloads (list sync, create, edit, delete, set side channel, cable channels for the open GUI), a client channel cache, a channel popup and form widget.
- `foundation/logistics/client`: `UtilityDuctScreen`, `ChannelButton`, `DuctBlockColor` (colour from the client cache, re-tint on edits).
- Lang for Default, the popup, the form and the dye colour names; GameTests for channel routing switched to created channels.
- Depends on `m5-duct-filter-picker` being applied first (`ItemPicker` in Foundation).
