# Design

## Context

Today two paths make a controller item:

- **Mining**: the generated block loot `servus_shuttles:blocks/warehouse_controller` copies only `servus_foundation:wood` from the block entity (`CopyComponentsFunction.copyComponentsFromBlockEntity(...).include(WoodContent.WOOD)` in `ServusShuttlesBlockLoot`). Name, FE and rules are lost.
- **Carrot dismantle**: `WarehouseControllerBlock.onWrench` drops the upgrades, calls `clearEnergyForItem()` and writes the whole block entity into the item with `BlockEntityItems.saveToItem` (`saveCustomOnly`, `removeComponentsFromTag`, `BlockItem.setBlockEntityData`, then `collectComponents`). That is how the name, the stock rules and the team ride the item today. `setPlacedBy` resets the span, clears the FE again and sets the placer's team.

## Goals / Non-Goals

**Goals:**
- One mechanism for the name and the FE that serves both drop paths: data components collected from the block entity.

**Non-Goals:**
- Moving the stock rules into a component; they stay in the carrot's block entity data.

## Decisions

### Two components in `ServusShuttlesComponents`
- `servus_shuttles:site_name` (`String`, `Codec.STRING`, `ByteBufCodecs.STRING_UTF8`), like `SHUTTLE_NAME`.
- `servus_shuttles:controller_energy` (`Integer`, `Codec.INT` (non negative is enforced on apply), `ByteBufCodecs.VAR_INT`).

Both are persistent and network synchronized so the client tooltip sees them. Alternative: one record component with both fields; rejected because each value is optional on its own and two plain components read better in `/data` and in the loot include list.

### Implicit components on the block entity
`WarehouseControllerBlockEntity.collectImplicitComponents` adds `site_name` when the name is not empty and `controller_energy` when FE is above zero, next to the wood. `applyImplicitComponents` reads them: the name goes through `setSiteName` (cleaned, record synced), the FE through `ControllerEnergy.setStored`, which clamps to the capacity and tells the owner, so the wireless capable flag, the no power lamp and the network learn the new FE the same way as when FE arrives through the capability. `removeComponentsFromTag` discards `SiteName` and `Energy` so the carrot item holds each value once, in the component.

Verified in the 26.1.2 sources (`minecraft-patched-26.1.2.116-sources.jar`):
- `BlockItem.place` runs `updateCustomBlockEntityTag` (block entity data), then `updateBlockEntityComponents` (`BlockEntity.applyComponentsFromItemStack` → `applyComponents` → `applyImplicitComponents(DataComponentGetter)`), then `Block.setPlacedBy`. So components win over the carried tag and `setPlacedBy` runs last.
- `BlockEntity.collectComponents()` adds the stored components and then calls `collectImplicitComponents(DataComponentMap.Builder)`.
- `CopyComponentsFunction.BlockEntitySource.get` returns `blockEntity.collectComponents()`, so the loot table's `include` list sees implicit components.

### Placement no longer clears FE
`clearEnergyForItem()` goes away: `onWrench` no longer zeroes the FE before saving and `setPlacedBy` no longer zeroes it after placing. A freshly crafted item has no `controller_energy` component, so a new controller still starts empty.

### Loot table
`ServusShuttlesBlockLoot` gets its own table for the controller that includes `WOOD`, `SITE_NAME` and `CONTROLLER_ENERGY`; the controller leaves the plain `WOODEN` list.

### Tooltip
A new `WarehouseControllerItem extends WoodBlockItem` appends, after the wood line, "Site: <name>" and "<FE> FE stored" in gray, each only when its component is present. Registered in `ServusShuttlesItems.WAREHOUSE_CONTROLLER`. Lang keys `tooltip.servus_shuttles.warehouse_controller.site` and `.energy` via `ServusShuttlesLanguageProvider`.

### No double counting
The controller only receives FE, so carrying FE creates none. Breaking removes the site record (`setRemoved`), placing makes a new record at the new position from `onLoad`/`syncRecord`; the item itself is never a site. A mined item is one item: the loot table drops one stack, and the carrot path removes the block without loot (`removeBlock`), so no path yields two items with the same FE.

## Risks / Trade-offs

- [Mined and carrot items differ in what they keep: rules only by carrot] → stays as specified; recorded in PLAN.md as an open point if players ask.
- [Old carrot items in a dev world carry `SiteName` in block entity data and no component] → still loads through `loadAdditional`; nothing is released, no migration needed.

## Logic split

No new pure logic; clamping already lives in `ControllerEnergy`. Behavior is covered by GameTests: mine a named controller with FE (via `Block.getDrops` with the block entity, which runs the loot table), place the drop through `BlockItem.place` semantics (tag, components, `setPlacedBy`), assert name and FE; the same for the carrot; capacity clamp; and the updated wireless dismantle test.
