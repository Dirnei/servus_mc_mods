---
title: Elixir dose
description: How likely the Servus Alchemy Elixir of Sight poisons its drinker, and for how long.
sidebar:
  order: 8
---

The dose makes the poison: every drink of an elixir rolls a chance to poison the drinker. Elemental Sight is given whether the roll poisons or not. Numbers on this page are the shipped values of Alchemy 0.3.0-alpha; they live in datapack JSON and modpacks can change them.

```
data/<namespace>/servus_alchemy/elixir_dose/<name>.json
```

The file id names the elixir item: `data/servus_alchemy/servus_alchemy/elixir_dose/elixir_of_sight.json` is the dose of `servus_alchemy:elixir_of_sight`. An elixir without a file never poisons.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `poison_chance` | number, 0 to 1 | required | Chance that one drink poisons. |
| `extra_chance_while_sighted` | number, 0 to 1 | 0 | Added to the chance when the drinker still has Elemental Sight; the sum is capped at 1. |
| `poison_ticks` | int | 0 | How long Poison lasts; 0 gives none. |
| `nausea_ticks` | int | 0 | How long Nausea lasts; 0 gives none. |

Shipped dose:

| Elixir | Chance | Under the sight | Poison | Nausea |
|---|---|---|---|---|
| `elixir_of_sight` | 15 % | 30 % | 200 ticks (10 s) | 200 ticks (10 s) |

A poisoned player also sees Paracelsus scolding them in the action bar.
