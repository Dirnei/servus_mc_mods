# Servus: Alchemy Plan

The current plan for the progression of Servus: Alchemy, as decided together. `PROGRESSION.md` is the discussion paper with the history, the ideas and the story background. This file holds what is decided and what is still open.

Numbers are examples. The real ones live in datapack JSON.

---

## Story

- **The player found Paracelsus' journal** after his death and discovers what is written in it, step by step.
- **The voice is his own diary:** confused notes, rants and crash-outs. Paracelsus was high on his own elixirs and a bit mad.
- **The dose makes the poison:** drinking the Elixir of Sight has a random chance to poison the player.
- **The homunculus is the dark secret.** Paracelsus praises it in the journal and never says it is a slave. Only under Elemental Sight does the player see it chained to the Hollows. By then the player no longer needs to drink the Elixir, so seeing the truth is a choice.
- **The star.** The homunculus sometimes whispers about a star. The journal never mentions it and no recipe viewer shows it. A Nether Star frees the homunculus: its output doubles and it tells the player **the secret**, the real truth rather than Paracelsus' version. *(The secret itself is open, see below.)*

## Elementals

- **Gnomes** (Albedo): grow and work ore veins. Classic look: red pointed hat, beard, round nose.
- **Salamander** (Citrinitas): a pet found in the Nether's lava, optional for progression, needed for the Ignis Perpetuus. See Citrinitas.
- **House Gnome:** Paracelsus' old gnome from the mines of Schwaz, found deep in the deepslate through the hints (see "Hints for hidden things"). He recognises the journal and moves in with the player. A second voice next to the journal, who knew the madman. What he does is open.
- Undines and Sylphs are not part of the plan.

---

## The stages

| Stage | Verb | Made in this stage | New | Athanor ring to reach the next stage |
|---|---|---|---|---|
| Nigredo | break down | Clay, Bone Meal | Calciner, Purifier, Coagulator, Athanor | Salt Crystal Block |
| Albedo | grow | Iron, Copper | Gnome Hollow, Elixir of Sight, Gnomes | Amethyst Shard |
| Citrinitas | smelt | Gold, Redstone, Diamond | Principle Dropper, Salamander (Salamander Forge, Receptaculum), trip to the Nether | Crying Obsidian |
| Rubedo | purify | Netherite | Prelum, trip to the End | none, the end goal |

```
 Overworld ------------------> Nether -------------------------> End
 Nigredo, Albedo               Citrinitas                        Rubedo
                               Crying Obsidian opens Rubedo      Dragon's Breath builds the Prelum
                                                                 Caput Mortuum --> Prelum --> Netherite
```

### Nigredo: break down
- Built today: Calciner, Purifier, Coagulator, Athanor.
- Products: Clay, Bone Meal and the first goods from the principles.
- Caput Mortuum from the Purifier piles up with no use until Rubedo.
- Ring: Salt Crystal Blocks.

### Albedo: grow
- Gnome Hollow, Elixir of Sight, gnomes (see "Gnome Hollow" below).
- Seeds: Iron, Copper.
- Ring: Amethyst Shards.

### Citrinitas: smelt
- Seeds: Gold, Redstone, Diamond.
- **Principle Dropper:** fed by pipes, drips the principles onto the Gnome Hollow below it. This automates the feeding without any pipe touching the Hollow.
- **The player has to go to the Nether** to progress.
- **Salamander (pet):** salamanders live in the Nether's lava lakes and basalt deltas. Without Sight only an odd flame dances on the lava; with Sight the salamander shows. Thrown Gold Ingots befriend it after a few tries, like a wolf with bones. A befriended salamander shows itself to everyone, follows the player, sits on command, likes lying in a campfire and gives off light.
- **Sal Ammoniac:** a befriended salamander fed Gold Ingots digests them into Sal Ammoniac. (Historically alchemists got sal ammoniac from the soot of burnt camel dung, *to check*.)
- **Ignis Perpetuus:** Sal Ammoniac and Sulfur at the Athanor make the eternal flame. It builds the **Salamander Forge**, an infinite furnace, and the **Receptaculum**. Players who skip the salamander still reach Rubedo, but without automated extraction from the Hollows.
- The loop: gnomes grow gold, the salamander turns gold into Sal Ammoniac, the eternal flame heats the Receptaculum, and the homunculus automates the gnomes.
- Ring: **Crying Obsidian** (Ruined Portals, Piglin bartering, Bastion chests). Ask for more than a Ruined Portal usually holds, so the Nether trip is the normal way.

### Rubedo: purify
- **Prelum (the Press):** built once with Dragon's Breath, a one-time cost. It presses Caput Mortuum into Netherite.
- End goal: Netherite from Caput Mortuum.

---

## Hints for hidden things

**Rule: everything hidden behind Elemental Sight shows a hint without it,** so the player knows when to drink. The hints come in three steps, from far to close:

```
   FAR                   NEAR                      HERE
   knocking in the  -->  dust trickles from   -->  a trace: a tiny pickaxe, a scrap
   rock, with a          one block, a pebble       of red felt, a little bowl of
   direction             falls, a light flickers   salt left as an offering
                                    |
                                    v
                       drink the Elixir and see it
```

- **Knocking** has a direction to follow and a subtitle ("Something knocks in the rock"). It comes from mining folklore: the Knockers in Cornwall and the Bergmaennlein of the German speaking mines *(to check)*.
- **Dust** marks the exact block when the player is close.
- **Traces** are small items to pick up. The journal reacts to them ("They are near. Drink, and look.").

| Hidden thing | Hints without Sight |
|---|---|
| Paracelsus' old gnome (House Gnome) | knocking, dust and traces in the deepslate |
| Salamander in Nether lava | a flame that dances oddly on the lava |
| Gnome at work in a Hollow | soft knocking from the stone now and then |
| Homunculus in the Vas | the liquid sometimes moves by itself |

**The old gnome without world generation:** once the player has reached the right chapter, each deepslate block they break has a small chance to wake the knocking. The mod then turns a deepslate block in a wall nearby into the ancient Hollow, and the hints lead there. It reacts to block breaks, so nothing scans the world, and every player finds him wherever they mine.

---

## Gnome Hollow

| | Without Sight | With Elemental Sight |
|---|---|---|
| Look | stone | stone with ore buds and a working gnome |
| Jade | "Stone" | the Hollow, its vein and ripeness |
| Right click | nothing | seed it, pour principles in by bucket, take items out |
| Break | drops stone, the vein is lost | drops a fresh, empty Hollow; moving it means the whole setup again |

- No screen, no pipes, no fluid or item capability.
- The gnome fills a small internal store and stops when it is full. Only a hand under Sight or a homunculus empties it.
- **Gnome upkeep for speed:** a gnome that gets something it likes works faster. Without gifts it still works at the base rate. *(What gnomes like is open.)*

## Vas Hermeticum and Receptaculum

The homunculus' workplace is a stack of two blocks:

```
   [ VAS HERMETICUM ]   the sealed glass vessel, the homunculus lives in it
   [  RECEPTACULUM  ]   the heated receiver below it, catches the yield
```

- **Vas Hermeticum:** the Sealed Flask from the Athanor (Glass Bottle, Mercury, Bone Meal, Crying Obsidian), placed as a block on top of the Receptaculum. The homunculus grows in it for a few in-game days while fed the Elixir. No screen. Without Sight it is a glass vessel with murky liquid; with Sight the homunculus is seen inside, with chains running to the Hollows. ("Hermetically sealed" comes from alchemy, named after Hermes Trismegistus, *to check*.)
- **Receptaculum:** a Hearth Chest: a brick hearth with a grated fire at the front, an iron-bound deepslate chest on top with a deepslate tile lid and a brass collar for the Vas. Our own texture and the only screen: the yield, the gifts for the gnomes and the **Elixir slot**. It is not hidden; only the homunculus is.
- **Reach: 5x5 on the Receptaculum's level,** centred on the stack.
- **Moves both ways:** takes the yield out of every Hollow in reach into the Receptaculum, and brings gifts to the gnomes (upkeep for speed).
- **Elixir slot:** the Elixir of Sight in it is used up over time. Without Elixir the homunculus stops.
- The Receptaculum is a normal inventory for pipes, so pipes and the Crafting CPU reach grown metal only through it.
- **Heat is required, with no bonus:** the Receptaculum has its own fire that keeps the homunculus warm, as the legend asks for. It is built with Ignis Perpetuus, like the Salamander Forge, and its look shows the heating. Under Sight the salamander is seen in its fire.
- The homunculus needs Crying Obsidian, so it arrives in late Citrinitas or Rubedo.
- **Freeing it with a Nether Star** used on the Vas: the seal breaks and the glass stands open (visible even without Sight), output doubles and it tells the secret.

---

## Changes to what is built

- Gnome Hollow: drop the screen, the sample and output slots, the tanks, the fluid capability and the output push; add the internal store, right click interactions under Sight, the Jade disguise and the stone drop.
- The Hollow's production capability for the Crafting CPU moves to the Receptaculum.
- New: Principle Dropper, Vas Hermeticum with the homunculus, Receptaculum, Salamander pet, Sal Ammoniac, Ignis Perpetuus, Salamander Forge, House Gnome, Prelum, Athanor rings of Amethyst and Crying Obsidian, the poison chance of the Elixir, the hints for hidden things (knocking, dust, traces).

---

## Open

Story (for the storyteller):
- The secret the freed homunculus tells.
- What gnomes like as gifts, and whether each gnome has a name and mood.
- What the House Gnome does (comments on the journal, reads ore samples, keeps watch?), his name, and whether his young apprentice with the lantern leads the player to him.
- Which chapter wakes the knocking for the old gnome.
- How the journal ends.
- The homunculus' whispers about the star.
Play:
- Does the Prelum make Netherite Scrap or Netherite Ingots? What does one press cost in Caput Mortuum?
- Is Ignis Perpetuus also an endless fuel (for example in the Calciner), or only a building part?
- Is the Philosopher's Stone still in the plan? Is "make anything learned by burning" dropped?
- Our own recipe for Crying Obsidian (proposal: Obsidian, Ghast Tear and Mercury at the Athanor), or only vanilla sources?
- How fast the poison chance of the Elixir is, and what it does.
- ATM11: Allthemodium and later ores after Netherite, as later gnome seeds by tag?

Technical:
- Jade API for showing "Stone" instead of the Hollow (verify before the design).
- Gnome upkeep as datapack data: which items, how much faster, how long a gift lasts.
