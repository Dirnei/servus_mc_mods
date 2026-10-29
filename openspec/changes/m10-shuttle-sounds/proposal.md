# Proposal

## Why

Shuttles glide through the warehouse in complete silence, so a player cannot hear a fleet at work or notice a shuttle passing behind them. A quiet drive sound gives the warehouse life; a Muffler lets players who dislike it turn it down for one shuttle or a whole garage. The Shuttle Workshop, where the Muffler goes in, also gets a small rework: the shuttle turns on its board, and the two build buttons become one Build button with a Skip for players who do not like the minigame.

This extends PLAN.md M10 Field work (Shuttle Workshop, shuttle looks, hand build minigame), which is otherwise done.

## What Changes

- Moving shuttles play a quiet looping drive sound: our own recording, a wandering low rumble with sparse dull wheel clicks and a loose rattle now and then. Only the nearest few moving shuttles sound at once, each with its own start point and a slightly different pitch, and the pitch rises with speed. A standing or parked shuttle is silent.
- New **Muffler** item, craftable. It fits in two places: installed into a shuttle through the workshop install slot (next to its tool and counts, with a remove button and a tooltip line), and in a new Muffler slot in the Shuttle Garage GUI that muffles every shuttle of that garage. A muffled shuttle plays a damped version of the drive sound, much quieter but not silent.
- The Shuttle Workshop's large shuttle view turns slowly while no build runs and can be turned by dragging it; a build by hand swings it back to the work angle and holds it there.
- **Build now** and **Build by hand** become one **Build** button that starts the minigame. While the minigame runs, the button reads **Skip**: it finishes the build at once like the old Build now, with no name prompt, and the shuttle keeps its old name plate. Cancel stays.
- A sound generator script under `tools/shuttle-sounds/` makes the drive sound files, like the texture generators.

## Capabilities

### New Capabilities
- `shuttles/shuttle-sounds`: the drive sound of moving shuttles, how many sound at once, and how the Muffler on a shuttle or a garage damps it.

### Modified Capabilities
- `shuttles/shuttle-workshop`: shuttle modules gain a Muffler flag; the install slot takes the Muffler and the GUI shows it with a remove button; the large shuttle view turns.
- `shuttles/shuttle-hand-build`: one Build button starts the minigame and turns into Skip; Skip finishes without a name prompt.
- `shuttles/shuttle-tiers`: the Tier builds requirement names the Build button and Skip instead of Build now and Build by hand.
- `shuttles/shuttle-garage`: the garage GUI gains a Muffler slot that keeps its Muffler with the block.

## Non-goals

- No sounds for stops, loading, lift cars, the garage shutter or the Fluid Rack coupling; only the drive loop.
- No client config volume setting; the two Mufflers cover the choice. A setting can come later if players ask.
- No silent mode: a muffled shuttle is quiet, never mute.
- No change to the minigame steps themselves.

## Impact

- `shuttles`: `ShuttleModules` (new muffled field, codec default false so old shuttles load unchanged), `ShuttleUpgrades`, `ShuttleWorkshopMenu` and `ShuttleWorkshopScreen`, `GuiLargeItemRenderState` and `GuiLargeItemRenderer` (yaw), `ShuttleGarageBlockEntity` and `ShuttleGarageMenu` and its screen, `ShuttleSyncPayload` (garage muffled flag), `ShuttleGarageRenderer` (reports positions for sounds), a new client `ShuttleSounds`, `ServusShuttlesSounds` and the sounds, item, recipe, model and lang datagen providers, new `.ogg` files under `assets/servus_shuttles/sounds/`.
- `tools/shuttle-sounds/gen.py` (Python with numpy, ffmpeg) for the sound files.
- Shuttles CHANGELOG under Unreleased: drive sound, Muffler, turning workshop view, Build and Skip.
