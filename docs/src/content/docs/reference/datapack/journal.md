---
title: Journal chapters
description: The chapter JSON of Paracelsus' Journal, the tech tree of Servus Alchemy.
sidebar:
  order: 3
---

Paracelsus' Journal is Alchemy's tech tree. A chapter lists tasks; when a team has done all of them, the chapter is finished for the team, every member gets the chapter's advancement and the next chapter opens. Counts on this page are the shipped values of Alchemy %alchemy_version%; they live in datapack JSON and modpacks can change them.

## File location

```
data/<namespace>/servus_alchemy/journal/<chapter>.json
```

Note the extra `servus_alchemy` folder. The chapter id is `<namespace>:<chapter>`. Chapters reload with `/reload`.

## Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `parent` | chapter id | none | The chapter that must be finished before this one counts anything. |
| `order` | int | 0 | Sort order in the journal command. |
| `tasks` | list | empty | What the chapter asks for. A chapter without tasks never finishes by itself. |

Each task:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `id` | string | required | Unique within the chapter, shown in `/servus_alchemy journal`. |
| `event` | string | required | `calcine` (items calcined), `principle` (mB of a principle made at the Calciner), `purify` (mB of Salt washed out at the Purifier), `coagulate` (items coagulated), `athanor` (Athanor crafts), `sight` (Elixirs of Sight drunk), `bind` (gnomes bound), `ripen` (items a ripe vein yields, by item), `press` (items pressed at the Prelum, by result item), `putrefy` (items the Venter Equinus makes) or `find` (a trace picked up, by item; the old gnome found, filter `servus_alchemy:old_gnome`). |
| `filter` | string | any | An item id or `#tag` for item events (`calcine`, `coagulate`, `ripen`, `press`, `find`), a principle (`salt`, `sulfur`, `mercury`) for `principle`, `tier:<name>` or an item for `athanor`, or the name for `find`. `bind`, `purify`, `putrefy` and `sight` take no filter. |
| `count` | int | 1 | How much the task needs. 1 makes it a first time task. |

Only machines placed by a player count, for that player's team at the time of placing.

## Advancements and the book

A finished chapter grants the advancement `<namespace>:journal/<chapter>`. The journal book unlocks the entry of a chapter when the reader has the advancement of its parent. A pack that adds a chapter adds the advancement (with a single `minecraft:impossible` criterion named `done`) and the book entry as well.

## Built in chapters

| Chapter | Parent | Tasks |
|---|---|---|
| `villach` | none | `calcine` 100, `principle` salt 1,000, `purify` 1, `coagulate` `servus_alchemy:salt_crystal` 1 |
| `schwaz` | `villach` | `athanor` `tier:albedo` 1, `sight` 1, `bind` 1 |
| `wandering_years` | `schwaz` | `athanor` `tier:citrinitas` 1, `ripen` `minecraft:raw_gold` 1, `bind` 4 |
| `basel_salzburg` | `wandering_years` | `athanor` `tier:rubedo` 1, `ripen` `minecraft:diamond` 1, `athanor` 16, `press` `minecraft:netherite_scrap` 1 |
| `old_gnome` | `schwaz` | `find` `#servus_alchemy:hint_traces` 1, `find` `servus_alchemy:old_gnome` 1 (a side chapter: no chapter has it as parent) |

## Locked recipes

A [coagulation recipe](/servus_mc_mods/reference/datapack/recipes/) may name a `chapter`; a Coagulator grows it from its seed only after its owner's team has finished that chapter.
