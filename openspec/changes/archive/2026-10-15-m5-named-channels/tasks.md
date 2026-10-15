# Tasks

## 1. Pure channel list

- [x] 1.1 Add `foundation/.../logistics/channel/Channel.java`, `ChannelList.java` and `ChannelColours.java` (design D3), no Minecraft imports. JUnit in `foundation/src/test/java/.../logistics/channel/`: `ChannelListTest` (`createAllocatesNewIds`, `idsNeverReused`, `renameKeepsId`, `emptyNameRefused`, `longNameRefused`, `takenNameIgnoresCase`, `sameNameInOtherTeam`, `deletableWhenUnused`, `deletableWhenOnlyOpenSide`, `notDeletableWithOtherUses`, `defaultNeverEditedOrDeleted`, `matchingByText`, `matchingByUsedIds`, `defaultListedFirst`) and `ChannelColoursTest` (`sixteenColours`, `blackIsLifted`). Verify: `gradlew :foundation:test` green.

## 2. Saved data, lookup and sync

- [x] 2.1 Add `foundation/.../logistics/channel/DuctChannelData.java` (SavedData on the overworld, codec over entries and next id) and `ChannelLookup` with the server side (design D1, D3). Verify: `gradlew build` green.
- [x] 2.2 Add `ChannelListPayload` (to client), `ClientChannels` (client cache, `ChannelLookup` client side, re-tint of ducts on colour changes, design D6), send on `PlayerLoggedInEvent` and after every change with `PacketDistributor.sendToAllPlayers`; register in `foundation/.../data/ServusFoundationPayloads.java`. Verify: `gradlew build` green.

## 3. Ducts on channel ids

- [x] 3.1 `foundation/.../logistics/duct/UtilityDuctBlockEntity.java`: `int[][]` channels, `setChannel` without clamping, `channel(line, side)` resolves unknown ids to 0 through `ChannelLookup` (design D2), save and load as ints; `DuctModelData` unchanged in shape. Delete `DuctChannels.java`; `DuctBlockColor` uses `ClientChannels.colour`. Update `ServusLogisticsGameTests` and `PumpGameTests` to create channels in `DuctChannelData` instead of `setChannel(..., 1)`. Add GameTests `unknownChannelReadsDefault` and `routingOnCreatedChannel` in `ServusLogisticsGameTests`. Verify: `gradlew build` green, GameTests pass in `gradlew :foundation:runGameTestServer`.
- [x] 3.2 `foundation/.../logistics/network/LineNetwork.java`: `channelUses()` (design D4); a helper `ChannelUses` that sums loaded uses over all line networks of all server levels. GameTest `channelUsesCountEndpoints` (two chests on one created channel give 2, a side without a block gives nothing). Verify: `gradlew build` green, GameTests pass.

## 4. Server requests

- [x] 4.1 Add `SetSideChannelPayload`, `CreateChannelPayload`, `EditChannelPayload`, `DeleteChannelPayload` (to server) and `CableChannelsPayload` (to client) with their checks (design D5); `UtilityDuctMenu` drops `BUTTON_CHANNEL`, sends `CableChannelsPayload` on open, after the player's change and every 20 ticks while it changed. GameTests in `ServusLogisticsGameTests`: `createAssignsToSide`, `deleteOnlyOpenSideFallsBack`, `deleteRefusedWhileUsed`, `otherTeamCannotEdit` (mock players; the request methods in `ChannelRequests` take the team id, so tests pass team ids directly instead of installing a team provider). Verify: `gradlew build` green, GameTests pass.

## 5. GUI

- [x] 5.1 `foundation/.../logistics/client/ChannelButton.java` becomes the wide channel button (swatch, icon, name); `UtilityDuctScreen` lays it out instead of the eight cells and moves the filter row if needed. Verify: `gradlew build` green; in game the button shows Default on a new duct.
- [x] 5.2 Add `ChannelPopup` and `ChannelForm` in `foundation/.../logistics/client/` (design D7): search, This cable / All toggle, rows with pencil and trash, "+ New", form with name, 16 swatches and icon via `ItemPicker`, name problems shown from `ChannelList.nameProblem`. Lang keys in `LogisticsDataParts.language` (Default, popup, toggle, form, problems, trash tooltip), run `gradlew :foundation:runData`. Verify: `gradlew build` green; in game: create "Iron ore" red with iron ore icon on one chest side, pick it on a second side via "This cable", items flow; recolour to blue and both rings turn blue; trash is disabled with "used on 2 loaded sides"; a black channel's ring is visible on the duct.

## 6. Plan and integration

- [x] 6.1 `PLAN.md`: keep the `m5-named-channels` open point until archive; nothing else changes. Verify: `openspec validate m5-named-channels --strict` passes.
- [x] 6.2 Full `gradlew build`, `gradlew test` green, the GameTests pass, and the client launches with all three mods (play check from main by the user).
