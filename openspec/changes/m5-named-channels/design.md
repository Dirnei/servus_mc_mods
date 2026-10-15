# Design

## Context

Today `UtilityDuctBlockEntity` keeps a `byte[line][side]` channel, 0 to 7, saved only when not 0, and passes it into `DuctModelData.CHANNELS`. `DuctChannels` (pure) holds the eight ARGB colours and lang keys. `ChannelButton` draws one colour cell; `UtilityDuctScreen` lays out eight of them and sends `BUTTON_CHANNEL + line * 8 + channel` through `clickMenuButton`. `DuctBlockColor` tints the ring quads with `DuctChannels.color(duct.channel(line, side))`. The line networks build one endpoint per enabled side that touches a block (`LineNetwork.endpoint(..., int channel)`), and routing groups endpoints by that int (`RoundRobinPlan.Target(channel, priority, ...)`, energy and fluid likewise). Networks are runtime objects found with `NetworkSet.networkAt(pos)`; they merge, split and unload. Teams come from `Teams.teamOf(ServerPlayer)` (a UUID, the player's own without a team mod). World data uses `SavedData` with a `SavedDataType` and a codec on the overworld (`EnderAnchorData`).

Builds on `m5-duct-filter-picker` (applied first): `ItemPicker` and `PickerList` are in Foundation and used here for the icon. `m5-data-potato` follows and reads channel names for its inspect.

## Goals / Non-Goals

**Goals:**
- Channels as stable ids that survive network merges, splits and unloads.
- Routing code keeps comparing ints; only where the int comes from changes.

**Non-Goals:**
- No per network channel lists, no channels on the data line.

## Decisions

### D1: A channel is a global int id, Default is 0

Ids are allocated from a counter in the saved data and never reused; 0 is the Default channel and is never stored as an entry. A side keeps an `int` per line and side (the `byte[][]` becomes `int[][]`), saved only when not 0, as today. Keeping an int means routing, `RoundRobinPlan` and the model data stay as they are. Alternative: UUIDs. Rejected, larger in saves and payloads with no benefit inside one world.

### D2: Unknown ids resolve to Default on read

`UtilityDuctBlockEntity.channel(line, side)` returns the stored id only if the channel exists, else 0. Existence comes from a small `ChannelLookup` on both sides: the server's saved data, the client's `ClientChannels` cache, picked by `level.isClientSide()`. This covers deleted channels, sides in areas that were unloaded during a delete, and old saves (channels 1 to 7 load as unknown ids, so Default). Endpoints are built through this getter, so routing never sees a deleted id. Alternative: rewrite sides on delete. Rejected, unloaded ducts cannot be reached.

### D3: Saved data and pure list

- Pure, `foundation/logistics/channel/` (no Minecraft): `Channel(int id, UUID team, String name, int colour, String icon)` with `colour` 0 to 15 in dye order and `icon` an item id or null; `ChannelList` with `create`, `edit`, `delete`, `nameProblem(team, name, exceptId)` (empty, longer than 24, taken ignoring case within the team), `deletable(id, loadedUses, usesOnOpenSide)` (true when `loadedUses == 0` or `loadedUses == usesOnOpenSide == 1`), `forTeam(team)`, `matching(entries, query, usedIds or null)` sorted by name with Default first; `ChannelColours` with one ARGB per dye colour, black lifted to `0xFF2A2A2A` and Default white `0xFFF0F0F0`, so the ring stays visible on the dark duct.
- Minecraft: `DuctChannelData extends SavedData` on the overworld, codec over the entries and the next id, wrapping a `ChannelList`. All writes go through it on the server thread and end with a broadcast (D5).

### D4: Uses come from the line networks

`LineNetwork` gets `channelUses()`, a map from channel id to the number of its endpoints on that id (through a new abstract `channel(P)` per endpoint type). "This cable" is the key set of `channelUses()` of the network at the duct for the open line (`NetworkSet.networkAt`). Loaded uses for the delete rule are summed over every item, power and fluid line network of every server level. Only endpoints count: a side not touching a block carries no traffic, and if it names a deleted channel D2 puts it on Default. Alternative: scan every loaded duct. Rejected, it would need a server side tracker that the networks already are.

### D5: Payloads

| Payload | Direction | Content | When |
|---|---|---|---|
| `ChannelListPayload` | to client | every channel | join (`PlayerLoggedInEvent`), and to all players after any create, edit or delete |
| `CableChannelsPayload` | to client | per line of the open side: used ids on this cable; loaded use counts of the player's team channels | GUI open, after the player's own change, and every 20 ticks while open when it changed |
| `SetSideChannelPayload` | to server | line, channel id | pick in the popup |
| `CreateChannelPayload` | to server | line, name, colour, icon | save in the "+ New" form; creates and assigns |
| `EditChannelPayload` | to server | id, name, colour, icon | save in the edit form |
| `DeleteChannelPayload` | to server | line, id | trash |

Every server handler checks that the player has a `UtilityDuctMenu` open, that the line is present, that the channel belongs to the player's team (or is Default for `SetSideChannel`), and re-runs `nameProblem` and `deletable`; a refused request changes nothing. The form shows name problems from the same pure check against the synced list before sending, so the server refusal only matters on a race. `BUTTON_CHANNEL` and its button ids go.

### D6: Client cache and re-tint

`ClientChannels` holds the synced list. When a `ChannelListPayload` changes the colour of an id, it marks every loaded duct using that id dirty with `levelRenderer.setBlocksDirty` over `DuctTracker.live`, as `PanelReveal` does; a duct whose own channel changes keeps today's `sendBlockUpdated` path. `DuctBlockColor` reads `ChannelColours` through `ClientChannels.colour(id)`.

### D7: GUI

The channel row becomes one `ChannelButton` (swatch, icon, name) across the page width. It opens `ChannelPopup` (`foundation/logistics/client/`), built on `PopupPanel` like `ItemPicker`: search field, two state toggle "This cable" / "All" (default "This cable"), list rows with swatch, icon, name and, for team channels, pencil and trash (trash greyed with the use count tooltip when not deletable), and "+ New". The form (`ChannelForm`) has an `EditBox` for the name, a 2 by 8 grid of dye swatches, an icon button that opens the Foundation `ItemPicker` with every item plus "none", and Save and Cancel. Dye names come from the vanilla `color.minecraft.<dye>` lang keys.

### APIs

- Minecraft: `SavedData`, `SavedDataType`, `DimensionDataStorage.computeIfAbsent` (as `EnderAnchorData`), `DyeColor` order for the palette, `EditBox`, `LevelRenderer.setBlocksDirty`.
- NeoForge: `RegisterPayloadHandlersEvent` with `playToClient` and `playToServer`, `PacketDistributor.sendToPlayer` and `sendToAllPlayers`, `PlayerEvent.PlayerLoggedInEvent`, `ClientPacketDistributor.sendToServer`.

### Pure logic and tests

- JUnit: `ChannelListTest` (create, ids never reused, rename keeps id, name empty, too long, taken ignoring case, same name allowed in another team, delete rule with 0 uses, only the open side, 2 uses, Default never deletable or editable, `matching` by text and by used ids with Default first), `ChannelColoursTest` (16 colours, black not darker than the lifted value).
- GameTests: routing on created channels, delete falls back to Default, unknown id reads as Default. The popup, the form and re-tinting are checked in game.

### PLAN.md

No open decision covers this. Archiving removes the `m5-named-channels` open point from `PLAN.md`.

## Risks / Trade-offs

- [Every client gets every team's channels] → names are not secret in Servus; the list is small (tens of entries).
- [Delete counts only loaded endpoints] → unloaded sides fall back to Default by D2; the popup tooltip says "loaded".
- [Two channels with the same colour look alike in the world] → names are unique per team; the inspect of `m5-data-potato` names the channel.
- [GameTests that set channel 1 today] → they create a channel through `DuctChannelData` first and use its id.
