# Design: m9-elixir-of-sight

## Context

See proposal.md for why. The Athanor (`m9-athanor`) crafts by data with a tier; the journal (`m9-journal`) records events per team. Foundation's `DuctOverlayRenderer` shows the client side pattern: a `RenderLevelStageEvent` handler at `AFTER_TRANSLUCENT_BLOCKS` that draws only when a condition holds for the local player.

## Goals / Non-Goals

**Goals:** the effect, the elixir, its Athanor craft, the reveal hook, the journal event.

**Non-Goals:** overdose, spectacles, any revealed content.

## Decisions

### D1: Effect and item

`ElementalSightEffect extends MobEffect` (category `BENEFICIAL`, pale blue), registered in `Registries.MOB_EFFECT`; the `DeferredHolder` is the `Holder<MobEffect>` that `MobEffectInstance` takes in 1.21.1. `ElixirOfSightItem extends Item` with `UseAnim.DRINK`, 32 ticks use, `finishUsingItem` adds the effect (duration from the item's own constant, example 3,600 ticks), reports the journal event for a `ServerPlayer`, and returns a glass bottle (or adds it to the inventory when the stack is not used up), like `HoneyBottleItem`.

Alternative: a vanilla potion with a custom `Potion`. Rejected: brewing stand recipes would bypass the Athanor.

### D2: Reveal hook

`ElementalSight.active(Player)` checks the effect. Client: `RevealRenderers` holds a list of `RevealRenderer` (functional: `render(RenderLevelStageEvent event)`); `RevealEvents` subscribes to `RenderLevelStageEvent`, returns early unless the stage is `AFTER_TRANSLUCENT_BLOCKS` and the local player has the sight, then calls each renderer.

### D3: Recipe and journal

Athanor craft `athanor/elixir_of_sight`: Quicksilver, 100 Salt, 100 Sulfur, 250 Mercury, tier 1, 200 ticks. `Journal.recordEvent(level, team, "sight", 1)` (no filter); Schwaz gains `{"id": "sight", "event": "sight", "count": 1}`; the Schwaz book entry describes the elixir.

### APIs

`MobEffect`, `MobEffectInstance`, `Registries.MOB_EFFECT`, `Item.finishUsingItem`, `UseAnim.DRINK`, `RenderLevelStageEvent`. All vanilla or already used in the repository.

### Pure and Minecraft logic

No pure logic. GameTest: the elixir item adds the effect to a mock player and returns a glass bottle; the Athanor makes the elixir at Albedo but not at Nigredo; drinking as a team member counts for Schwaz (event recorded through `Journal.recordEvent` with the team, since a mock `ServerPlayer` triggers Modonomicon sync on advancement awards). Manual: effect icon, reveal hook (with the Gnome Hollow).

## PLAN.md decisions settled

PLAN.md 2.1 named the Elixir of Sight; settled as a drinkable item made on an Albedo Athanor. Section 5's overdose and spectacles questions stay open.

## Assumptions to confirm with the user

1. **Duration** 3 minutes, stack size 16.
2. **Recipe**: Quicksilver with 100 Salt, 100 Sulfur, 250 Mercury on an Albedo Athanor, 10 seconds.
3. **No overdose** yet; drinking again only refreshes the duration.
4. **Schwaz's second task**: drink the Elixir of Sight once.
5. **Placeholder art** for the elixir and the effect icon.

## Risks / Trade-offs

- [The sight shows nothing until the Gnome Hollow exists] → The next change registers the first reveal renderer.

## Migration Plan

None.
