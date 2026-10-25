package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.PrincipleText;
import io.github.dirnei.mc.servus.alchemy.PurificationText;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.coagulator.SeedText;
import io.github.dirnei.mc.servus.alchemy.datagen.AlchemyRecipeTable;
import io.github.dirnei.mc.servus.alchemy.datagen.ServusAlchemyMultiblockProvider;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * The Workshop: how to build and use each alchemy machine, with its crafting recipe and the list
 * of what it works, read from the same table that writes the recipes. Nigredo entries are open
 * from the start; Albedo entries open with the Schwaz chapter, once Villach is finished, Citrinitas
 * entries with Wandering years and Rubedo entries with Basel and Salzburg.
 */
final class WorkshopCategory extends CategoryProvider {

    WorkshopCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[] {
                "_______________________",
                "___p___c___u___o___a___",
                "_______f_______________",
                "___________________s___",
                "___________________e___",
                "_________________k_h___",
                "_________________d_y___",
                "_______________g_m_r_l_",
                "_________________v_q___",
        };
    }

    @Override
    protected void generateEntries() {
        BookEntryModel principles = add(principles().generate('p'));
        BookEntryModel calciner = add(calciner().generate('c').withParent(principles));
        BookEntryModel purifier = add(purifier().generate('u').withParent(calciner));
        BookEntryModel coagulator = add(coagulator().generate('o').withParent(purifier));
        BookEntryModel athanor = add(athanor().generate('a').withParent(coagulator));
        add(pipes().generate('f').withParent(calciner));
        BookEntryModel block = add(albedo(saltCrystalBlock().generate('s').withParent(athanor)));
        BookEntryModel elixir = add(albedo(elixir().generate('e').withParent(block)));
        BookEntryModel hollow = add(albedo(hollow().generate('h').withParent(elixir)));
        add(albedo(knockers().generate('k').withParent(hollow)));
        BookEntryModel yellow = add(yellowRing().generate('y').withParent(hollow)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ)));
        add(dropper().generate('d').withParent(yellow)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ)));
        BookEntryModel salamander = add(salamander().generate('m').withParent(yellow)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ)));
        add(forge().generate('g').withParent(salamander)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ)));
        BookEntryModel red = add(redCrown().generate('r').withParent(yellow)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.WANDERING_YEARS)));
        BookEntryModel receptaculum = add(receptaculum().generate('q').withParent(red)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.WANDERING_YEARS)));
        add(vas().generate('v').withParent(receptaculum)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.WANDERING_YEARS)));
        add(prelum().generate('l').withParent(red)
                .withCondition(condition().researchNodeUnlocked(JournalResearch.WANDERING_YEARS)));
    }

    /** Locks an Albedo entry until the Villach chapter is finished. */
    private BookEntryModel albedo(BookEntryModel entry) {
        return entry.withCondition(condition().researchNodeUnlocked(JournalResearch.VILLACH));
    }

    private WorkshopEntry principles() {
        return new WorkshopEntry(this, "principles", "The Three Principles", "Salt, Sulfur and Mercury",
                PrincipleFluids.entry(Principle.SALT).bucket().get(), e -> {
            e.text("three", "The Three",
                    "Every body is made of three. Salt gives it form, Sulfur makes it burn, and Mercury is the spirit that flies off. "
                            + "In the fire the Salt stays behind as Ash; the Purifier washes it into liquid Salt.\n\n"
                            + "In this work they are liquids. They flow through Fluid Pipes, rest in Fluid Tanks and fill buckets.");
            e.text("keeping", "Keeping Them",
                    "A Fluid Tank holds one principle at a time, so give each principle its own tank.\n\n"
                            + "Poured on the ground they spread like water but never make a new source. What you do not need, pour into a Sink.");
        });
    }

    private WorkshopEntry calciner() {
        String mod = ServusAlchemyIds.MOD_ID + ":";
        return new WorkshopEntry(this, "calciner", "The Calciner", "Burn matter into its principles",
                ServusAlchemyItems.CALCINER.get(), e -> {
            e.spotlight("machine", ServusAlchemyItems.CALCINER.get(), "The Calciner",
                    "My furnace for the first work. It burns a thing until only its three principles are left.");
            e.multiblock("build", ServusAlchemyMultiblockProvider.CALCINER, "The Calciner",
                    "A wall one block deep with a tank on each side. Press Visualize, then click the ground where it shall stand.");
            e.text("parts", "The Parts",
                    "Three parts stack in the middle: the Ash Pan at the bottom between Deepslate Tiles, the Firebox above it between "
                            + "two Fluid Tanks, and the Calciner, the mouth, between Stone Bricks.\n\n"
                            + "On top of the mouth sits the Alembic, the copper still, and the Sulfur Hood crowns it. The Receiver, a copper "
                            + "condenser, stands right of the still over a stone basin, and the still's beak leads into it. The Copper Duct leads from the Hood down into the left Stone Bricks.");
            e.crafting("recipe", mod + "calciner", mod + "calciner_firebox", "The mouth with its cauldron, and the Firebox.");
            e.crafting("recipe_pan", mod + "calciner_ash_pan", mod + "alembic", "The Ash Pan and the Alembic.");
            e.crafting("recipe_receiver", mod + "receiver", mod + "sulfur_hood", "The Receiver and the Sulfur Hood.");
            e.crafting("recipe_duct", mod + "copper_duct", "The Copper Duct, three at once.");
            e.text("waking", "Waking It",
                    "When every block stands, use this journal on the Calciner. The stones join and the furnace becomes one thing. "
                            + "If something is missing, the journal tells you what and where.\n\n"
                            + "Break any block and the furnace falls apart into its stones and parts again.");
            e.text("firing", "Firing It",
                    "Hand the matter to the mouth, or let a hopper or a pipe feed it. Coal and other fuel go into the Firebox below; "
                            + "one coal burns for several items. Sneak with an empty hand at the mouth to take the matter back.\n\n"
                            + "When a tank or the Ash Pan is full, the Calciner waits.\n\n"
                            + "Set Ignis Perpetuus into the Firebox and it burns forever without fuel and calcines faster. "
                            + "Sneak with an empty hand at the Firebox to take it back.");
            e.text("emptying", "Taking Them Out",
                    "Each principle leaves where it belongs. Salt stays behind as ash in the Ash Pan. Mercury cools in the Receiver, "
                            + "drips from its tap into the basin and runs down into the right tank, and Sulfur rises into the Hood and runs down the duct into the left tank.\n\n"
                            + "Take them from the tanks with a bucket, or with a pipe whose side at the tank you set to pull in with the carrot: "
                            + "a tank on a plain side only ever fills.\n\n"
                            + "The Salt stays in the Ash Pan as Ash, one Ash for every 100 Salt. Take it with an empty hand, or put a hopper "
                            + "under the Ash Pan, and wash it in the Purifier. The Ash Pan holds a stack of 64 Ash; when it is full, the Calciner waits.");
            e.calcinationTable("burns");
        });
    }

    private WorkshopEntry purifier() {
        return new WorkshopEntry(this, "purifier", "The Purifier", "Wash the Salt out of the Ash",
                ServusAlchemyItems.PURIFIER.get(), e -> {
            e.spotlight("machine", ServusAlchemyItems.PURIFIER.get(), "The Purifier",
                    "A leaching vat on a log. Water washes the Salt out of the Ash and runs down into the catch basin as "
                            + "the Salt the Coagulator needs; what stays behind is Caput Mortuum, the dead head, and it falls into the residue crock.");
            e.multiblock("build", ServusAlchemyMultiblockProvider.PURIFIER, "The Purifier",
                    "The Vat on a log of any wood, the Basin on one side of the log and the Crock on the other, either way round. "
                            + "Press Visualize, then click the ground.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":purifier", ServusAlchemyIds.MOD_ID + ":catch_basin",
                    "The Leaching Vat and the Catch Basin.");
            e.crafting("crock", ServusAlchemyIds.MOD_ID + ":residue_crock", "The Residue Crock.");
            e.text("leaching", "Leaching",
                    "Use this journal on the Vat to bring the build together. Give the Vat Ash by hand, by hopper or by pipe, and water by "
                            + "bucket or by pipe from a pump. Four Ash at a time give up their Salt, and it runs into the Basin.\n\n"
                            + "Take the Salt from the Basin with an empty bucket, or pipe it away. Take the Caput Mortuum from the Crock "
                            + "with an empty hand, or set a hopper under it. Sneak with an empty hand on the Vat to take the Ash back. Keep the Caput Mortuum: nothing in the "
                            + "Great Work is worthless forever.");
            e.text("more", "More Than Ash",
                    "The Vat washes more than Ash. Sugar, Gunpowder and Redstone leave crystals behind that the Coagulator will want, "
                            + "and Dried Kelp gives the cheapest Salt of all.\n\n"
                            + "Water also washes the dye out of wool, carpet and terracotta and sets Concrete Powder. "
                            + "What gives no Salt only fills the Crock.");
            List<String> washes = new ArrayList<>();
            AlchemyRecipeTable.purification().forEach(row -> washes.add(PurificationText.line(row.count(), row.label(), row.water(),
                    row.salt(), row.residueCount(), row.residueLabel())));
            AlchemyRecipeTable.washes().forEach(wash -> {
                AlchemyRecipeTable.Purify first = wash.rows().getFirst();
                washes.add(PurificationText.kindLine(first.count(), wash.label(), first.water(), wash.residueLabel()));
            });
            e.list("washes", "What It Washes", washes);
        });
    }

    private WorkshopEntry coagulator() {
        return new WorkshopEntry(this, "coagulator", "The Coagulator", "Join the principles into a body",
                ServusAlchemyItems.COAGULATOR.get(), e -> {
            e.spotlight("machine", ServusAlchemyItems.COAGULATOR.get(), "The Coagulator",
                    "It joins the principles again into a body. It needs no fire, only time to cool.");
            e.multiblock("build", ServusAlchemyMultiblockProvider.COAGULATOR, "The Coagulator",
                    "Three Principle Vats over a casting table between Stone Bricks. Press Visualize, then click the ground.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":coagulator", ServusAlchemyIds.MOD_ID + ":principle_vat",
                    "The casting table, and a Principle Vat; you need three.");
            e.text("building", "Building It",
                    "Set the Coagulator between two Stone Bricks and put a Principle Vat on each of the three. "
                            + "The left Vat takes Sulfur, the middle one Salt and the right one Mercury, the same sides as at the Calciner. "
                            + "Use this journal on the Coagulator to bring the build together; break any block and it falls apart.");
            e.text("joining", "Joining",
                    "Like begets like. Lay seeds in the mold: crystals of what should grow, as many of one kind as a stack holds. "
                            + "Open the Coagulator with an empty hand and put them into the seed slot, or use them on the table.\n\n"
                            + "Then bring the principles: by pipe or bucket into their Vats, or on a gauge in the GUI. Each time they set, "
                            + "one seed is used up and comes back doubled: two of its kind lie in the output. When the last seed is gone, "
                            + "the Coagulator stops.\n\n"
                            + "To stop sooner, take the seeds out: in the GUI, or sneak with an empty hand on the table. A hopper or pipe "
                            + "feeds the ingredient and more of the same seed and takes the products, but never takes the seeds.");
            e.text("first_seeds", "First Seeds",
                    "Every product grows from a crystal of itself. The very first Salt Crystal, Sulfur and Quicksilver need a "
                            + "stranger: the Purifier washes them out of Sugar, Gunpowder and Redstone. A stranger seed does not "
                            + "come back; it turns into one crystal of the product.");
            e.list("seeds", "Seeds", AlchemyRecipeTable.coagulation().stream()
                    .flatMap(row -> row.seedItems().stream()
                            .filter(seed -> seed != row.result().item().value())
                            .map(seed -> SeedText.line(AlchemyRecipeTable.label(new ItemStackTemplate(seed)), row.label())))
                    .toList());
            e.list("makes", "What It Makes", AlchemyRecipeTable.coagulation().stream()
                    .map(row -> PrincipleText.line(row.label(), row.input())).toList());
        });
    }

    private WorkshopEntry athanor() {
        return new WorkshopEntry(this, "athanor", "The Athanor", "The furnace that never goes out",
                ServusAlchemyItems.ATHANOR.get(), e -> {
            e.spotlight("machine", ServusAlchemyItems.ATHANOR.get(), "The Athanor",
                    "Lay an item on it, feed it principles, and it burns them into something new.");
            e.multiblock("build", ServusAlchemyMultiblockProvider.athanor("nigredo"), "The Athanor on its Hearth",
                    "Set it on a hearth of Bricks and Brick Slabs with a Brick Stair in front, stand three Fluid Tanks three high "
                            + "to its left, behind it and to its right, lay a Copper Duct from the Athanor to each tank, and use this journal on it. "
                            + "Press Visualize, then click the ground.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":athanor", "Stone Bricks, a cauldron and a blast furnace.");
            e.text("hearth", "Its Hearth",
                    "The Athanor burns only on a hearth of 3 by 3 right below it: Bricks under it and in front of it, a bottom Brick Slab "
                            + "on each of the other seven cells, and a Brick Stair in front as the step. "
                            + "Three Fluid Tanks stand two blocks out, each three high from the hearth up, and a Copper Duct beside the Athanor "
                            + "leads to each. When all is laid, use this journal on the Athanor: it rises into a brick tower with a fire door "
                            + "and the ducts become pipes into its tanks. Take a brick, a slab, a tank or a duct away and its fire goes out.");
            e.text("tanks", "Its Tanks",
                    "The Athanor keeps no principles of its own. It drinks them from its three Fluid Tanks, two blocks out to its left, "
                            + "behind it and to its right, so they never touch and never run together.\n\n"
                            + "Any tank may hold any principle. Lead pipes to them or pour buckets in. Each tank of three is one big tank.");
            e.text("fire", "Its Fire",
                    "Use an item on it to lay it on top. It burns what its tanks hold, and the work lies on top when it is done: "
                            + "take it with an empty hand, or sneak to take the item back. Hoppers and pipes may do both.\n\n"
                            + "Alone its flame is black: Nigredo. A ring of Salt Crystal Blocks under its hearth turns the flame white: Albedo. "
                            + "An Amethyst Block offered to it grows citrine in Principle Vats beside its tanks and turns it yellow: Citrinitas. "
                            + "A Ghast Tear offered to it grows dark red obelisks out of Obsidian beside its tanks and turns it red: Rubedo. "
                            + "Some works need a higher fire.");
            e.list("works", "Its Works", AlchemyRecipeTable.athanor().stream()
                    .map(craft -> PrincipleText.line(craft.resultLabel() + " from " + craft.inputLabel(), craft.principles())
                            + (craft.tier() > 0 ? " (" + AlchemyRecipeTable.stageName(craft.tier()) + ")" : ""))
                    .toList(), 3, 4);
        });
    }

    private WorkshopEntry pipes() {
        return new WorkshopEntry(this, "pipes", "Pipes and Sides", "Where each principle goes",
                ServusLogisticsItems.FLUID_PIPE.get(), e -> {
            e.text("channels", "Pipes and Channels",
                    "A Fluid Pipe moves principles between blocks. Every pipe arm that touches a block has a channel colour, "
                            + "and only arms of the same channel trade.\n\n"
                            + "Set the channel with the carrot on the arm that touches the block.");
            e.text("sides", "Sides",
                    "A machine side has two settings. The Items tab says what items do there. The Fluids tab says which principle "
                            + "goes in or out: Off, All, or one principle.\n\n"
                            + "The carrot works on one of them; use it on the air to switch between Items and Fluids.\n\n"
                            + "The Calciner has no sides to set: each principle leaves through its own part.");
        });
    }

    private WorkshopEntry saltCrystalBlock() {
        return new WorkshopEntry(this, "salt_crystal_block", "Salt Crystal Block", "Albedo: the white ring",
                ServusAlchemyItems.SALT_CRYSTAL_BLOCK.get(), e -> {
            e.spotlight("block", ServusAlchemyItems.SALT_CRYSTAL_BLOCK.get(), "Salt Crystal Block",
                    "A Salt Crystal grown whole on the Athanor. Sixteen of them in a ring under its hearth make its flame white.");
            e.text("grow", "Growing It",
                    "No hand can press Salt into a block. Lay a Salt Crystal from the Coagulator on the Athanor and give its tanks "
                            + PrincipleText.amounts(AlchemyRecipeTable.athanor().getFirst().principles())
                            + ": the fire grows the crystal into a block.");
            e.multiblock("ring", ServusAlchemyMultiblockProvider.athanor("albedo"), "The White Ring",
                    "Sixteen Salt Crystal Blocks in a 5 by 5 ring one layer under the hearth, so the tanks and the edge of the hearth "
                            + "stand on it. Press Visualize, then click the ground.");
        });
    }

    private WorkshopEntry elixir() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("elixir_of_sight");
        return new WorkshopEntry(this, "elixir_of_sight", "Elixir of Sight", "Albedo: see what lives in the rock",
                ElementalSight.ELIXIR.get(), e -> {
            e.spotlight("elixir", ElementalSight.ELIXIR.get(), "Elixir of Sight",
                    "Drink it and for a few minutes you see the spirits of the earth and what they tend.");
            e.text("brewing", "Brewing It", albedoCraftText(craft));
            e.text("dose", "The Dose",
                    "The dose makes the poison. Now and then a drink sickens you: poison and a spinning head for a few breaths, "
                            + "though the sight comes all the same. Drink again while the sight still lasts and the risk grows.");
        });
    }

    private WorkshopEntry hollow() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("gnome_hollow");
        return new WorkshopEntry(this, "gnome_hollow", "Gnome Hollow", "Albedo: a vein that grows",
                ServusAlchemyItems.GNOME_HOLLOW.get(), e -> {
            e.spotlight("hollow", ServusAlchemyItems.GNOME_HOLLOW.get(), "Gnome Hollow",
                    "A home for a gnome. To common eyes it is only stone.");
            e.text("making", "Making It", albedoCraftText(craft));
            e.text("gnome", "Calling the Gnome",
                    "Drink the Elixir of Sight first. To common eyes the Hollow is only stone, and it answers no one who cannot see.\n\n"
                            + "Under the sight, use a Salt Crystal on the empty Hollow. A gnome takes it and moves in. "
                            + "He waits for a seed to plant.");
            e.text("seeds", "Seeds of Metals",
                    "Raw ore does not take root; the gnome wants the seed of a metal. The Athanor makes them: "
                            + seedText(1) + " on a white Athanor. Nobler seeds need a hotter fire.\n\n"
                            + "Give the seed to the Hollow and the gnome plants it.");
            e.text("growing", "Growing a Vein",
                    "Pour Salt and Mercury in by bucket. No pipe reaches it. Step by step the vein ripens while the gnome tends it, "
                            + "and once ripe he digs its ore for you, as long as the Hollow stands.\n\n"
                            + "Give him the seed of another metal and he digs the old vein out and plants the new one. "
                            + "The old vein is lost; what he dug stays in his store.");
            e.text("store", "The Gnome's Store",
                    "The gnome keeps what it digs in a small store inside the Hollow and stops when the store is full. "
                            + "Use an empty hand on the Hollow, under the sight, to take the ore out. "
                            + "After a new seed, take the old ore out before he digs the new one.");
            e.text("gifts", "Gifts",
                    "Gnomes have a sweet tooth. Under the sight, hand a working gnome a cookie, bread or berries and it digs "
                            + "twice as fast for a while. It needs no gifts to work; it only works slower without them. "
                            + "Anything else it will not touch.");
            e.text("moving", "Moving It",
                    "Break the Hollow under the sight and you get an empty Hollow back, with the ore from its store. "
                            + "The vein, its principles and its gnome are lost.\n\n"
                            + "Break it without the sight and it is only stone.");
        });
    }

    private WorkshopEntry yellowRing() {
        return new WorkshopEntry(this, "amethyst_ring", "The Yellow Ring", "Citrinitas: the yellow fire",
                Items.AMETHYST_BLOCK, e -> {
            e.spotlight("block", Items.AMETHYST_BLOCK, "Amethyst Block",
                    "Four shards of the violet crystal that grows in the hollow stones of the deep make a block. "
                            + "Heated in the white fire, amethyst turns into yellow citrine, the stone Citrinitas is named after.");
            e.text("build", "Building It",
                    "Beside each tank, on its outer side three blocks from the Athanor, set an empty Principle Vat on the hearth level. "
                            + "Then use an Amethyst Block on the white Athanor: it rises, bursts into three streams of light, and a tower "
                            + "of citrine grows out of each vat, budding as it stands. The flame turns yellow.\n\n"
                            + "Break a grown vat and the citrine falls to shards; the fire is white again until the ritual is cast anew.");
            e.multiblock("ring", ServusAlchemyMultiblockProvider.athanor("citrinitas"), "The Yellow Ring",
                    "Three empty Principle Vats beside the tanks of the white Athanor; then offer it an Amethyst Block. "
                            + "Press Visualize, then click the ground.");
            e.text("veins", "Noble Veins",
                    "The yellow fire makes nobler seeds: " + seedText(2) + ". Only the red fire makes " + seedText(3) + ".\n\n"
                            + "Noble veins drink more Salt and Mercury, the diamond Sulfur as well, and the gnomes dig them slower.");
        });
    }

    private WorkshopEntry redCrown() {
        return new WorkshopEntry(this, "crying_obsidian", "The Red Crown", "Rubedo: the red fire",
                Items.GHAST_TEAR, e -> {
            e.spotlight("block", Items.GHAST_TEAR, "Ghast Tear",
                    "A ghast weeps it as it dies, far over the lava seas of the Nether. Catch it before it falls into the fire. "
                            + "In the yellow fire one tear is enough to wake the Obsidian.");
            e.text("build", "Building It",
                    "On the white ring, on the hearth level, set Obsidian on each side of the tank behind the Athanor and behind each "
                            + "side tank, with nothing above it. Then use a Ghast Tear on the yellow Athanor: the tear splits into four "
                            + "drops, the Obsidian cracks, and a dark red obelisk rises out of each block while the citrine turns orange. "
                            + "The flame turns red.\n\n"
                            + "Break an obelisk and it falls back to Obsidian; the fire is yellow again until the ritual is cast anew.");
            e.multiblock("crown", ServusAlchemyMultiblockProvider.athanor("rubedo"), "The Red Crown",
                    "Obsidian on the four pillar cells of the yellow Athanor, with air above; then offer it a Ghast Tear. "
                            + "Press Visualize, then click the ground.");
        });
    }

    private WorkshopEntry dropper() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("principle_dropper");
        return new WorkshopEntry(this, "principle_dropper", "Principle Dropper", "Citrinitas: the Hollow fed by drops",
                ServusAlchemyItems.PRINCIPLE_DROPPER.get(), e -> {
            e.spotlight("dropper", ServusAlchemyItems.PRINCIPLE_DROPPER.get(), "Principle Dropper",
                    "A vat taught to weep. It lets the principles fall drop by drop, so no pipe need ever touch the Hollow.");
            e.text("making", "Making It",
                    "Lay " + craft.inputLabel() + " on a yellow Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ".");
            e.text("using", "Using It",
                    "Set the Dropper right on top of a Gnome Hollow and lead Salt, Sulfur and Mercury into it with a Fluid Pipe, "
                            + "or pour buckets in. Each principle has its own small tank.\n\n"
                            + "While the vein below is seeded and not yet ripe, the Dropper lets fall what the vein drinks, a few drops at a time, "
                            + "and the vein ripens without your buckets. A ripe vein, an empty Hollow or plain stone gets nothing; "
                            + "the Dropper keeps its principles and pours them into nothing else.");
            e.text("binding", "Still by Hand",
                    "The drops only ripen the vein. Binding the gnome with a Salt Crystal and taking its ore out remain your work, "
                            + "under the sight.");
        });
    }

    private WorkshopEntry knockers() {
        return new WorkshopEntry(this, "knockers", "Knocking in the Rock", "Signs of what the eye cannot see",
                ServusAlchemyItems.TINY_PICKAXE.get(), e -> {
            e.text("signs", "Three Signs",
                    "What lives in the elements hides from common eyes, but not from common ears. First you hear it: a knocking in the rock, "
                            + "and it comes from somewhere, so follow it. Closer, dust trickles from one block. Closest of all, you find what "
                            + "they leave behind: a tiny pickaxe, a scrap of red felt, a little bowl of salt.\n\n"
                            + "Then drink the Elixir of Sight, and look.");
            e.text("who", "Who Knocks",
                    "A gnome at work in a Gnome Hollow knocks softly now and then. A salamander in the lava of the Nether shows as a flame "
                            + "that dances a little too long. The homunculus in its Vas stirs the liquid by itself.\n\n"
                            + "And deep in the deepslate, if you have walked with me to Schwaz, someone older may knock for you.");
        });
    }

    private WorkshopEntry salamander() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("ignis_perpetuus");
        return new WorkshopEntry(this, "salamander", "The Salamander", "Citrinitas: the fire that eats gold",
                ServusAlchemyItems.IGNIS_PERPETUUS.get(), e -> {
            e.text("stems", "Among the Crimson Stems",
                    "The salamanders, lizards of glowing coal, live in the Nether's crimson forests, near the crimson trees and the Nether Wart. "
                            + "Fewer live among the warped trees; now and then one swims on lava.\n\n"
                            + "Without the sight you see only a flame that dances a little too long. "
                            + "Drink the Elixir of Sight and you see them. Fire and lava do them no harm, and they harm nobody.");
            e.text("gold", "Gold for a Friend",
                    "Hold out a Gold Ingot to a salamander you can see. It eats every ingot, and now and then it takes a liking to you: "
                            + "hearts, and it is yours. A befriended salamander shows itself to everyone.\n\n"
                            + "It follows you, even through the lava, and finds you when you go too far. "
                            + "Click it with an empty hand to make it sit or stand. It likes to lie in a lit campfire.");
            e.spotlight("sal_ammoniac", ServusAlchemyItems.SAL_AMMONIAC.get(), "Sal Ammoniac",
                    "Feed your salamander a Gold Ingot and wait a minute. It gives back one lump of Sal Ammoniac. "
                            + "One ingot at a time; it will not take the next before it is done.");
            e.spotlight("ignis", ServusAlchemyItems.IGNIS_PERPETUUS.get(), "Ignis Perpetuus",
                    "Lay " + craft.inputLabel() + " on a yellow Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ". "
                            + "The flame that never goes out. It does not feed a furnace; it fires a Calciner and is built into greater works.");
            e.text("eternal_fire", "The Eternal Flame",
                    "Set Ignis Perpetuus into the Firebox of a Calciner and it fires the Calciner forever, without coal, "
                            + "and makes it calcine faster. Sneak with an empty hand at the Firebox to take it back.");
        });
    }

    private WorkshopEntry forge() {
        return new WorkshopEntry(this, "salamander_forge", "The Salamander Forge", "Citrinitas: the furnace that needs no coal",
                ServusAlchemyItems.SALAMANDER_FORGE.get(), e -> {
            e.spotlight("forge", ServusAlchemyItems.SALAMANDER_FORGE.get(), "The Salamander Forge",
                    "A furnace with the eternal flame shut inside. It has eaten its last lump of coal.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":salamander_forge",
                    "Ignis Perpetuus over a Furnace, walled in Deepslate Bricks.");
            e.text("using", "Using It",
                    "It smelts what a furnace smelts, as fast as a furnace, and never asks for fuel. "
                            + "Its fire burns day and night and lights the room around it.\n\n"
                            + "Lay the ore in the top slot and take the metal from the right. Hoppers and Item Pipes feed it from any side but the front, "
                            + "and it pushes its work into a chest beside it. On a data line it smelts what a Crafting CPU orders.");
        });
    }

    private WorkshopEntry receptaculum() {
        return new WorkshopEntry(this, "receptaculum", "The Receptaculum", "Rubedo: the warm chest below the glass",
                ServusAlchemyItems.RECEPTACULUM.get(), e -> {
            e.spotlight("chest", ServusAlchemyItems.RECEPTACULUM.get(), "The Receptaculum",
                    "A chest with a fire in its belly. What my little helper gathers falls into it, "
                            + "and its fire keeps him warm, as every homunculus must be kept warm.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":receptaculum",
                    "Ignis Perpetuus between Deepslate Bricks, a Chest above it and a hearth of Bricks below.");
            e.text("slots", "Its Slots",
                    "The upper row holds three gifts for the gnomes on the left and the Elixir of Sight on the right. "
                            + "Below lie twenty-seven slots for the yield; you may only take from them.\n\n"
                            + "Hoppers and Item Pipes take the yield out on every side but the front and bring gifts and Elixir in. "
                            + "Pipe nothing else into it: whatever is no Elixir lands among the gifts. "
                            + "On a data line a Crafting CPU orders ore from it as from any machine.");
        });
    }

    private WorkshopEntry vas() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("sealed_flask");
        return new WorkshopEntry(this, "vas_hermeticum", "The Vas Hermeticum", "Rubedo: the faithful little helper",
                ServusAlchemyItems.SEALED_FLASK.get(), e -> {
            e.text("helper", "My Little Helper",
                    "They laughed in Basel when I wrote it down. Seal the seed in the glass, keep it warm forty days, "
                            + "feed it the arcanum, and a little man grows in it, wiser than the fool who made it.\n\n"
                            + "They laugh no more. Mine sits in his glass and works for me, my faithful little helper, "
                            + "and he never complains.");
            e.spotlight("flask", ServusAlchemyItems.SEALED_FLASK.get(), "Sealed Flask",
                    "Lay " + craft.inputLabel() + " on a red Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ". "
                            + "Set it on a Receptaculum; it stands nowhere else.");
            e.text("growing", "Growing",
                    "Lay the Elixir of Sight into the Receptaculum's Elixir slot. The homunculus grows while it drinks, "
                            + "two days and two nights, and the Elixir runs out as it grows and as it works. "
                            + "Without the Elixir it sleeps and waits.\n\n"
                            + "Break the glass and it is gone; a new flask starts a new one from the seed.");
            e.text("work", "Its Work",
                    "Grown, it reaches every Gnome Hollow on the Receptaculum's level, two blocks to every side. "
                            + "It takes the ore the gnomes dug and lays it into the Receptaculum, and when a gnome tires "
                            + "it brings it a gift from the gift slots.\n\n"
                            + "Only the sight shows him at his work. Do not listen too closely to what he mutters.");
        });
    }

    private WorkshopEntry prelum() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("prelum_screw");
        AlchemyRecipeTable.Press press = AlchemyRecipeTable.pressing().getFirst();
        return new WorkshopEntry(this, "prelum", "The Prelum", "Rubedo: the press at the end of the Work",
                ServusAlchemyItems.PRELUM.get(), e -> {
            e.text("story", "The Press",
                    "[storyteller] The dead head! Caput Mortuum, the worthless earth that stays when the Salt is gone. "
                            + "For years I swept it out of the vat and they laughed. Press it, I say, press it hard enough and long enough "
                            + "and the dead head gives up the black metal of the burning world. The dragon's breath makes the screw. "
                            + "I have seen the dragon. Or I dreamt it. The screw is real.");
            e.spotlight("screw", ServusAlchemyItems.PRELUM_SCREW.get(), "Prelum Screw",
                    "Lay " + craft.inputLabel() + " on a red Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ". "
                            + "One bottle from the End, once: the screw does not wear out.");
            e.crafting("recipe", ServusAlchemyIds.MOD_ID + ":prelum",
                    "The screw over a Heavy Weighted Pressure Plate between two Stripped Dark Oak Logs, on Polished Deepslate.");
            e.text("using", "Using It",
                    "Lay " + press.count() + " " + press.label() + " in the left slot. The press needs no fire and no principles; "
                            + "it turns its bar, lowers its plate and after " + press.time() / 20 + " seconds gives one "
                            + AlchemyRecipeTable.label(press.result()) + ". Smith four scraps with four Gold Ingots into a Netherite Ingot.\n\n"
                            + "Hoppers and Item Pipes feed it from any side but the front, and it pushes its scrap into a chest beside it. "
                            + "On a data line it presses what a Crafting CPU orders.");
        });
    }

    /** The Seeds of one stage with what they are made from: "the Seed of Gold from Raw Gold and ...". */
    private static String seedText(int tier) {
        List<String> seeds = AlchemyRecipeTable.seeds().stream().filter(craft -> craft.tier() == tier)
                .map(craft -> "the " + craft.resultLabel() + " from " + craft.inputLabel()).toList();
        return seeds.size() < 2 ? String.join("", seeds)
                : String.join(", ", seeds.subList(0, seeds.size() - 1)) + " and " + seeds.getLast();
    }

    private static String albedoCraftText(AlchemyRecipeTable.AthanorCraft craft) {
        return "Lay " + craft.inputLabel() + " on a white Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ".";
    }

    private static AlchemyRecipeTable.AthanorCraft athanorCraft(String name) {
        return AlchemyRecipeTable.athanor().stream().filter(craft -> craft.name().equals(name)).findFirst().orElseThrow();
    }

    @Override
    protected String categoryName() {
        return "Workshop";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.CRAFTING_TABLE);
    }

    @Override
    public String categoryId() {
        return "workshop";
    }
}
