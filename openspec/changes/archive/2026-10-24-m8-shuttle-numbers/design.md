## Context

Shuttles are items in Shuttle Garage slots (`ShuttleGarageBlockEntity.shuttles`, an `ItemSlots`). The fleet copies the slot stacks; `ShuttleView` syncs the shuttle stack of every drawn shuttle to clients. A name plate is the `shuttle_name` data component and `ShuttleItem.getName` shows it as "Berta (Shuttle Mk2)". Tier builds use `ItemStack.transmuteCopy`, which keeps every component. This change is the first of five in the shuttle job series (decided 2026-10-23); `m8-slot-locks` will name the shuttle of a lock with the label built here.

## Goals / Non-Goals

**Goals:**
- A per world counter and a number component, given in the garage.
- One label helper usable on server and client.
- Numbering of shuttles already in garages of an old world.

**Non-Goals:**
- Renumbering, a command to set numbers, or number reuse.
- Showing the number on the shuttle model in the world.
- Changing what the Field Head names (it keeps the plate, see Open Questions).

## Decisions

- **Counter in SavedData on the overworld.** `ShuttleNumberData extends SavedData`, stored as `servus_shuttles_shuttle_numbers` in the overworld's data storage through `ValueIo.tagSavedData`, like `SiteRegistry`. It keeps the last given number (`Last`); one counter for every dimension of a world. The pure counting rule lives in `ShuttleCounter` (no Minecraft imports): `take()` returns last + 1 and remembers it; the SavedData wraps it and marks itself dirty.
- **Component `shuttle_number`** (`Integer`, `Codec.INT` persistent, `ByteBufCodecs.VAR_INT` synced) in `ServusShuttlesComponents`. A missing component means not numbered yet, so old items and saves need no migration.
- **Where numbers are given.** `ShuttleGarageBlockEntity` numbers a slot's stack in `onContentsChanged` when its level is a `ServerLevel`, before it tells the network, so the fleet copy already carries the number. The live stack is changed in place (`stack.set`), which the open menu syncs like any slot change. `onLoad` numbers every slot in slot order before `super.onLoad()` joins the network; that covers old worlds, because the slots are filled during `loadAdditional` while the block entity has no level yet. Client copies of the garage never number.
- **Label.** `ShuttleItem.label(ItemStack)` returns a `Component`:
  - number and plate: `shuttle.servus_shuttles.label.named` = "%s (#%s)"
  - number only: `shuttle.servus_shuttles.label` = "Shuttle #%s"
  - plate only: the old "Berta (Shuttle Mk2)" form
  - neither: the item's description name ("Shuttle Mk1")
  A translatable component is resolved by the reader's client, so the same helper serves a server built payload (lock tooltips later) and client screens. The pure choice between the four forms is `ShuttleLabelForm.of(boolean plate, boolean number)`, covered by JUnit.
- **Item name and tooltip.** `ShuttleItem.getName` returns the label. A numbered shuttle's tooltip starts with a gray line naming the tier (`item.servus_shuttles.shuttle_mkN`), because the label leaves the tier out. An anvil name still wins, as vanilla `getHoverName` prefers `CUSTOM_NAME`.
- **Garage GUI.** The slots show the shuttle item, so hovering shows the label as title; no extra drawing.
- **ShuttleView.** Already carries the shuttle stack with its components, so clients can label every drawn shuttle without a format change.

## Risks / Trade-offs

- Creative copies (middle click, `/give` with components) duplicate a number. Accepted: numbers name shuttles for players, nothing keys on them being unique yet. `m8-job-persistence` keys jobs by garage slot, not by number.
- Changing the stack inside `onContentsChanged` runs after the transfer transaction commits; only the component changes, the slot keeps the same stack.

## Assumptions

- "Per world" means one counter per save, shared by all dimensions.
- The tier moves from the name to the first tooltip line for numbered shuttles; unnumbered shuttles keep their old name.
- The Shuttle Workshop shows the hover name, so it shows the label for numbered shuttles too; no further workshop change.

## Open Questions

- The Field Head names its worker by plate only (a server built string). Naming it by label needs the payload to carry the number; left for a later change if wanted.
