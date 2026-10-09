# Proposal: m9-elixir-of-sight

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 5: `m9-elixir-of-sight`.

## Why

PLAN.md 2.1: Paracelsus' elementals live in their element unseen, and only Elemental Sight shows them. The Gnome Hollow of the next change is a plain block whose vein and gnome are drawn only under the sight, so the sight and its client hook come first. The sight comes from the Elixir of Sight, brewed from the principles.

## What Changes

- **Elemental Sight**: a beneficial mob effect.
- **Elixir of Sight**: a drink (like a potion: drunk over a moment, leaves a glass bottle) that gives Elemental Sight for a time (example 3 minutes), made on an Albedo Athanor from Quicksilver and the three principles.
- **Client reveal hook**: a small client registry of reveal renderers that draw in the world only while the local player has Elemental Sight, the same render stage the duct overlay uses. The Gnome Hollow registers the first one.
- **Journal**: drinking the elixir reports a `sight` event; Schwaz gets a second task, drinking the Elixir of Sight. The book tells of it.

## Capabilities

### New Capabilities
- `manufacturing/elemental-sight`: the effect, the elixir and the client reveal hook.

### Modified Capabilities
- `manufacturing/journal`: the `sight` event and Schwaz's second task.

## Non-goals

- Overdose effects ("the dose makes the poison") and the Alchemist's Spectacles: still open in PLAN.md section 5.
- Anything for the reveal hook to draw; the Gnome Hollow comes next.

## Impact

- Code: new `alchemy/sight` package (effect, item), client `sight` package (hook and render event), registration, an Athanor recipe, journal event.
- Data: lang, item model and placeholder textures, the effect icon, the Athanor recipe.
- Docs: Manufacturing page.
