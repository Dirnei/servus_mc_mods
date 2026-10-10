package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookAdvancementConditionModel;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleText;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.manufacturing.datagen.AlchemyRecipeTable;
import io.github.dirnei.mc.servus.manufacturing.datagen.ServusManufacturingMultiblockProvider;
import io.github.dirnei.mc.servus.manufacturing.journal.Journal;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.world.item.Items;

/**
 * The Workshop: how to build and use each alchemy machine, with its crafting recipe and the list
 * of what it works, read from the same table that writes the recipes. Nigredo entries are open
 * from the start; Albedo entries open with the Schwaz chapter.
 */
final class WorkshopCategory extends CategoryProvider {

    WorkshopCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[] {
                "_____________________",
                "___p___c___u___o___a_",
                "_______f_____________",
                "___________________s_",
                "___________________e_",
                "___________________h_",
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
        add(albedo(hollow().generate('h').withParent(elixir)));
    }

    /** Albedo entries stay locked until the Villach chapter is finished, like the Schwaz chapter. */
    private static BookEntryModel albedo(BookEntryModel entry) {
        return entry.withCondition(BookAdvancementConditionModel.create()
                .withAdvancementId(Journal.advancementId(ServusManufacturingIds.MOD_ID + ":villach")));
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
        String mod = ServusManufacturingIds.MOD_ID + ":";
        return new WorkshopEntry(this, "calciner", "The Calciner", "Burn matter into its principles",
                ServusManufacturingItems.CALCINER.get(), e -> {
            e.spotlight("machine", ServusManufacturingItems.CALCINER.get(), "The Calciner",
                    "My furnace for the first work. It burns a thing until only its three principles are left.");
            e.multiblock("build", ServusManufacturingMultiblockProvider.CALCINER, "The Calciner",
                    "A wall one block deep with a tank on each side. Press Visualize, then click the ground where it shall stand.");
            e.text("parts", "The Parts",
                    "The furnace is bricks around three parts stacked in the middle: the Ash Pan at the bottom, the Firebox above it "
                            + "and the Calciner, the mouth, above that.\n\n"
                            + "On top of the mouth sits the Alembic, the copper still, and the Sulfur Hood crowns it. Beside the furnace stand two "
                            + "columns of three Fluid Tanks. The Receiver sits on the right column and catches what drips from the still's spout; "
                            + "keep the gap under the spout free. The Sulfur Duct leads from the Hood down into the left column.");
            e.crafting("recipe", mod + "calciner", mod + "calciner_firebox", "The mouth with its cauldron, and the Firebox.");
            e.crafting("recipe_pan", mod + "calciner_ash_pan", mod + "alembic", "The Ash Pan and the Alembic.");
            e.crafting("recipe_receiver", mod + "receiver", mod + "sulfur_hood", "The Receiver and the Sulfur Hood.");
            e.crafting("recipe_duct", mod + "sulfur_duct", "The Sulfur Duct, three at once.");
            e.text("waking", "Waking It",
                    "When every block stands, use this journal on the Calciner. The bricks join and the furnace becomes one thing. "
                            + "If something is missing, the journal tells you what and where.\n\n"
                            + "Break any block and the furnace falls apart into its bricks and parts again.");
            e.text("firing", "Firing It",
                    "Hand the matter to the mouth, or let a hopper or a pipe feed it. Coal and other fuel go into the Firebox below; "
                            + "one coal burns for several items. Sneak with an empty hand at the mouth to take the matter back.\n\n"
                            + "When a principle has no room left, the Calciner waits.");
            e.text("emptying", "Taking Them Out",
                    "Each principle leaves where it belongs. Salt stays behind as ash in the Ash Pan. Mercury drips through the Receiver "
                            + "into the right tank, and Sulfur rises into the Hood and runs down the duct into the left tank.\n\n"
                            + "Take them from the tanks with a bucket, or with a pipe whose side at the tank you set to pull in with the carrot: "
                            + "a tank on a plain side only ever fills.\n\n"
                            + "The Salt stays in the Ash Pan as Ash, one Ash for every 100 Salt. Take it with an empty hand, or put a hopper "
                            + "under the Ash Pan, and wash it in the Purifier. When the Ash Pan is full, the Calciner waits.");
            e.list("burns", "What Burns", AlchemyRecipeTable.calcination().stream()
                    .map(row -> PrincipleText.line(row.label(), row.output())).toList());
        });
    }

    private WorkshopEntry purifier() {
        return new WorkshopEntry(this, "purifier", "The Purifier", "Wash the Salt out of the Ash",
                ServusManufacturingItems.PURIFIER.get(), e -> {
            e.spotlight("machine", ServusManufacturingItems.PURIFIER.get(), "The Purifier",
                    "A leaching vat on a log between two planks. Water washes the Salt out of the Ash and runs down into the catch basin as "
                            + "the Salt the Coagulator needs; what stays behind is Caput Mortuum, the dead head, and it falls into the residue crock.");
            e.multiblock("build", ServusManufacturingMultiblockProvider.PURIFIER, "The Purifier",
                    "The Vat between two planks on a log, any wood; the Basin below one plank, the Crock below the other, either way round. "
                            + "Press Visualize, then click the ground.");
            e.crafting("recipe", ServusManufacturingIds.MOD_ID + ":purifier", ServusManufacturingIds.MOD_ID + ":catch_basin",
                    "The Leaching Vat and the Catch Basin.");
            e.crafting("crock", ServusManufacturingIds.MOD_ID + ":residue_crock", "The Residue Crock.");
            e.text("leaching", "Leaching",
                    "Use this journal on the Vat to bring the build together. Give the Vat Ash by hand, by hopper or by pipe, and water by "
                            + "bucket or by pipe from a pump. Four Ash at a time give up their Salt, and it runs into the Basin.\n\n"
                            + "Take the Salt from the Basin with an empty bucket, or pipe it away. Take the Caput Mortuum from the Crock "
                            + "with an empty hand, or set a hopper under it. Sneak with an empty hand on the Vat to take the Ash back. Keep the Caput Mortuum: nothing in the "
                            + "Great Work is worthless forever.");
            e.list("washes", "What It Washes", AlchemyRecipeTable.purification().stream()
                    .map(row -> row.count() + " " + row.label() + " and " + row.water() + " mB water: " + row.salt() + " Salt"
                            + (row.residue().isEmpty() ? "" : " and " + row.residue().getCount() + " " + row.residue().getHoverName().getString()))
                    .toList());
        });
    }

    private WorkshopEntry coagulator() {
        return new WorkshopEntry(this, "coagulator", "The Coagulator", "Join the principles into a body",
                ServusManufacturingItems.COAGULATOR.get(), e -> {
            e.spotlight("machine", ServusManufacturingItems.COAGULATOR.get(), "The Coagulator",
                    "It joins the principles again into a body. It needs no fire, only time to cool.");
            e.multiblock("build", ServusManufacturingMultiblockProvider.COAGULATOR, "The Coagulator",
                    "Three Principle Vats over a casting table between Stone Bricks. Press Visualize, then click the ground.");
            e.crafting("recipe", ServusManufacturingIds.MOD_ID + ":coagulator", ServusManufacturingIds.MOD_ID + ":principle_vat",
                    "The casting table, and a Principle Vat; you need three.");
            e.text("building", "Building It",
                    "Set the Coagulator between two Stone Bricks and put a Principle Vat on each of the three. "
                            + "The left Vat takes Sulfur, the middle one Salt and the right one Mercury, the same sides as at the Calciner. "
                            + "Use this journal on the Coagulator to bring the build together; break any block and it falls apart.");
            e.text("joining", "Joining",
                    "Open the Coagulator with an empty hand, press the select button and choose what to make. Then bring the principles: "
                            + "by pipe or bucket into their Vats, or on a gauge in the GUI.\n\n"
                            + "A hopper or pipe feeds the ingredient into the table and takes the products from it.");
            e.list("makes", "What It Makes", AlchemyRecipeTable.coagulation().stream()
                    .map(row -> PrincipleText.line(row.label(), row.input())).toList());
        });
    }

    private WorkshopEntry athanor() {
        return new WorkshopEntry(this, "athanor", "The Athanor", "The furnace that never goes out",
                ServusManufacturingItems.ATHANOR.get(), e -> {
            e.spotlight("machine", ServusManufacturingItems.ATHANOR.get(), "The Athanor",
                    "Lay an item on it, feed it principles, and it burns them into something new.");
            e.multiblock("build", ServusManufacturingMultiblockProvider.athanor("nigredo"), "The Athanor on its Hearth",
                    "Set it on 3 by 3 Bricks and use this journal on it. Press Visualize, then click the ground.");
            e.crafting("recipe", ServusManufacturingIds.MOD_ID + ":athanor", "Bricks, a cauldron and a blast furnace.");
            e.text("hearth", "Its Hearth",
                    "The Athanor burns only on a hearth: nine Bricks in a square right below it. When the hearth is laid, "
                            + "use this journal on the Athanor to light it for good. Take a brick away and its fire goes out.");
            e.text("fire", "Its Fire",
                    "Use an item on it to lay it on top. Pour principle buckets on it, or lead pipes to it.\n\n"
                            + "Alone its flame is black: Nigredo. Eight Salt Crystal Blocks in a ring around it, on its own level, "
                            + "turn the flame white: Albedo. Some works need the white fire.");
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
                ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get(), e -> {
            e.spotlight("block", ServusManufacturingItems.SALT_CRYSTAL_BLOCK.get(), "Salt Crystal Block",
                    "Nine Salt Crystals pressed together. Eight of them around the Athanor make its flame white.");
            e.crafting("recipe", ServusManufacturingIds.MOD_ID + ":salt_crystal_block", "Nine Salt Crystals from the Coagulator.");
            e.multiblock("ring", ServusManufacturingMultiblockProvider.athanor("albedo"), "The White Ring",
                    "Eight Salt Crystal Blocks around the Athanor on its own level. Press Visualize, then click the ground.");
        });
    }

    private WorkshopEntry elixir() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("elixir_of_sight");
        return new WorkshopEntry(this, "elixir_of_sight", "Elixir of Sight", "Albedo: see what lives in the rock",
                ElementalSight.ELIXIR.get(), e -> {
            e.spotlight("elixir", ElementalSight.ELIXIR.get(), "Elixir of Sight",
                    "Drink it and for a few minutes you see the spirits of the earth and what they tend.");
            e.text("brewing", "Brewing It",
                    "Lay " + craft.inputLabel() + " on a white Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ".");
        });
    }

    private WorkshopEntry hollow() {
        AlchemyRecipeTable.AthanorCraft craft = athanorCraft("gnome_hollow");
        return new WorkshopEntry(this, "gnome_hollow", "Gnome Hollow", "Albedo: a vein that grows",
                ServusManufacturingItems.GNOME_HOLLOW.get(), e -> {
            e.spotlight("hollow", ServusManufacturingItems.GNOME_HOLLOW.get(), "Gnome Hollow",
                    "A home for a gnome. To common eyes it is only stone.");
            e.text("making", "Making It",
                    "Lay " + craft.inputLabel() + " on a white Athanor and feed it " + PrincipleText.amounts(craft.principles()) + ".");
            e.text("growing", "Growing a Vein",
                    "Put a raw ore or an ore block into its sample slot. Feed it Salt and Mercury by pipe or bucket, "
                            + "and step by step the vein ripens.\n\n"
                            + "When it is ripe, drink the Elixir of Sight and use a Salt Crystal on the Hollow. A gnome takes it "
                            + "and digs that ore for you, as long as the Hollow stands.");
        });
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
